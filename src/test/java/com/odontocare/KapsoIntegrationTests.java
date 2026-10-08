package com.odontocare;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.agent.config.*;
import com.odontocare.agent.dto.AgentContracts.TestMessage;
import com.odontocare.agent.service.*;
import com.odontocare.kapso.config.*;
import com.odontocare.kapso.repository.KapsoRepository;
import com.odontocare.kapso.service.*;
import com.odontocare.security.model.Permission;
import com.odontocare.whatsapp.config.*;
import com.odontocare.whatsapp.service.TwilioSender;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;

@SpringBootTest(
    properties = {
      "odontocare.kapso.enabled=true",
      "odontocare.kapso.worker-enabled=false",
      "odontocare.ai.worker-enabled=false",
      "odontocare.whatsapp.worker-enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class KapsoIntegrationTests {
  static final String HOOK = "/api/v1/integrations/kapso/events", API = "/api/v1/whatsapp";
  static final String NUMBER = "123456789012345",
      PHONE = "+51999998888",
      SECRET = "kapso-webhook-unit-secret-32-characters";
  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired KapsoProperties config;
  @Autowired KapsoMessagingService messages;
  @Autowired KapsoWorker worker;
  @Autowired KapsoRepository repository;
  @Autowired AgentProperties ai;
  @Autowired AgentWorker agentWorker;
  @Autowired AgentQueueService agentQueue;
  @Autowired WhatsAppProperties twilioConfig;
  @Autowired WhatsAppWorker twilioWorker;
  @MockitoBean KapsoSender sender;
  @MockitoBean TwilioSender twilioSender;
  @MockitoBean LanguageModelClient model;
  MockHttpSession admin;
  String username, password;

  @BeforeEach
  void prepare() throws Exception {
    assertThat(jdbc.queryForObject("select current_database()", String.class))
        .isEqualTo("sistema_odontologo_test");
    jdbc.execute(
        "TRUNCATE agent_appointment_reference,agent_change_proposal,agent_request_context,agent_supervision,"
            + " kapso_webhook_event,kapso_message,kapso_conversation,agent_proposal,agent_slot,agent_step,agent_run,agent_message_source,agent_conversation_source,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update("DELETE FROM role_permission");
    for (var permission : Permission.values())
      jdbc.update("INSERT INTO role_permission VALUES('ADMIN',?)", permission.name());
    config.setEnabled(true);
    config.setWorkerEnabled(false);
    config.setApiKey("kapso-private-test-key");
    config.setWebhookSecret(SECRET);
    config.setPhoneNumberId(NUMBER);
    config.setSender("+56920403095");
    config.setPublicBaseUrl("https://wa.example.test");
    config.setAllowedParticipants(List.of(PHONE));
    ai.setEnabled(true);
    ai.setApiKey("gsk_test_key");
    ai.setWorkerEnabled(false);
    twilioConfig.setWorkerEnabled(false);
    username = "kapso-admin-" + UUID.randomUUID().toString().substring(0, 8);
    password = "Test-" + UUID.randomUUID();
    call(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", username, "displayName", "Admin Kapso", "password", password),
        201);
    admin = login(username);
  }

  @AfterEach
  void disableWorkers() {
    ai.setWorkerEnabled(false);
    config.setWorkerEnabled(false);
    twilioConfig.setWorkerEnabled(false);
  }

  @Test
  void signedReceptionPreservesUnicodeAndDoesNotStartTheAgent() throws Exception {
    String text = "Hola, quiero reservar mañana a las 9. No confirmo. Precio 10%_especial ñ 😀";
    var payload = payload("RECEIVED", reference(), text, PHONE, Instant.now());
    var key = UUID.randomUUID().toString();
    hook(payload, "RECEIVED", key, 200);
    hook(payload, "RECEIVED", key, 200);
    hook(payload, "RECEIVED", UUID.randomUUID().toString(), 200);
    assertThat(count("kapso_message")).isEqualTo(1);
    assertThat(count("agent_run")).isZero();
    assertThat(count("appointment")).isZero();
    assertThat(count("patient")).isZero();
    assertThat(count("money_movement")).isZero();
    assertThat(count("whatsapp_message")).isZero();
    var page =
        call(
            get(API + "/conversations/" + conversation() + "/messages").param("search", text),
            admin,
            null,
            200);
    assertThat(page.path("items").get(0).path("body").asString()).isEqualTo(text);
    assertThat(page.path("items").get(0).path("source").asString()).isEqualTo("KAPSO");
    assertThat(call(get(API + "/connection"), admin, null, 200).path("agentEnabled").asBoolean())
        .isFalse();
    verifyNoInteractions(model);
  }

  @Test
  void realSandboxInboundMayAlreadyHaveDeliveredStatus() throws Exception {
    var data =
        payload(
            "RECEIVED",
            reference(),
            "Hola, esta es una prueba de conexión con OdontoCare",
            PHONE,
            Instant.now());
    @SuppressWarnings("unchecked")
    var message = (Map<String, Object>) data.get("message");
    @SuppressWarnings("unchecked")
    var metadata = (Map<String, Object>) message.get("kapso");
    metadata.put("status", "delivered");
    hook(data, "RECEIVED", UUID.randomUUID().toString(), 200);
    assertThat(count("kapso_message")).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT status FROM kapso_message", String.class))
        .isEqualTo("RECEIVED");
    assertThat(count("agent_run")).isZero();
    metadata.put("direction", "outbound");
    hook(data, "RECEIVED", UUID.randomUUID().toString(), 400);
  }

  @Test
  void verifiesRawSignatureNumberParticipantVersionAndSize() throws Exception {
    var data = payload("RECEIVED", reference(), "Hola", PHONE, Instant.now());
    String raw = mapper.writeValueAsString(data);
    mvc.perform(
            post(HOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Event", "whatsapp.message.received")
                .content(raw))
        .andExpect(status().isForbidden());
    mvc.perform(
            post(HOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Event", "whatsapp.message.received")
                .header("X-Webhook-Signature", sign(raw))
                .content(raw + " "))
        .andExpect(status().isForbidden());
    data.put("phone_number_id", "999999999999999");
    hook(data, "RECEIVED", UUID.randomUUID().toString(), 403);
    hook(
        payload("RECEIVED", reference(), "Hola", "+51900000000", Instant.now()),
        "RECEIVED",
        UUID.randomUUID().toString(),
        403);
    mvc.perform(
            post(HOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Event", "whatsapp.message.received")
                .header("X-Webhook-Signature", sign(raw))
                .header("X-Webhook-Payload-Version", "v1")
                .content(raw))
        .andExpect(status().isBadRequest());
    String large = " ".repeat(65537);
    mvc.perform(
            post(HOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Signature", sign(large))
                .content(large))
        .andExpect(status().isPayloadTooLarge());
    mvc.perform(get(HOOK)).andExpect(status().isUnauthorized());
    assertThat(count("kapso_message")).isZero();
  }

  @Test
  void sendingAndDeliveryUseLongReferencesAndNeverRegressFromRead() throws Exception {
    receive("Prueba de recepción");
    UUID id = send("Respuesta personalizada mañana ñ 😀", UUID.randomUUID());
    String reference = reference();
    when(sender.send(any(), eq(PHONE)))
        .thenReturn(new KapsoSender.Outcome(reference, "ACCEPTED", null, null));
    config.setWorkerEnabled(true);
    worker.sendPending();
    config.setWorkerEnabled(false);
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("ACCEPTED");
    hook(
        payload("READ", reference, "Respuesta", PHONE, Instant.now()),
        "READ",
        UUID.randomUUID().toString(),
        200);
    hook(
        payload("SENT", reference, "Respuesta", PHONE, Instant.now()),
        "SENT",
        UUID.randomUUID().toString(),
        200);
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("READ");
    assertThat(repository.message(id, false).orElseThrow().providerSid()).hasSizeGreaterThan(34);
    verify(sender, times(1)).send(any(), eq(PHONE));
    verifyNoInteractions(twilioSender, model);
  }

  @Test
  void earlyDeliveryReceiptSurvivesUntilSendResponseIsLinked() throws Exception {
    receive("Hola");
    UUID id = send("Una respuesta", UUID.randomUUID());
    var claimed = messages.claim().orElseThrow();
    assertThat(claimed.id()).isEqualTo(id);
    String reference = reference();
    hook(
        payload("READ", reference, "Una respuesta", PHONE, Instant.now()),
        "READ",
        UUID.randomUUID().toString(),
        200);
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("SENDING");
    messages.finish(id, new KapsoSender.Outcome(reference, "ACCEPTED", null, null));
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("READ");
    assertThat(count("kapso_webhook_event")).isEqualTo(2);
  }

  @Test
  void requestKeyIsIdempotentEvenUnderConcurrentRequests() throws Exception {
    receive("Hola");
    UUID conversation = conversation(), key = UUID.randomUUID();
    var pool = Executors.newFixedThreadPool(2);
    try {
      var gate = new CountDownLatch(1);
      var a =
          pool.submit(
              () -> {
                gate.await();
                return messages.enqueue(conversation, "Respuesta", key);
              });
      var b =
          pool.submit(
              () -> {
                gate.await();
                return messages.enqueue(conversation, "Respuesta", key);
              });
      gate.countDown();
      assertThat(a.get(10, TimeUnit.SECONDS).id()).isEqualTo(b.get(10, TimeUnit.SECONDS).id());
    } finally {
      pool.shutdownNow();
    }
    assertThat(count("kapso_message")).isEqualTo(2);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "Otro contenido", "requestKey", key),
        409);
    var payload = payload("RECEIVED", reference(), "Dato A", PHONE, Instant.now());
    String delivery = UUID.randomUUID().toString();
    hook(payload, "RECEIVED", delivery, 200);
    payload.put(
        "message", payload("RECEIVED", reference(), "Dato B", PHONE, Instant.now()).get("message"));
    hook(payload, "RECEIVED", delivery, 409);
  }

  @Test
  void responseWindowIsCheckedAtEnqueueAndDispatchAndHistoryDoesNotReopenIt() throws Exception {
    receive("Hola");
    UUID conversation = conversation();
    UUID id = send("Respuesta en cola", UUID.randomUUID());
    jdbc.update(
        "UPDATE kapso_conversation SET last_inbound_at=now()-interval '25 hours' WHERE id=?",
        conversation);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "Fuera de ventana", "requestKey", UUID.randomUUID()),
        400);
    config.setWorkerEnabled(true);
    worker.sendPending();
    config.setWorkerEnabled(false);
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("FAILED");
    verifyNoInteractions(sender);
    hook(
        payload("RECEIVED", reference(), "Histórico", PHONE, Instant.now().minusSeconds(172800)),
        "RECEIVED",
        UUID.randomUUID().toString(),
        200);
    assertThat(repository.conversation(conversation, false).orElseThrow().lastInboundAt())
        .isBefore(Instant.now().minusSeconds(86400));
    call(
        post(API + "/conversations/" + conversation + "/test-reply").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID()),
        400);
  }

  @Test
  void pagedSearchPermissionsCsrfAndPublicConfigurationAreEnforced() throws Exception {
    for (int i = 0; i < 21; i++) receive("Mensaje listado " + i);
    var page =
        call(
            get(API
                    + "/conversations/"
                    + conversation()
                    + "/messages?size=10&page=1&messageDirection=INBOUND&status=RECEIVED")
                .param("search", "Mensaje listado"),
            admin,
            null,
            200);
    assertThat(page.path("items").size()).isEqualTo(10);
    assertThat(page.path("totalElements").asLong()).isEqualTo(21);
    call(get(API + "/conversations?sort=untrusted"), admin, null, 400);
    call(get(API + "/conversations/" + conversation() + "/messages?size=101"), admin, null, 400);
    call(
        post(API + "/conversations/" + conversation() + "/messages"),
        admin,
        Map.of("body", "Sin CSRF", "requestKey", UUID.randomUUID()),
        403);
    call(get(API + "/connection"), null, null, 401);
    var view = call(get(API + "/connection"), admin, null, 200).toString();
    assertThat(view).contains("KAPSO_SANDBOX").doesNotContain("kapso-private-test-key", SECRET);
    String restricted = "kapso-caja-" + UUID.randomUUID().toString().substring(0, 8);
    call(
        post("/api/v1/users").with(csrf()),
        admin,
        Map.of(
            "username",
            restricted,
            "displayName",
            "Caja",
            "password",
            password,
            "email",
            "",
            "active",
            true,
            "roles",
            List.of("CASHIER")),
        201);
    call(get(API + "/conversations"), login(restricted), null, 403);
    call(
        post("/api/v1/whatsapp/agent/test-messages").with(csrf()),
        admin,
        Map.of(
            "phone",
            PHONE,
            "contactName",
            "Demo",
            "body",
            "Reservar",
            "requestKey",
            UUID.randomUUID()),
        400);
  }

  @Test
  void unsupportedMediaAndUnresolvedIdentityDoNotCreatePatientsOrInvokeAi() throws Exception {
    var image = payload("RECEIVED", reference(), "", PHONE, Instant.now());
    @SuppressWarnings("unchecked")
    var message = (Map<String, Object>) image.get("message");
    message.put("type", "image");
    message.remove("text");
    hook(image, "RECEIVED", UUID.randomUUID().toString(), 200);
    assertThat(jdbc.queryForObject("SELECT kind FROM kapso_message", String.class))
        .isEqualTo("UNSUPPORTED");
    var unknown = payload("RECEIVED", reference(), "Hola", PHONE, Instant.now());
    @SuppressWarnings("unchecked")
    var noPhone = (Map<String, Object>) unknown.get("message");
    noPhone.remove("from");
    @SuppressWarnings("unchecked")
    var contact = (Map<String, Object>) unknown.get("conversation");
    contact.remove("phone_number");
    contact.put("business_scoped_user_id", "PE.123456789");
    hook(unknown, "RECEIVED", UUID.randomUUID().toString(), 200);
    assertThat(count("kapso_message")).isEqualTo(1);
    assertThat(count("patient")).isZero();
    verifyNoInteractions(model);
  }

  @Test
  void boundedBatchAndOutOfOrderTimeKeepTheLatestWindow() throws Exception {
    var now = Instant.now();
    var batch =
        Map.of(
            "type",
            "whatsapp.message.received",
            "batch",
            true,
            "data",
            List.of(
                payload("RECEIVED", reference(), "Nuevo", PHONE, now),
                payload("RECEIVED", reference(), "Antiguo", PHONE, now.minusSeconds(86400))));
    hook(batch, "RECEIVED", UUID.randomUUID().toString(), 200);
    assertThat(count("kapso_message")).isEqualTo(2);
    assertThat(repository.conversation(conversation(), false).orElseThrow().lastMessagePreview())
        .isEqualTo("Nuevo");
    assertThat(repository.conversation(conversation(), false).orElseThrow().lastInboundAt())
        .isAfter(now.minusSeconds(2));
  }

  @Test
  void providerChangeAndUnknownSendDoNotDispatchOrRepeatOtherMessages() throws Exception {
    receive("Hola");
    UUID id = send("Texto", UUID.randomUUID());
    when(sender.send(any(), anyString()))
        .thenReturn(new KapsoSender.Outcome(null, "UNKNOWN", null, "Incierto"));
    config.setWorkerEnabled(true);
    worker.sendPending();
    worker.sendPending();
    config.setWorkerEnabled(false);
    verify(sender, times(1)).send(any(), eq(PHONE));
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("UNKNOWN");
    config.setPhoneNumberId("999999999999999");
    assertThat(call(get(API + "/conversations"), admin, null, 200).path("totalElements").asLong())
        .isZero();
    call(get(API + "/conversations/" + conversation()), admin, null, 404);
  }

  @Test
  void selectingKapsoPausesTwilioAndExistingAgentWork() throws Exception {
    config.setEnabled(false);
    UUID agentRun =
        agentQueue.test(new TestMessage(PHONE, "Demo", "Hola", UUID.randomUUID())).runId();
    config.setEnabled(true);
    ai.setWorkerEnabled(true);
    agentWorker.processPending();
    assertThat(
            jdbc.queryForObject("SELECT state FROM agent_run WHERE id=?", String.class, agentRun))
        .isEqualTo("QUEUED");
    verifyNoInteractions(model);
    twilioConfig.setEnabled(true);
    twilioConfig.setAccountSid("AC" + "1".repeat(32));
    twilioConfig.setAuthToken("twilio-test-key");
    twilioConfig.setSender("whatsapp:+14155238886");
    twilioConfig.setPublicBaseUrl("https://wa.example.test");
    twilioConfig.setAllowedParticipants(List.of(PHONE));
    twilioConfig.setSendMode("TEXT");
    twilioConfig.setWorkerEnabled(true);
    twilioWorker.sendPending();
    verifyNoInteractions(twilioSender);
    assertThat(
            call(get("/api/v1/whatsapp/agent/configuration"), admin, null, 200)
                .path("enabled")
                .asBoolean())
        .isFalse();
  }

  @Test
  void rateLimitRetriesAreBoundedAndFailedStatusRemainsTraceable() throws Exception {
    receive("Hola");
    UUID id = send("Texto", UUID.randomUUID());
    for (int i = 0; i < 3; i++) {
      jdbc.update(
          "UPDATE kapso_message SET next_attempt_at=now()-interval '1 minute' WHERE id=?", id);
      messages.claim().orElseThrow();
      messages.finish(id, new KapsoSender.Outcome(null, "RATE_LIMIT", "RATE_LIMIT", "Espera"));
    }
    assertThat(repository.message(id, false).orElseThrow().status()).isEqualTo("FAILED");
    assertThat(repository.message(id, false).orElseThrow().attempts()).isEqualTo(3);
    UUID another = send("Otra respuesta", UUID.randomUUID());
    messages.claim().orElseThrow();
    String reference = reference();
    messages.finish(another, new KapsoSender.Outcome(reference, "ACCEPTED", null, null));
    var failed = payload("FAILED", reference, "", PHONE, Instant.now());
    @SuppressWarnings("unchecked")
    var message = (Map<String, Object>) failed.get("message");
    @SuppressWarnings("unchecked")
    var kapso = (Map<String, Object>) message.get("kapso");
    kapso.put(
        "statuses",
        List.of(
            Map.of(
                "id",
                reference,
                "errors",
                List.of(Map.of("code", 131047, "message", "private upstream text")))));
    hook(failed, "FAILED", UUID.randomUUID().toString(), 200);
    assertThat(repository.message(another, false).orElseThrow().errorCode()).isEqualTo("131047");
    assertThat(repository.message(another, false).orElseThrow().errorMessage())
        .doesNotContain("private upstream");
  }

  Map<String, Object> payload(
      String state, String reference, String body, String phone, Instant timestamp) {
    boolean incoming = state.equals("RECEIVED");
    var message = new LinkedHashMap<String, Object>();
    message.put("id", reference);
    message.put("timestamp", Long.toString(timestamp.getEpochSecond()));
    message.put("type", "text");
    message.put(incoming ? "from" : "to", phone.substring(1));
    message.put("text", Map.of("body", body));
    message.put(
        "kapso",
        new LinkedHashMap<>(
            Map.of(
                "direction",
                incoming ? "inbound" : "outbound",
                "status",
                state.toLowerCase(Locale.ROOT),
                "origin",
                "cloud_api")));
    return new LinkedHashMap<>(
        Map.of(
            "message",
            message,
            "phone_number_id",
            NUMBER,
            "conversation",
            new LinkedHashMap<>(
                Map.of(
                    "phone_number_id",
                    NUMBER,
                    "phone_number",
                    phone,
                    "contact_name",
                    "Participante Kapso"))));
  }

  String reference() {
    return "wamid."
        + UUID.randomUUID().toString().replace("-", "")
        + "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789==";
  }

  String sign(String body) throws Exception {
    var mac = Mac.getInstance("HmacSHA256");
    mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
    return HexFormat.of().formatHex(mac.doFinal(body.getBytes(StandardCharsets.UTF_8)));
  }

  void hook(Object payload, String state, String key, int status) throws Exception {
    String body = mapper.writeValueAsString(payload);
    mvc.perform(
            post(HOOK)
                .contentType(MediaType.APPLICATION_JSON)
                .header("X-Webhook-Signature", sign(body))
                .header("X-Webhook-Event", "whatsapp.message." + state.toLowerCase(Locale.ROOT))
                .header("X-Idempotency-Key", key)
                .header("X-Webhook-Payload-Version", "v2")
                .content(body))
        .andExpect(status().is(status));
  }

  void receive(String body) throws Exception {
    hook(
        payload("RECEIVED", reference(), body, PHONE, Instant.now()),
        "RECEIVED",
        UUID.randomUUID().toString(),
        200);
  }

  UUID conversation() {
    return jdbc.queryForObject(
        "SELECT id FROM kapso_conversation WHERE phone=?", UUID.class, PHONE);
  }

  UUID send(String body, UUID key) throws Exception {
    return UUID.fromString(
        call(
                post(API + "/conversations/" + conversation() + "/messages").with(csrf()),
                admin,
                Map.of("body", body, "requestKey", key),
                200)
            .path("id")
            .asString());
  }

  long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  MockHttpSession login(String login) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .param("username", login)
                    .param("password", password))
            .andExpect(status().isOk())
            .andReturn()
            .getRequest()
            .getSession(false);
  }

  JsonNode call(
      MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int status)
      throws Exception {
    if (session != null) request.session(session);
    if (body != null)
      request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
    String response =
        mvc.perform(request)
            .andExpect(status().is(status))
            .andReturn()
            .getResponse()
            .getContentAsString();
    return response.isBlank() ? null : mapper.readTree(response);
  }
}
