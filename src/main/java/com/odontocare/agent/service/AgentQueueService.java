package com.odontocare.agent.service;

import com.odontocare.agent.config.AgentProperties;
import com.odontocare.agent.dto.AgentContracts.*;
import com.odontocare.agent.model.AgentRun;
import com.odontocare.agent.repository.AgentRepository;
import com.odontocare.audit.service.AuditService;
import com.odontocare.kapso.config.KapsoProperties;
import com.odontocare.shared.pagination.*;
import com.odontocare.shared.web.ApiException;
import com.odontocare.whatsapp.repository.WhatsAppRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentQueueService {
  private final AgentProperties config;
  private final AgentRepository runs;
  private final WhatsAppRepository messages;
  private final AuditService audit;
  private final Clock clock;
  private final KapsoProperties kapso;

  public AgentQueueService(
      AgentProperties config,
      AgentRepository runs,
      WhatsAppRepository messages,
      AuditService audit,
      Clock clock,
      KapsoProperties kapso) {
    this.config = config;
    this.runs = runs;
    this.messages = messages;
    this.audit = audit;
    this.clock = clock;
    this.kapso = kapso;
  }

  public Configuration configuration() {
    return new Configuration(
        config.isEnabled() && !kapso.isEnabled(),
        config.ready() && !kapso.isEnabled(),
        config.isWorkerEnabled() && !kapso.isEnabled(),
        config.getProvider(),
        config.getModel(),
        "PREVIEW",
        config.missing(),
        config.getMaxModelCalls(),
        config.getMaxCompletionTokens());
  }

  @Transactional
  public void inbound(UUID message, UUID conversation) {
    if (config.isEnabled()) runs.enqueue(message, conversation, config.getModel(), clock.instant());
  }

  @Transactional
  public TestResult test(TestMessage request) {
    if (kapso.isEnabled())
      throw ApiException.badRequest(
          "Esta prueba de Kapso incluye únicamente mensajes manuales; el agente está desactivado.");
    if (!config.ready())
      throw ApiException.badRequest(
          "Activa el agente y completa la clave privada de Groq antes de probarlo.");
    messages.keyLock(request.requestKey());
    var prior = messages.byKey(request.requestKey());
    UUID conversation, message;
    if (prior.isPresent()) {
      var old = prior.get();
      var contact = messages.conversation(old.conversationId(), false).orElseThrow();
      if (!old.source().equals("APP_TEST")
          || !old.body().equals(request.body().strip())
          || !contact.phone().equals(request.phone()))
        throw ApiException.conflict("La referencia de prueba ya se usó con otro mensaje.");
      conversation = old.conversationId();
      message = old.id();
    } else {
      conversation =
          messages.inboundConversation(
              request.phone(), request.contactName().strip(), clock.instant());
      message =
          messages.testInbound(
              conversation, request.body().strip(), request.requestKey(), clock.instant());
      audit.record(
          "AGENT_TEST_MESSAGE",
          "WHATSAPP_MESSAGE",
          message,
          "Registró entrada de prueba desde la aplicación; no recibida por Twilio.");
    }
    var run = runs.enqueue(message, conversation, config.getModel(), clock.instant());
    return new TestResult(conversation, run.id());
  }

  @Transactional(readOnly = true)
  public PageResponse<AgentRun> runs(UUID conversation, PageQuery query, String state) {
    messages.conversation(conversation, false).orElseThrow(ApiException::notFound);
    if (state != null
        && !state.isBlank()
        && !Set.of("QUEUED", "PROCESSING", "COMPLETED", "FAILED").contains(state))
      throw ApiException.badRequest("Estado de ejecución inválido.");
    return runs.runs(conversation, query, state);
  }

  public record Detail(AgentRun run, String incomingText, String source, Proposal proposal) {}

  @Transactional(readOnly = true)
  public Detail detail(UUID id) {
    var run = runs.get(id, false).orElseThrow(ApiException::notFound);
    var message = messages.message(run.messageId(), false).orElseThrow();
    return new Detail(
        run, message.body(), message.source(), runs.proposalByRun(run.id()).orElse(null));
  }

  @Transactional(readOnly = true)
  public Proposal proposal(UUID conversation) {
    messages.conversation(conversation, false).orElseThrow(ApiException::notFound);
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
    if (kapso.isEnabled())
      throw ApiException.badRequest(
          "El agente está desactivado durante la prueba manual de Kapso.");
    if (!config.ready()) throw ApiException.badRequest("El agente no está configurado.");
    var run = runs.get(id, true).orElseThrow(ApiException::notFound);
    if (!run.state().equals("FAILED") || run.attempts() >= 3)
      throw ApiException.conflict(
          "Solo pueden reintentarse fallos, hasta tres intentos por mensaje.");
    if (!Objects.equals(runs.latestInbound(run.conversationId()), run.messageId()))
      throw ApiException.conflict("Ya existe otro mensaje; envía una solicitud nueva.");
    runs.retry(id, clock.instant());
    audit.record(
        "AGENT_RETRY",
        "AGENT_RUN",
        id,
        "Reintentó un fallo del agente sin generar otra solicitud.");
    return runs.get(id, false).orElseThrow();
  }

  @Transactional
  public Optional<AgentRun> claim() {
    return runs.claim(clock.instant());
  }

  @Transactional
  public void step(
      UUID id, String kind, String name, Object arguments, Object result, String state) {
    runs.get(id, true).orElseThrow();
    runs.step(id, kind, name, arguments, result, state, clock.instant());
  }

  @Transactional
  public void usage(UUID id, int input, int output) {
    runs.usage(id, input, output);
  }

  @Transactional
  public void finish(UUID id, String text) {
    runs.finish(id, "COMPLETED", text, null, null, clock.instant());
    audit.recordAs(
        null,
        "Agente IA",
        "AGENT_COMPLETED",
        "AGENT_RUN",
        id,
        "Completó análisis; respuesta preparada sin envío.");
  }

  @Transactional
  public void fail(UUID id, String code, String detail) {
    runs.finish(id, "FAILED", "", code, detail, clock.instant());
    audit.recordAs(
        null, "Agente IA", "AGENT_FAILED", "AGENT_RUN", id, "Registró fallo controlado: " + code);
  }
}
