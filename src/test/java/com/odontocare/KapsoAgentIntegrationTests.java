package com.odontocare;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.agent.config.AgentProperties;
import com.odontocare.agent.dto.AgentContracts.*;
import com.odontocare.agent.model.AgentRun;
import com.odontocare.agent.service.*;
import com.odontocare.security.model.Permission;
import com.odontocare.whatsapp.service.TwilioSender;
import java.time.*;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;

@SpringBootTest(
    properties = {
      "odontocare.ai.enabled=true",
      "odontocare.ai.api-key=gsk_test_key",
      "odontocare.ai.worker-enabled=false",
      "odontocare.whatsapp.worker-enabled=false",
      "odontocare.kapso.enabled=true",
      "odontocare.kapso.agent-enabled=true",
      "odontocare.kapso.worker-enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KapsoAgentIntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired AgentQueueService queue;
  @Autowired BookingAgent agent;
  @Autowired AgentToolService tools;
  @Autowired AgentProperties config;
  @MockitoBean LanguageModelClient model;
  @MockitoBean TwilioSender sender;
  @MockitoBean com.odontocare.kapso.service.KapsoSender kapsoSender;
  @Autowired com.odontocare.kapso.config.KapsoProperties kapso;
  @Autowired com.odontocare.kapso.service.KapsoMessagingService transport;
  @Autowired com.odontocare.kapso.repository.KapsoRepository outbox;
  @Autowired AgentBookingService booking;
  @Autowired com.odontocare.appointments.service.AppointmentService manual;
  @Autowired com.odontocare.agent.repository.AgentRepository runs;
  MockHttpSession admin;
  UUID serviceId, doctorId, patientId;
  String password, phone = "+51987654321";

  @BeforeEach
  void prepare() throws Exception {
    assertThat(jdbc.queryForObject("select current_database()", String.class))
        .isEqualTo("sistema_odontologo_test");
    jdbc.execute(
        "TRUNCATE"
            + " kapso_webhook_event,kapso_message,kapso_conversation,agent_proposal,agent_slot,agent_step,agent_run,agent_message_source,agent_conversation_source,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update(
        "UPDATE installation_profile SET"
            + " time_zone='America/Lima',minimum_lead_minutes=0,appointment_gap_minutes=0,patient_next_number=1,version=0");
    jdbc.update("DELETE FROM role_permission");
    for (var p : Permission.values())
      jdbc.update("INSERT INTO role_permission VALUES('ADMIN',?)", p.name());
    config.setEnabled(true);
    config.setApiKey("gsk_test_key");
    config.setWorkerEnabled(false);
    config.setMaxModelCalls(6);
    config.setDebounceMilliseconds(0);
    kapso.setEnabled(true);
    kapso.setAgentEnabled(true);
    kapso.setWorkerEnabled(false);
    kapso.setApiKey("kapso-test-key");
    kapso.setPhoneNumberId("123456789012345");
    kapso.setSender("+56920403095");
    kapso.setWebhookSecret("kapso-test-hook-secret-for-agent");
    kapso.setPublicBaseUrl("https://wa.example.test");
    kapso.setAllowedParticipants(List.of(phone));
    reset(model, sender, kapsoSender);
    password = "Test-" + UUID.randomUUID();
    String username = "admin" + UUID.randomUUID().toString().substring(0, 8);
    call(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", username, "displayName", "Admin agente", "password", password),
        201);
    admin = login(username);
    var category = postData("/api/v1/categories", Map.of("name", "General", "active", true));
    serviceId =
        id(
            postData(
                "/api/v1/services",
                Map.of(
                    "name",
                    "Limpieza dental",
                    "categoryId",
                    category.path("id").asString(),
                    "price",
                    100,
                    "durationMinutes",
                    60,
                    "description",
                    "",
                    "bookableByAgent",
                    true,
                    "active",
                    true)));
    var user =
        postData(
            "/api/v1/users",
            Map.of(
                "username",
                "doctoragente",
                "displayName",
                "Doctora Demo",
                "email",
                "",
                "password",
                password,
                "roles",
                List.of("DENTIST"),
                "active",
                true));
    doctorId =
        id(
            postData(
                "/api/v1/dentists",
                Map.of(
                    "userId",
                    user.path("id").asString(),
                    "fullName",
                    "Doctora Demo",
                    "licenseNumber",
                    "AGENTE-001",
                    "specialty",
                    "General",
                    "serviceIds",
                    List.of(serviceId),
                    "active",
                    true)));
    for (int day = 1; day <= 7; day++)
      postData(
          "/api/v1/schedules/periods",
          Map.of(
              "dentistId",
              doctorId,
              "dayOfWeek",
              day,
              "kind",
              "WORK",
              "startMinute",
              540,
              "endMinute",
              1020,
              "active",
              true));
    var patient = new LinkedHashMap<String, Object>();
    patient.put("fullName", "Paciente Agente");
    patient.put("birthDate", "1990-01-01");
    patient.put("documentType", "");
    patient.put("documentNumber", "");
    patient.put("address", "");
    patient.put("email", "");
    patient.put("emergencyName", "");
    patient.put("emergencyPhone", "");
    patient.put("notes", "");
    patient.put("provisional", false);
    patient.put("active", true);
    patient.put(
        "contacts",
        List.of(
            Map.of(
                "phone",
                phone,
                "name",
                "Paciente Agente",
                "relationship",
                "Titular",
                "guardian",
                false,
                "payer",
                true)));
    patientId = id(postData("/api/v1/patients", patient));
  }

  private UUID id(JsonNode n) {
    return UUID.fromString(n.path("id").asString());
  }

  private JsonNode call(
      MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int status)
      throws Exception {
    if (session != null) request.session(session);
    if (body != null)
      request.contentType("application/json").content(mapper.writeValueAsString(body));
    var r = mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse();
    return r.getContentAsString().isBlank() ? null : mapper.readTree(r.getContentAsString());
  }

  private JsonNode postData(String path, Object body) throws Exception {
    return call(post(path).with(csrf()), admin, body, 201);
  }

  private MockHttpSession login(String username) throws Exception {
    var s = new MockHttpSession();
    mvc.perform(
            post("/api/v1/auth/login")
                .session(s)
                .with(csrf())
                .param("username", username)
                .param("password", password))
        .andExpect(status().isOk());
    return s;
  }

  private TestResult submit(String text) {
    try {
      String reference =
          "wamid." + UUID.randomUUID().toString().replace("-", "") + "ABCDEFGHIJKLMNOPQRSTUVWXYZ==";
      event(reference, "received", text, UUID.randomUUID().toString());
      return jdbc.queryForObject(
          "SELECT r.conversation_id,r.id FROM agent_run r JOIN kapso_message m ON m.id=r.message_id"
              + " WHERE m.provider_sid=?",
          (r, i) -> new TestResult(r.getObject(1, UUID.class), r.getObject(2, UUID.class)),
          reference);
    } catch (Exception failure) {
      throw new IllegalStateException(failure);
    }
  }

  private AgentRun process() {
    var run = queue.claim().orElseThrow();
    agent.process(run);
    return queue.detail(run.id()).run();
  }

  @Test
  void correctsUngroundedCatalogueClaimsBeforeSendingAnyResponse() {
    when(model.reply(anyList(), anyList()))
        .thenReturn(new LanguageModelClient.Reply("S/. 120 y 50 minutos", List.of(), 30, 10))
        .thenReturn(tool("consultar_servicios", Map.of("search", "limpieza")))
        .thenReturn(
            new LanguageModelClient.Reply(
                "La limpieza cuesta PEN 100.00 y dura 60 minutos.", List.of(), 30, 10));
    submit("¿Cuánto cuesta y dura la limpieza? Solo información.");
    var done = process();
    assertThat(done.state()).isEqualTo("COMPLETED");
    assertThat(done.responseText())
        .contains("100.00", "60 minutos")
        .doesNotContain("120", "50 minutos");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM appointment", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM kapso_message WHERE direction='OUTBOUND'", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT body FROM kapso_message WHERE direction='OUTBOUND'", String.class))
        .isEqualTo(done.responseText());
    verify(model, times(3)).reply(anyList(), anyList());
  }

  @Test
  void boundedFailureNeverSendsRepeatedInventedPrices() {
    when(model.reply(anyList(), anyList()))
        .thenReturn(new LanguageModelClient.Reply("S/. 120 y 50 minutos", List.of(), 30, 10));
    submit("¿Cuánto cuesta la limpieza? Solo información.");
    var done = process();
    assertThat(done.state()).isEqualTo("FAILED");
    assertThat(
            jdbc.queryForObject(
                "SELECT body FROM kapso_message WHERE direction='OUTBOUND'", String.class))
        .doesNotContain("120", "50 minutos");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM appointment", Integer.class)).isZero();
    verify(model, times(6)).reply(anyList(), anyList());
  }

  private void scriptProposal(UUID patient, String name) {
    var index = new AtomicInteger();
    when(model.reply(anyList(), anyList()))
        .thenAnswer(
            invocation -> {
              int step = index.getAndIncrement();
              if (step == 0) return tool("consultar_servicios", Map.of("search", "limpieza"));
              if (step == 1)
                return tool(
                    "consultar_horarios",
                    Map.of(
                        "service_id", serviceId, "days_from_today", 1, "preferred_time", "09:00"));
              if (step == 2) {
                List<Map<String, Object>> messages = invocation.getArgument(0);
                var result = mapper.readTree(messages.getLast().get("content").toString());
                var args = new LinkedHashMap<String, Object>();
                args.put("slot_id", result.path("items").path(0).path("slot_id").asString());
                if (patient != null) args.put("patient_id", patient);
                args.put("patient_name", name);
                return tool("proponer_cita", args);
              }
              return new LanguageModelClient.Reply("Oferta preparada", List.of(), 30, 10);
            });
  }

  @Test
  void requestedWeekdayOverridesIncorrectModelOffsetAndRespectsHoliday() {
    var requested =
        LocalDate.now(ZoneId.of("America/Lima"))
            .with(java.time.temporal.TemporalAdjusters.next(DayOfWeek.MONDAY));
    jdbc.update(
        "INSERT INTO schedule_exception(id,kind,start_date,end_date,reason)"
            + " VALUES(?,'HOLIDAY',?,?,'Cierre general')",
        UUID.randomUUID(),
        requested,
        requested);
    submit("Paciente Agente quiere limpieza con Doctora Demo el próximo lunes a las 9");
    var run = queue.claim().orElseThrow();
    var result =
        mapper.valueToTree(
            tools.execute(
                run,
                "consultar_horarios",
                mapper.valueToTree(
                    Map.of(
                        "service_id",
                        serviceId,
                        "dentist_name",
                        "Doctora Demo",
                        "days_from_today",
                        2,
                        "preferred_time",
                        "09:00"))));
    assertThat(result.path("date").asString()).isEqualTo(requested.toString());
    assertThat(result.path("items").size()).isZero();
    var wrongSlot =
        runs.addSlot(
            run.id(),
            run.conversationId(),
            doctorId,
            serviceId,
            requested.plusDays(1).atTime(9, 0),
            60,
            "America/Lima",
            Instant.now().plusSeconds(1800),
            "Limpieza dental",
            "Doctora Demo");
    assertThatThrownBy(
            () ->
                tools.execute(
                    run,
                    "proponer_cita",
                    mapper.valueToTree(
                        Map.of(
                            "slot_id",
                            wrongSlot.id(),
                            "patient_name",
                            "Paciente Agente",
                            "patient_id",
                            patientId))))
        .isInstanceOf(com.odontocare.shared.web.ApiException.class)
        .hasMessageContaining("día solicitado");
    assertThat(appointments()).isZero();
  }

  @Test
  void rejectsLegacyProposalForAnotherWeekdayEvenAfterExpressConfirmation() throws Exception {
    var proposal = offered();
    String requested =
        LocalDate.now(ZoneId.of("America/Lima")).plusDays(1).getDayOfWeek() == DayOfWeek.MONDAY
            ? "martes"
            : "lunes";
    jdbc.update(
        "UPDATE kapso_message SET body=? WHERE id=(SELECT message_id FROM agent_run WHERE id=?)",
        "Paciente Agente solicitó limpieza el próximo " + requested + " a las 9",
        proposal.runId());
    submit("Sí, confirmo la cita");
    var done = process();
    assertThat(done.state()).isEqualTo("FAILED");
    assertThat(done.errorCode()).isEqualTo("BOOKING_VALIDATION");
    assertThat(appointments()).isZero();
  }

  private LanguageModelClient.Reply tool(String name, Object args) {
    return new LanguageModelClient.Reply(
        "",
        List.of(
            new LanguageModelClient.ToolCall(
                UUID.randomUUID().toString(), name, mapper.writeValueAsString(args))),
        30,
        10);
  }

  private Proposal propose(UUID patient, String name) {
    scriptProposal(patient, name);
    var result = submit("Soy " + name + ". Limpieza mañana a las9.");
    assertThat(process().state()).isEqualTo("COMPLETED");
    return queue.proposal(result.conversationId());
  }

  private long appointments() {
    return jdbc.queryForObject("SELECT count(*) FROM appointment", Long.class);
  }

  private void event(String reference, String state, String text, String key) throws Exception {
    boolean incoming = state.equals("received");
    String payload =
        mapper.writeValueAsString(
            Map.of(
                "phone_number_id",
                kapso.getPhoneNumberId(),
                "message",
                Map.of(
                    "id",
                    reference,
                    "timestamp",
                    Long.toString(Instant.now().getEpochSecond()),
                    "type",
                    "text",
                    incoming ? "from" : "to",
                    phone.substring(1),
                    "text",
                    Map.of("body", text),
                    "kapso",
                    Map.of(
                        "direction",
                        incoming ? "inbound" : "outbound",
                        "status",
                        incoming ? "delivered" : state)),
                "conversation",
                Map.of(
                    "phone_number_id",
                    kapso.getPhoneNumberId(),
                    "phone_number",
                    phone,
                    "contact_name",
                    "Contacto Demo")));
    var mac = javax.crypto.Mac.getInstance("HmacSHA256");
    mac.init(
        new javax.crypto.spec.SecretKeySpec(
            kapso.getWebhookSecret().getBytes(java.nio.charset.StandardCharsets.UTF_8),
            "HmacSHA256"));
    mvc.perform(
            post("/api/v1/integrations/kapso/events")
                .contentType("application/json")
                .header(
                    "X-Webhook-Signature",
                    HexFormat.of()
                        .formatHex(
                            mac.doFinal(payload.getBytes(java.nio.charset.StandardCharsets.UTF_8))))
                .header("X-Webhook-Event", "whatsapp.message." + state)
                .header("X-Idempotency-Key", key)
                .header("X-Webhook-Payload-Version", "v2")
                .content(payload))
        .andExpect(status().isOk());
  }

  private void delivered(UUID run) throws Exception {
    var reply = queue.detail(run).reply();
    assertThat(reply).isNotNull();
    var claimed = transport.claim().orElseThrow();
    assertThat(claimed.id()).isEqualTo(reply.id());
    String reference =
        "wamid." + UUID.randomUUID().toString().replace("-", "") + "ABCDEFGHIJKLMNOPQRSTUVWXYZ==";
    transport.finish(
        reply.id(),
        new com.odontocare.kapso.service.KapsoSender.Outcome(reference, "ACCEPTED", null, null));
    event(reference, "delivered", reply.body(), UUID.randomUUID().toString());
  }

  private Proposal offered() throws Exception {
    var proposal = propose(patientId, "Paciente Agente");
    delivered(proposal.runId());
    return proposal;
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  @Test
  void appProposalAndDiscardNeverDisplaceRealPendingBooking() throws Exception {
    var real = offered();
    scriptProposal(patientId, "Paciente Agente");
    var preview =
        queue.test(
            new TestMessage(
                phone, "Demo", "Paciente Agente quiere limpieza mañana 9", UUID.randomUUID()));
    process();
    var app = queue.proposal(preview.conversationId());
    assertThat(
            runs.proposalByCode(real.conversationId(), real.confirmationCode())
                .orElseThrow()
                .state())
        .isEqualTo("PENDING");
    assertThat(app.id()).isNotEqualTo(real.id());
    queue.test(new TestMessage(phone, "Demo", "No reserves", UUID.randomUUID()));
    var discarded = queue.claim().orElseThrow();
    tools.execute(discarded, "descartar_propuesta", mapper.createObjectNode());
    queue.finish(discarded.id(), "Propuesta de prueba descartada.");
    assertThat(
            runs.proposalByCode(app.conversationId(), app.confirmationCode()).orElseThrow().state())
        .isEqualTo("SUPERSEDED");
    assertThat(
            runs.proposalByCode(real.conversationId(), real.confirmationCode())
                .orElseThrow()
                .state())
        .isEqualTo("PENDING");
    submit("Sí, confirmo la cita");
    assertThat(process().state()).isEqualTo("COMPLETED");
    assertThat(appointments()).isEqualTo(1);
    assertThat(
            runs.proposalByCode(real.conversationId(), real.confirmationCode())
                .orElseThrow()
                .state())
        .isEqualTo("CONFIRMED");
  }

  @Test
  void ambiguousAcknowledgementClarifiesWithoutModelCallOrNewProposal() throws Exception {
    var proposal = offered();
    clearInvocations(model);
    submit("sí");
    var done = process();
    assertThat(done.state()).isEqualTo("COMPLETED");
    assertThat(done.responseText()).contains("confirmación expresa", proposal.confirmationCode());
    assertThat(appointments()).isZero();
    assertThat(queue.proposal(done.conversationId()).id()).isEqualTo(proposal.id());
    verifyNoInteractions(model);
  }

  @Test
  void realChannelBooksAfterDeliveredSummaryAndNaturalConfirmationAndNeverDuplicates()
      throws Exception {
    var proposal = offered();
    assertThat(appointments()).isZero();
    assertThat(proposal.summary())
        .contains(
            LocalDate.now(ZoneId.of("America/Lima"))
                .plusDays(1)
                .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")));
    submit("Sí, confirmo la cita");
    var done = process();
    assertThat(done.state()).isEqualTo("COMPLETED");
    assertThat(appointments()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT status||':'||origin FROM appointment", String.class))
        .isEqualTo("CONFIRMED:WHATSAPP");
    assertThat(queue.detail(done.id()).reply()).isNotNull();
    delivered(done.id());
    submit("CONFIRMO " + proposal.confirmationCode());
    var repeated = process();
    assertThat(repeated.state()).isEqualTo("COMPLETED");
    assertThat(appointments()).isEqualTo(1);
    assertThat(count("charge_entry")).isZero();
    assertThat(count("money_movement")).isZero();
  }

  @Test
  void confirmationBeforeTheProposalIsSentDoesNotBook() {
    var proposal = propose(patientId, "Paciente Agente");
    submit("Sí, confirmo");
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(appointments()).isZero();
    assertThat(queue.proposal(proposal.conversationId()).state()).isEqualTo("PENDING");
  }

  @Test
  void negationAndAmbiguousAcknowledgementDoNotReserve() throws Exception {
    offered();
    when(model.reply(anyList(), anyList()))
        .thenReturn(
            new LanguageModelClient.Reply(
                "¿Quieres confirmar los datos de la propuesta?", List.of(), 20, 10));
    for (String body :
        List.of("sí", "No confirmo ninguna reserva", "Sí, pero todavía no me reserves")) {
      submit(body);
      process();
      assertThat(appointments()).isZero();
    }
  }

  @Test
  void repeatedIncomingEventEnqueuesOneRunAndReply() {
    when(model.reply(anyList(), anyList()))
        .thenReturn(new LanguageModelClient.Reply("¿Para quién es la cita?", List.of(), 20, 10));
    String id = "wamid." + UUID.randomUUID().toString().replace("-", "");
    String key = UUID.randomUUID().toString();
    try {
      event(id, "received", "Quiero una cita", key);
      event(id, "received", "Quiero una cita", key);
      event(id, "received", "Quiero una cita", UUID.randomUUID().toString());
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
    assertThat(count("agent_run")).isEqualTo(1);
    process();
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM kapso_message WHERE direction='OUTBOUND'", Long.class))
        .isEqualTo(1);
    verify(model, times(1)).reply(anyList(), anyList());
  }

  @Test
  void rapidMessagesAreGroupedWithoutLosingTheirContext() {
    config.setDebounceMilliseconds(1500);
    submit("Necesito una limpieza");
    submit("La cita es para Paciente Agente");
    assertThat(queue.claim()).isEmpty();
    jdbc.update("UPDATE agent_run SET created_at=now()-interval '3 seconds'");
    when(model.reply(anyList(), anyList()))
        .thenAnswer(
            i -> {
              List<Map<String, Object>> history = i.getArgument(0);
              assertThat(history.toString())
                  .contains("Necesito una limpieza", "La cita es para Paciente Agente");
              return new LanguageModelClient.Reply("¿Qué día prefieres?", List.of(), 20, 10);
            });
    process();
    assertThat(
            jdbc.queryForObject("SELECT count(*) FROM agent_run WHERE state='GROUPED'", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM kapso_message WHERE direction='OUTBOUND'", Long.class))
        .isEqualTo(1);
  }

  @Test
  void rejectedReplyCanRetryWithoutRunningTheModelOrBookingAgain() throws Exception {
    offered();
    submit("Sí, confirmo");
    var done = process();
    var reply = queue.detail(done.id()).reply();
    transport.claim().orElseThrow();
    transport.finish(
        reply.id(),
        new com.odontocare.kapso.service.KapsoSender.Outcome(
            null, "FAILED", "AUTH", "Clave rechazada"));
    int modelCalls = org.mockito.Mockito.mockingDetails(model).getInvocations().size();
    call(
        post("/api/v1/whatsapp/agent/runs/" + done.id() + "/reply/retry").with(csrf()),
        admin,
        Map.of(),
        200);
    assertThat(outbox.message(reply.id(), false).orElseThrow().status()).isEqualTo("QUEUED");
    assertThat(appointments()).isEqualTo(1);
    assertThat(org.mockito.Mockito.mockingDetails(model).getInvocations()).hasSize(modelCalls);
    assertThat(queue.proposal(done.conversationId()).appointmentId()).isNotNull();
  }

  @Test
  void interruptedRunAndRateLimitRecoverWithBoundedAttempts() {
    var initial = submit("Quiero una cita");
    jdbc.update(
        "UPDATE agent_run SET state='PROCESSING',attempts=1,updated_at=now()-interval '6 minutes'"
            + " WHERE id=?",
        initial.runId());
    when(model.reply(anyList(), anyList())).thenThrow(new ModelFailure("RATE_LIMIT", "Espera"));
    var run = process();
    assertThat(run.state()).isEqualTo("QUEUED");
    assertThat(run.attempts()).isEqualTo(2);
    assertThat(queue.detail(run.id()).reply()).isNull();
    jdbc.update(
        "UPDATE agent_run SET next_attempt_at=now()-interval '1 minute' WHERE id=?", run.id());
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(queue.detail(run.id()).reply()).isNotNull();
    assertThat(appointments()).isZero();
  }

  @Test
  void unknownDeliveryIsNotRetriedAndBookingSurvives() throws Exception {
    offered();
    submit("Sí, confirmo");
    var done = process();
    var reply = queue.detail(done.id()).reply();
    transport.claim().orElseThrow();
    transport.finish(
        reply.id(),
        new com.odontocare.kapso.service.KapsoSender.Outcome(null, "UNKNOWN", null, "Incierto"));
    call(
        post("/api/v1/whatsapp/agent/runs/" + done.id() + "/reply/retry").with(csrf()),
        admin,
        Map.of(),
        409);
    assertThat(appointments()).isEqualTo(1);
    assertThat(queue.proposal(done.conversationId()).state()).isEqualTo("CONFIRMED");
  }

  @Test
  void receptionAndAgentCompeteForTheSameSlotWithOneWinner() throws Exception {
    var proposal = offered();
    submit("Sí, confirmo");
    var run = queue.claim().orElseThrow();
    var slot = runs.slot(proposal.slotId()).orElseThrow();
    var executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    var gate = new java.util.concurrent.CountDownLatch(1);
    try {
      var automated =
          executor.submit(
              () -> {
                gate.await();
                try {
                  booking.confirm(run, proposal.confirmationCode());
                  return true;
                } catch (RuntimeException conflict) {
                  return false;
                }
              });
      var reception =
          executor.submit(
              () -> {
                gate.await();
                try {
                  manual.create(
                      new com.odontocare.appointments.dto.AppointmentRequest(
                          patientId,
                          doctorId,
                          serviceId,
                          "",
                          null,
                          slot.localStart(),
                          "Reserva concurrente recepción",
                          UUID.randomUUID()));
                  return true;
                } catch (RuntimeException conflict) {
                  return false;
                }
              });
      gate.countDown();
      boolean a = automated.get(15, java.util.concurrent.TimeUnit.SECONDS),
          b = reception.get(15, java.util.concurrent.TimeUnit.SECONDS);
      assertThat(a ^ b).isTrue();
      assertThat(appointments()).isEqualTo(1);
    } finally {
      executor.shutdownNow();
    }
  }

  @Test
  void confirmedAppointmentAndReplyAreOneTransaction() throws Exception {
    var proposal = offered();
    submit("Sí, confirmo");
    var run = queue.claim().orElseThrow();
    jdbc.execute(
        "CREATE FUNCTION test_reject_agent_reply() RETURNS trigger LANGUAGE plpgsql AS $$ BEGIN IF"
            + " NEW.source='AGENT' THEN RAISE EXCEPTION 'controlled reply failure'; END IF; RETURN"
            + " NEW; END $$");
    jdbc.execute(
        "CREATE TRIGGER test_agent_reply_failure BEFORE INSERT ON kapso_message FOR EACH ROW"
            + " EXECUTE FUNCTION test_reject_agent_reply()");
    try {
      assertThatThrownBy(() -> booking.confirm(run, proposal.confirmationCode()))
          .isInstanceOf(RuntimeException.class);
      assertThat(appointments()).isZero();
      assertThat(queue.proposal(run.conversationId()).state()).isEqualTo("PENDING");
    } finally {
      jdbc.execute("DROP TRIGGER test_agent_reply_failure ON kapso_message");
      jdbc.execute("DROP FUNCTION test_reject_agent_reply()");
    }
  }

  @Test
  void occupiedPreferenceReturnsOnlyCalculatedAlternatives() {
    var tomorrow = LocalDate.now(ZoneId.of("America/Lima")).plusDays(1);
    manual.create(
        new com.odontocare.appointments.dto.AppointmentRequest(
            patientId,
            doctorId,
            serviceId,
            "",
            null,
            tomorrow.atTime(9, 0),
            "Ocupado",
            UUID.randomUUID()));
    var index = new AtomicInteger();
    when(model.reply(anyList(), anyList()))
        .thenAnswer(
            i -> {
              if (index.getAndIncrement() == 0)
                return tool(
                    "consultar_horarios",
                    Map.of(
                        "service_id", serviceId, "days_from_today", 1, "preferred_time", "09:00"));
              List<Map<String, Object>> context = i.getArgument(0);
              var result = mapper.readTree(context.getLast().get("content").toString());
              assertThat(result.path("preferred_time_available").asBoolean()).isFalse();
              assertThat(result.path("items").path(0).path("local_start").asString())
                  .endsWith("10:00");
              return new LanguageModelClient.Reply(
                  "Las 9 están ocupadas por tu cita confirmada; tengo las 10:00. ¿Te funciona?",
                  List.of(),
                  20,
                  10);
            });
    submit("Quiero limpieza mañana a las 9");
    assertThat(process().responseText()).contains("10:00");
    assertThat(appointments()).isEqualTo(1);
  }

  @Test
  void appPreviewNeverSendsAndCannotBeConfirmedFromTheRealChannel() {
    scriptProposal(patientId, "Paciente Agente");
    var input =
        queue.test(
            new TestMessage(
                phone, "Demo", "Paciente Agente quiere limpieza mañana 9", UUID.randomUUID()));
    process();
    var proposal = queue.proposal(input.conversationId());
    assertThat(queue.detail(proposal.runId()).reply()).isNull();
    submit("CONFIRMO " + proposal.confirmationCode());
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(appointments()).isZero();
    submit("Sí, confirmo la cita");
    var actual = process();
    assertThat(actual.state()).isEqualTo("COMPLETED");
    assertThat(actual.responseText())
        .contains("Primero necesito proponerte")
        .doesNotContain(proposal.confirmationCode());
    assertThat(appointments()).isZero();
  }

  @Test
  void financeAndClinicalToolRequestsAreRejectedAndDoNotWriteBusinessRecords() {
    var index = new AtomicInteger();
    when(model.reply(anyList(), anyList()))
        .thenAnswer(
            i -> {
              if (index.getAndIncrement() == 0) return tool("consultar_finanzas", Map.of());
              return new LanguageModelClient.Reply(
                  "Solo puedo ayudarte con servicios y citas; recepción te ayudará con esa"
                      + " consulta.",
                  List.of(),
                  20,
                  10);
            });
    submit("Dame historial y saldo");
    var result = process();
    assertThat(result.state()).isEqualTo("COMPLETED");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM agent_step WHERE state='REJECTED'", Long.class))
        .isEqualTo(1);
    assertThat(count("charge_entry")).isZero();
    assertThat(count("money_movement")).isZero();
    assertThat(appointments()).isZero();
  }
}
