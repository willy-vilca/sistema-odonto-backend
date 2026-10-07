package com.odontocare.agent.service;

import com.odontocare.agent.config.AgentProperties;
import com.odontocare.agent.dto.AgentContracts.*;
import com.odontocare.agent.model.AgentRun;
import com.odontocare.agent.repository.*;
import com.odontocare.audit.service.AuditService;
import com.odontocare.kapso.config.KapsoProperties;
import com.odontocare.kapso.repository.KapsoRepository;
import com.odontocare.shared.pagination.*;
import com.odontocare.shared.web.ApiException;
import com.odontocare.whatsapp.dto.WhatsAppContracts.Message;
import com.odontocare.whatsapp.repository.WhatsAppRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentQueueService {
  private final AgentProperties config;
  private final AgentRepository runs;
  private final AgentInboxRepository inbox;
  private final WhatsAppRepository twilio;
  private final KapsoRepository messages;
  private final AgentReplyService replies;
  private final AuditService audit;
  private final Clock clock;
  private final KapsoProperties kapso;

  public AgentQueueService(
      AgentProperties config,
      AgentRepository runs,
      AgentInboxRepository inbox,
      WhatsAppRepository twilio,
      KapsoRepository messages,
      AgentReplyService replies,
      AuditService audit,
      Clock clock,
      KapsoProperties kapso) {
    this.config = config;
    this.runs = runs;
    this.inbox = inbox;
    this.twilio = twilio;
    this.messages = messages;
    this.replies = replies;
    this.audit = audit;
    this.clock = clock;
    this.kapso = kapso;
  }

  private boolean allowed() {
    return !kapso.isEnabled() || kapso.isAgentEnabled();
  }

  public Configuration configuration() {
    return new Configuration(
        config.isEnabled() && allowed(),
        config.ready() && allowed() && (!kapso.isEnabled() || kapso.ready()),
        config.isWorkerEnabled() && allowed(),
        config.getProvider(),
        config.getModel(),
        kapso.isEnabled() && kapso.isAgentEnabled() ? "WHATSAPP" : "PREVIEW",
        config.missing(),
        config.getMaxModelCalls(),
        config.getMaxCompletionTokens());
  }

  @Transactional
  public void inbound(UUID message, UUID conversation) {
    inbound("TWILIO", message, conversation);
  }

  @Transactional
  public void inbound(String provider, UUID message, UUID conversation) {
    inbox.register(provider, conversation, message);
    if (config.isEnabled() && allowed())
      runs.enqueue(message, conversation, config.getModel(), clock.instant());
  }

  @Transactional
  public TestResult test(TestMessage request) {
    if (!allowed() || !config.ready())
      throw ApiException.badRequest(
          "Activa el agente y completa su configuración antes de probarlo.");
    boolean useKapso = kapso.isEnabled();
    if (useKapso) messages.keyLock(request.requestKey());
    else twilio.keyLock(request.requestKey());
    var prior =
        useKapso ? messages.byKey(request.requestKey()) : twilio.byKey(request.requestKey());
    UUID conversation, message;
    if (prior.isPresent()) {
      var old = prior.get();
      var contact =
          useKapso
              ? messages.conversation(old.conversationId(), false).orElseThrow()
              : twilio.conversation(old.conversationId(), false).orElseThrow();
      if (!old.source().equals("APP_TEST")
          || !old.body().equals(request.body().strip())
          || !contact.phone().equals(request.phone()))
        throw ApiException.conflict("La referencia de prueba ya se usó con otro mensaje.");
      conversation = old.conversationId();
      message = old.id();
    } else {
      conversation =
          useKapso
              ? messages.inboundConversation(
                  kapso.getPhoneNumberId(),
                  request.phone(),
                  request.contactName().strip(),
                  clock.instant())
              : twilio.inboundConversation(
                  request.phone(), request.contactName().strip(), clock.instant());
      message =
          useKapso
              ? messages.testInbound(
                  conversation, request.body().strip(), request.requestKey(), clock.instant())
              : twilio.testInbound(
                  conversation, request.body().strip(), request.requestKey(), clock.instant());
      audit.record(
          "AGENT_TEST_MESSAGE",
          "AGENT_MESSAGE",
          message,
          "Registró entrada de prueba; sus respuestas no se envían a WhatsApp.");
    }
    inbox.register(useKapso ? "KAPSO" : "TWILIO", conversation, message);
    var run = runs.enqueue(message, conversation, config.getModel(), clock.instant());
    return new TestResult(conversation, run.id());
  }

  @Transactional(readOnly = true)
  public PageResponse<AgentRun> runs(UUID conversation, PageQuery query, String state) {
    inbox.conversation(conversation, false).orElseThrow(ApiException::notFound);
    if (state != null
        && !state.isBlank()
        && !Set.of("QUEUED", "PROCESSING", "COMPLETED", "FAILED", "GROUPED").contains(state))
      throw ApiException.badRequest("Estado de ejecución inválido.");
    return runs.runs(conversation, query, state);
  }

  public record Detail(
      AgentRun run, String incomingText, String source, Proposal proposal, Message reply) {}

  @Transactional(readOnly = true)
  public Detail detail(UUID id) {
    var run = runs.get(id, false).orElseThrow(ApiException::notFound);
    var input = inbox.message(run.messageId(), false).orElseThrow();
    return new Detail(
        run,
        input.body(),
        input.source(),
        runs.proposalByRun(id).orElse(null),
        inbox.reply(id).map(Message::of).orElse(null));
  }

  @Transactional(readOnly = true)
  public Proposal proposal(UUID conversation) {
    inbox.conversation(conversation, false).orElseThrow(ApiException::notFound);
    return runs.currentProposal(conversation).orElse(null);
  }

  @Transactional(readOnly = true)
  public PageResponse<Step> steps(UUID id, PageQuery query, String kind) {
    runs.get(id, false).orElseThrow(ApiException::notFound);
    if (kind != null && !kind.isBlank() && !Set.of("MODEL", "TOOL", "BOOKING").contains(kind))
      throw ApiException.badRequest("Tipo de paso inválido.");
    return runs.steps(id, query, kind);
  }

  @Transactional
  public AgentRun retry(UUID id) {
    if (!allowed() || !config.ready())
      throw ApiException.badRequest("El agente no está configurado.");
    var run = runs.get(id, true).orElseThrow(ApiException::notFound);
    if (!run.state().equals("FAILED") || run.attempts() >= 3)
      throw ApiException.conflict(
          "Solo pueden reintentarse fallos, hasta tres intentos por mensaje.");
    if (!Objects.equals(runs.latestInbound(run.conversationId(), run.messageId()), run.messageId()))
      throw ApiException.conflict("Ya existe otro mensaje; envía una solicitud nueva.");
    runs.retry(id, clock.instant());
    audit.record("AGENT_RETRY", "AGENT_RUN", id, "Reintentó el análisis sin volver a reservar.");
    return runs.get(id, false).orElseThrow();
  }

  @Transactional
  public Message retryReply(UUID id) {
    var run = runs.get(id, true).orElseThrow(ApiException::notFound);
    var reply = inbox.reply(id).orElseThrow(ApiException::notFound);
    var conversation = inbox.conversation(run.conversationId(), true).orElseThrow();
    if (!kapso.ready()
        || !kapso.permits(conversation.phone())
        || conversation.lastInboundAt() == null
        || !conversation.lastInboundAt().isAfter(clock.instant().minusSeconds(86400)))
      throw ApiException.conflict(
          "Revisa la conexión y pide un mensaje nuevo si venció la ventana de respuesta.");
    if (!reply.status().equals("FAILED") || reply.providerSid() != null || reply.attempts() >= 3)
      throw ApiException.conflict(
          "Solo se reintentan rechazos confirmados sin referencia de envío, hasta tres intentos."
              + " Verifica los resultados inciertos en Kapso.");
    messages.retryReply(reply.id(), clock.instant());
    audit.record(
        "AGENT_REPLY_RETRY",
        "KAPSO_MESSAGE",
        reply.id(),
        "Reintentó solo el envío de la respuesta; no repitió la reserva.");
    return Message.of(messages.message(reply.id(), false).orElseThrow());
  }

  @Transactional
  public Optional<AgentRun> claim() {
    return runs.claim(
        clock.instant(), kapso.isEnabled() ? "KAPSO" : "TWILIO", config.getDebounceMilliseconds());
  }

  @Transactional
  public void step(UUID id, String kind, String name, Object args, Object result, String state) {
    runs.get(id, true).orElseThrow();
    runs.step(id, kind, name, args, result, state, clock.instant());
  }

  @Transactional
  public void usage(UUID id, int input, int output) {
    runs.usage(id, input, output);
  }

  @Transactional
  public void finish(UUID id, String text) {
    replies.complete(id, text);
    audit.recordAs(
        null,
        "Agente IA",
        "AGENT_COMPLETED",
        "AGENT_RUN",
        id,
        "Completó análisis y guardó su respuesta vinculada.");
  }

  @Transactional
  public void bookingConflict(UUID id, String detail, String alternatives) {
    replies.error(id, "BOOKING_VALIDATION", detail, alternatives);
    audit.recordAs(
        null,
        "Agente IA",
        "AGENT_BOOKING_CONFLICT",
        "AGENT_RUN",
        id,
        "La reserva se rechazó y se consultaron alternativas.");
  }

  @Transactional
  public void fail(UUID id, String code, String detail) {
    var run = runs.get(id, true).orElseThrow();
    var input = inbox.message(run.messageId(), false).orElseThrow();
    if (code.equals("RATE_LIMIT")
        && inbox.provider(run.conversationId()).equals("KAPSO")
        && !input.source().equals("APP_TEST")
        && run.attempts() < 3
        && Objects.equals(
            runs.latestInbound(run.conversationId(), run.messageId()), run.messageId())) {
      runs.scheduleRetry(id, clock.instant().plusSeconds(run.attempts() * 30L));
      return;
    }
    String response =
        code.equals("BOOKING_VALIDATION")
            ? "No pude registrar esa cita: " + detail + " Podemos consultar otro horario."
            : "Ahora no pude completar la consulta. Puedes intentarlo nuevamente o pedir ayuda a"
                + " recepción. No se creó una cita por este error.";
    replies.error(id, code, detail, response);
    audit.recordAs(
        null, "Agente IA", "AGENT_FAILED", "AGENT_RUN", id, "Registró fallo controlado: " + code);
  }
}
