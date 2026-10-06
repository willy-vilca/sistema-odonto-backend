package com.odontocare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.security.model.Permission;
import com.odontocare.whatsapp.config.WhatsAppProperties;
import com.odontocare.whatsapp.config.WhatsAppWorker;
import com.odontocare.whatsapp.service.TwilioSender;
import com.odontocare.whatsapp.service.WhatsAppOutboxService;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.sql.Timestamp;
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
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest(
    properties = {
      "odontocare.whatsapp.enabled=true",
      "odontocare.whatsapp.account-sid=AC11111111111111111111111111111111",
      "odontocare.whatsapp.auth-token=whatsapp-test-token",
      "odontocare.whatsapp.sender=whatsapp:+14155238886",
      "odontocare.whatsapp.public-base-url=https://wa.example.test",
      "odontocare.whatsapp.allowed-participants[0]=+51999998888",
      "odontocare.whatsapp.send-mode=TEMPLATE",
      "odontocare.whatsapp.test-template-sid=HX22222222222222222222222222222222",
      "odontocare.whatsapp.worker-enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
class WhatsAppIntegrationTests {
  private static final String INBOUND = "/api/v1/integrations/whatsapp/inbound";
  private static final String STATUS = "/api/v1/integrations/whatsapp/status";
  private static final String API = "/api/v1/whatsapp";
  private static final String PHONE = "+51999998888";
  private static final String BASE = "https://wa.example.test";
  private static final String TOKEN = "whatsapp-test-token";
  private static final String ACCOUNT = "AC11111111111111111111111111111111";
  private static final String SENDER = "whatsapp:+14155238886";

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired WhatsAppProperties config;
  @Autowired WhatsAppOutboxService outbox;
  @Autowired WhatsAppWorker worker;
  @MockitoBean TwilioSender sender;
  private MockHttpSession admin;
  private String password;

  @BeforeEach
  void prepareIsolatedDatabase() throws Exception {
    assertThat(jdbc.queryForObject("select current_database()", String.class))
        .isEqualTo("sistema_odontologo_test");
    jdbc.execute(
        "TRUNCATE agent_proposal,agent_slot,agent_step,agent_run,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,"
            + "financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update("DELETE FROM role_permission");
    for (var permission : Permission.values())
      jdbc.update("INSERT INTO role_permission VALUES ('ADMIN',?)", permission.name());
    config.setEnabled(true);
    config.setWorkerEnabled(false);
    config.setAccountSid(ACCOUNT);
    config.setAuthToken(TOKEN);
    config.setSender(SENDER);
    config.setPublicBaseUrl(BASE);
    config.setAllowedParticipants(List.of(PHONE));
    config.setSendMode("TEMPLATE");
    config.setTestTemplateSid("HX22222222222222222222222222222222");
    password = "Test-" + UUID.randomUUID();
    String username = "admin" + UUID.randomUUID().toString().substring(0, 8);
    call(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", username, "displayName", "Admin WhatsApp test", "password", password),
        201);
    admin = login(username);
  }

  @Test
  void onlySignedExactWebhookRequestsArePublicAndApplicationWritesKeepCsrf() throws Exception {
    var form = inboundForm("Quiero una cita");
    mvc.perform(formRequest(INBOUND, form)).andExpect(status().isForbidden());
    mvc.perform(formRequest(INBOUND, form).header("X-Twilio-Signature", "incorrecta"))
        .andExpect(status().isForbidden());
    String originalSignature = signature(BASE + INBOUND, form);
    form.put("Body", "Texto alterado después de firmar");
    mvc.perform(formRequest(INBOUND, form).header("X-Twilio-Signature", originalSignature))
        .andExpect(status().isForbidden());
    mvc.perform(
            formRequest(INBOUND, form)
                .header(
                    "X-Twilio-Signature", signature("https://other.example.test" + INBOUND, form)))
        .andExpect(status().isForbidden());
    mvc.perform(
            formRequest(INBOUND, form)
                .header("X-Twilio-Signature", signature(BASE + INBOUND, form))
                .header("X-Forwarded-Host", "attacker.example.test")
                .header("X-Forwarded-Proto", "http"))
        .andExpect(status().isOk())
        .andExpect(content().string(""));
    mvc.perform(get(INBOUND)).andExpect(status().isUnauthorized());
    mvc.perform(put(INBOUND).with(csrf())).andExpect(status().isUnauthorized());
    mvc.perform(get(API + "/conversations?sort=lastMessageAt"))
        .andExpect(status().isUnauthorized());
    mvc.perform(
            post(API + "/conversations/" + conversationId() + "/test-reply")
                .session(admin)
                .contentType(MediaType.APPLICATION_JSON)
                .content(mapper.writeValueAsString(Map.of("requestKey", UUID.randomUUID()))))
        .andExpect(status().isForbidden());
    assertThat(count("whatsapp_message")).isEqualTo(1);
  }

  @Test
  void signedAccountSenderAndParticipantAreCheckedBeforePersisting() throws Exception {
    for (var override :
        List.of(
            Map.of("AccountSid", "AC" + "9".repeat(32)),
            Map.of("From", "whatsapp:+51999998889"),
            Map.of("To", "whatsapp:+14155238887"),
            Map.of("From", PHONE))) {
      var form = inboundForm("Hola");
      form.putAll(override);
      signed(INBOUND, form, 403);
    }
    assertThat(count("whatsapp_conversation")).isZero();
    assertThat(count("whatsapp_message")).isZero();
  }

  @Test
  void concurrentRepeatedInboundIsStoredOnceWithoutCreatingPatientsOrAppointments()
      throws Exception {
    var form = inboundForm("Necesito una cita para mi hija");
    signed(INBOUND, form, 200);
    UUID conversation = conversationId();
    Timestamp firstInbound =
        jdbc.queryForObject(
            "SELECT last_inbound_at FROM whatsapp_conversation WHERE id=?",
            Timestamp.class,
            conversation);
    try (var executor = Executors.newFixedThreadPool(6)) {
      var jobs = new ArrayList<Callable<Integer>>();
      for (int i = 0; i < 6; i++)
        jobs.add(() -> signedRequest(INBOUND, form).getResponse().getStatus());
      for (var result : executor.invokeAll(jobs))
        assertThat(result.get(10, TimeUnit.SECONDS)).isEqualTo(200);
    }
    assertThat(count("whatsapp_conversation")).isEqualTo(1);
    assertThat(count("whatsapp_message")).isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT last_inbound_at FROM whatsapp_conversation WHERE id=?",
                Timestamp.class,
                conversation))
        .isEqualTo(firstInbound);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM audit_event WHERE action='WHATSAPP_MESSAGE_RECEIVED'",
                Long.class))
        .isEqualTo(1);
    assertThat(count("patient")).isZero();
    assertThat(count("appointment")).isZero();
    assertThat(count("whatsapp_delivery_event")).isZero();
    verifyNoInteractions(sender);
  }

  @Test
  void mediaIsRecordedAsUnsupportedWithoutFetchingItAndOversizedBodiesAreRejected()
      throws Exception {
    var form = inboundForm("Foto adjunta");
    form.put("NumMedia", "1");
    form.put("MediaUrl0", "http://127.0.0.1:1/must-never-be-downloaded");
    form.put("MediaContentType0", "image/png");
    signed(INBOUND, form, 200);
    JsonNode messages = messages(conversationId(), "");
    assertThat(messages.path("items").get(0).path("status").asString()).isEqualTo("UNSUPPORTED");
    assertThat(messages.path("items").get(0).has("MediaUrl0")).isFalse();
    signed(INBOUND, inboundForm("x".repeat(4097)), 400);
    assertThat(count("whatsapp_message")).isEqualTo(1);
    verifyNoInteractions(sender);
  }

  @Test
  void conversationsAndMessagesArePagedFilteredAndPermissionProtectedWithoutSecrets()
      throws Exception {
    config.setAllowedParticipants(List.of(PHONE, "+51999998889"));
    signed(INBOUND, inboundForm("Precio 10%_especial"), 200);
    UUID conversation = conversationId();
    for (String text : List.of("Horario por la mañana", "Segunda pregunta", "Tercera pregunta"))
      signed(INBOUND, inboundForm(text), 200);
    var other = inboundForm("Otra conversación");
    other.put("From", "whatsapp:+51999998889");
    other.put("ProfileName", "Segundo contacto");
    signed(INBOUND, other, 200);
    JsonNode paged =
        call(
            get(API + "/conversations").param("sort", "lastMessageAt").param("size", "1"),
            admin,
            null,
            200);
    assertThat(paged.path("items").size()).isEqualTo(1);
    assertThat(paged.path("totalElements").asLong()).isEqualTo(2);
    assertThat(paged.path("totalPages").asInt()).isEqualTo(2);
    JsonNode search =
        call(
            get(API + "/conversations").param("sort", "phone").param("search", "Segundo contacto"),
            admin,
            null,
            200);
    assertThat(search.path("totalElements").asLong()).isEqualTo(1);
    JsonNode secondPage =
        messages(conversation, "&size=2&page=1&messageDirection=INBOUND&status=RECEIVED");
    assertThat(secondPage.path("items").size()).isEqualTo(2);
    assertThat(secondPage.path("totalElements").asLong()).isEqualTo(4);
    JsonNode literal =
        call(
            get(API + "/conversations/" + conversation + "/messages")
                .param("sort", "createdAt")
                .param("search", "%_"),
            admin,
            null,
            200);
    assertThat(literal.path("totalElements").asLong()).isEqualTo(1);
    call(get(API + "/conversations?sort=lastMessageAt&size=101"), admin, null, 400);
    call(get(API + "/conversations?sort=password"), admin, null, 400);
    call(
        get(
            API
                + "/conversations/"
                + conversation
                + "/messages?sort=createdAt&messageDirection=INVALID"),
        admin,
        null,
        400);
    call(
        get(API + "/conversations/" + conversation + "/messages?sort=createdAt&status=INVALID"),
        admin,
        null,
        400);
    JsonNode connection = call(get(API + "/connection"), admin, null, 200);
    assertThat(connection.path("configured").asBoolean()).isTrue();
    assertThat(connection.path("agentEnabled").asBoolean()).isFalse();
    assertThat(connection.toString())
        .doesNotContain(TOKEN)
        .doesNotContain("authToken")
        .doesNotContain(ACCOUNT);
    MockHttpSession reader = readOnlyReception();
    call(get(API + "/conversations?sort=lastMessageAt"), reader, null, 200);
    call(
        post(API + "/conversations/" + conversation + "/test-reply").with(csrf()),
        reader,
        Map.of("requestKey", UUID.randomUUID()),
        403);
    jdbc.update("DELETE FROM role_permission WHERE role_code='RECEPTION'");
    call(get(API + "/connection"), reader, null, 403);
  }

  @Test
  void textEnqueueIsIdempotentAlsoUnderConcurrencyAndRejectsChangedPayloadOrExpiredWindow()
      throws Exception {
    config.setSendMode("TEXT");
    signed(INBOUND, inboundForm("Hola, quisiera reservar"), 200);
    UUID conversation = conversationId(), key = UUID.randomUUID();
    var payload = Map.of("body", "Hola, recibimos tu mensaje.", "requestKey", key);
    JsonNode first =
        call(
            post(API + "/conversations/" + conversation + "/messages").with(csrf()),
            admin,
            payload,
            200);
    try (var executor = Executors.newFixedThreadPool(4)) {
      var jobs = new ArrayList<Callable<JsonNode>>();
      for (int i = 0; i < 4; i++)
        jobs.add(
            () ->
                call(
                    post(API + "/conversations/" + conversation + "/messages").with(csrf()),
                    admin,
                    payload,
                    200));
      for (var result : executor.invokeAll(jobs))
        assertThat(result.get(10, TimeUnit.SECONDS).path("id").asString())
            .isEqualTo(first.path("id").asString());
    }
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM whatsapp_message WHERE direction='OUTBOUND'", Long.class))
        .isEqualTo(1);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "Otro texto", "requestKey", key),
        409);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "   ", "requestKey", UUID.randomUUID()),
        400);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "x".repeat(1601), "requestKey", UUID.randomUUID()),
        400);
    jdbc.update(
        "UPDATE whatsapp_conversation SET last_inbound_at=? WHERE id=?",
        Timestamp.from(Instant.now().minusSeconds(86401)),
        conversation);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "Ya pasó la ventana", "requestKey", UUID.randomUUID()),
        400);
    assertThat(count("whatsapp_message")).isEqualTo(2);
    verifyNoInteractions(sender);
  }

  @Test
  void queuedReplyIsRevalidatedAgainstParticipantModeAndTimeBeforeProviderSend() throws Exception {
    signed(INBOUND, inboundForm("Prueba de conexión"), 200);
    UUID conversation = conversationId();
    UUID message = templateReply(conversation);
    call(
        post(API + "/conversations/" + conversation + "/messages").with(csrf()),
        admin,
        Map.of("body", "El texto no está habilitado", "requestKey", UUID.randomUUID()),
        400);
    var claimed = outbox.claim().orElseThrow();
    assertThat(claimed.id()).isEqualTo(message);
    assertThat(claimed.attempts()).isEqualTo(1);
    config.setAllowedParticipants(List.of());
    assertThat(outbox.destination(claimed)).isNull();
    assertThat(message(message).path("status").asString()).isEqualTo("FAILED");
    assertThat(message(message).path("errorCode").asString()).isEqualTo("LOCAL_POLICY");
    verifyNoInteractions(sender);
  }

  @Test
  void earlyStatusCallbackCorrelatesByUuidAndReadDoesNotRegressOrRepeatEvents() throws Exception {
    signed(INBOUND, inboundForm("Confirma que recibes mis mensajes"), 200);
    UUID id = templateReply(conversationId());
    outbox.claim().orElseThrow();
    String sid = sid();
    callback(id, sid, "read", null, 200);
    outbox.finish(id, new TwilioSender.Outcome(sid, "ACCEPTED", null, null));
    callback(id, sid, "sent", null, 200);
    callback(id, sid, "sent", null, 200);
    assertThat(message(id).path("status").asString()).isEqualTo("READ");
    assertThat(message(id).path("providerSid").asString()).isEqualTo(sid);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM whatsapp_delivery_event WHERE message_id=?", Long.class, id))
        .isEqualTo(2);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM whatsapp_message WHERE direction='INBOUND'", Long.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM whatsapp_message WHERE direction='OUTBOUND'", Long.class))
        .isEqualTo(1);
    assertThat(outbox.claim()).isEmpty();
  }

  @Test
  void failedCallbackBeforeApiResponseKeepsItsOriginalFailureDetails() throws Exception {
    signed(INBOUND, inboundForm("Prueba de envío"), 200);
    UUID id = templateReply(conversationId());
    outbox.claim().orElseThrow();
    String sid = sid();
    callback(id, sid, "undelivered", "63015", 200);
    outbox.finish(id, new TwilioSender.Outcome(sid, "ACCEPTED", null, null));
    assertThat(message(id).path("status").asString()).isEqualTo("FAILED");
    assertThat(message(id).path("errorCode").asString()).isEqualTo("63015");
    assertThat(message(id).path("errorMessage").asString()).isNotBlank();
  }

  @Test
  void sentCallbackBeforeTimeoutKeepsConfirmedStatusWithoutAmbiguousError() throws Exception {
    signed(INBOUND, inboundForm("Prueba de envío"), 200);
    UUID id = templateReply(conversationId());
    outbox.claim().orElseThrow();
    callback(id, sid(), "sent", null, 200);
    outbox.finish(id, new TwilioSender.Outcome(null, "UNKNOWN", null, "Resultado incierto"));
    assertThat(message(id).path("status").asString()).isEqualTo("SENT");
    assertThat(message(id).path("errorMessage").isNull()).isTrue();
  }

  @Test
  void ambiguousResultIsNotRetriedAndAuthenticatedCallbackCanReconcileIt() throws Exception {
    signed(INBOUND, inboundForm("Prueba de conexión"), 200);
    UUID id = templateReply(conversationId());
    outbox.claim().orElseThrow();
    outbox.finish(id, new TwilioSender.Outcome(null, "UNKNOWN", null, "Resultado incierto"));
    assertThat(message(id).path("status").asString()).isEqualTo("UNKNOWN");
    assertThat(outbox.claim()).isEmpty();
    callback(id, sid(), "delivered", null, 200);
    assertThat(message(id).path("status").asString()).isEqualTo("DELIVERED");
    assertThat(message(id).path("errorMessage").isNull()).isTrue();
    assertThat(message(id).path("attempts").asInt()).isEqualTo(1);
  }

  @Test
  void providerRateLimitRetriesAreBoundedAndCrashRecoveryDoesNotResend() throws Exception {
    signed(INBOUND, inboundForm("Prueba de conexión"), 200);
    UUID id = templateReply(conversationId());
    for (int attempt = 1; attempt <= 3; attempt++) {
      var claimed = outbox.claim().orElseThrow();
      assertThat(claimed.attempts()).isEqualTo(attempt);
      outbox.finish(id, new TwilioSender.Outcome(null, "RATE_LIMIT", "20429", "Espera"));
      assertThat(message(id).path("status").asString())
          .isEqualTo(attempt < 3 ? "QUEUED" : "FAILED");
      jdbc.update(
          "UPDATE whatsapp_message SET next_attempt_at=? WHERE id=?",
          Timestamp.from(Instant.now().minusSeconds(1)),
          id);
    }
    assertThat(outbox.claim()).isEmpty();
    UUID staleId = templateReply(conversationId());
    outbox.claim().orElseThrow();
    jdbc.update(
        "UPDATE whatsapp_message SET updated_at=? WHERE id=?",
        Timestamp.from(Instant.now().minusSeconds(61)),
        staleId);
    assertThat(outbox.claim()).isEmpty();
    assertThat(message(staleId).path("status").asString()).isEqualTo("UNKNOWN");
    assertThat(message(staleId).path("attempts").asInt()).isEqualTo(1);
    worker.sendPending();
    verifyNoInteractions(sender);
  }

  @Test
  void statusSignaturesAndReferencesAreCheckedAndRevokedParticipantsStillReceiveDeliveryUpdates()
      throws Exception {
    signed(INBOUND, inboundForm("Prueba"), 200);
    UUID id = templateReply(conversationId());
    outbox.claim().orElseThrow();
    String sid = sid();
    var form = statusForm(sid, "sent", null);
    form.put("From", "whatsapp:+14155238887");
    signed(STATUS + "?messageId=" + id, form, 403);
    form = statusForm(sid, "sent", null);
    form.put("To", "whatsapp:+51999998889");
    signed(STATUS + "?messageId=" + id, form, 403);
    String url = STATUS + "?messageId=" + id;
    mvc.perform(formRequest(url, statusForm(sid, "sent", null))).andExpect(status().isForbidden());
    callback(id, sid, "sent", null, 200);
    callback(id, sid(), "delivered", null, 403);
    config.setAllowedParticipants(List.of());
    callback(id, sid, "delivered", null, 200);
    assertThat(message(id).path("status").asString()).isEqualTo("DELIVERED");
    assertThat(count("whatsapp_delivery_event")).isEqualTo(2);
    assertThat(count("whatsapp_message")).isEqualTo(2);
  }

  private MockHttpSession login(String username) throws Exception {
    var result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .session(new MockHttpSession())
                    .with(csrf())
                    .param("username", username)
                    .param("password", password))
            .andExpect(status().isOk())
            .andReturn();
    return (MockHttpSession) result.getRequest().getSession(false);
  }

  private MockHttpSession readOnlyReception() throws Exception {
    jdbc.update("INSERT INTO role_permission VALUES('RECEPTION','WHATSAPP_READ')");
    String username = "reader" + UUID.randomUUID().toString().substring(0, 8);
    call(
        post("/api/v1/users").with(csrf()),
        admin,
        Map.of(
            "username",
            username,
            "displayName",
            "Solo lectura WhatsApp",
            "password",
            password,
            "email",
            "",
            "active",
            true,
            "roles",
            List.of("RECEPTION")),
        201);
    return login(username);
  }

  private JsonNode call(
      MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int expected)
      throws Exception {
    if (session != null) request.session(session);
    if (body != null)
      request.contentType(MediaType.APPLICATION_JSON).content(mapper.writeValueAsString(body));
    var response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();
    return response.getContentAsString().isBlank()
        ? null
        : mapper.readTree(response.getContentAsString());
  }

  private long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  private UUID conversationId() {
    return jdbc.queryForObject(
        "SELECT id FROM whatsapp_conversation WHERE phone=?", UUID.class, PHONE);
  }

  private Map<String, String> inboundForm(String body) {
    var form = new LinkedHashMap<String, String>();
    form.put("AccountSid", ACCOUNT);
    form.put("MessageSid", sid());
    form.put("From", "whatsapp:" + PHONE);
    form.put("To", SENDER);
    form.put("Body", body);
    form.put("NumMedia", "0");
    form.put("ProfileName", "Participante de pruebas");
    return form;
  }

  private static String sid() {
    return "SM" + UUID.randomUUID().toString().replace("-", "");
  }

  private static String signature(String url, Map<String, String> form) {
    var signedContent = new StringBuilder(url);
    new TreeMap<>(form).forEach((key, value) -> signedContent.append(key).append(value));
    try {
      var mac = Mac.getInstance("HmacSHA1");
      mac.init(new SecretKeySpec(TOKEN.getBytes(StandardCharsets.UTF_8), "HmacSHA1"));
      return Base64.getEncoder()
          .encodeToString(mac.doFinal(signedContent.toString().getBytes(StandardCharsets.UTF_8)));
    } catch (GeneralSecurityException exception) {
      throw new IllegalStateException("No se pudo firmar la solicitud de prueba.", exception);
    }
  }

  private MockHttpServletRequestBuilder formRequest(String path, Map<String, String> form) {
    var request =
        post(path)
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .characterEncoding(StandardCharsets.UTF_8.name());
    form.forEach((key, value) -> request.formField(key, value));
    return request;
  }

  private MvcResult signedRequest(String path, Map<String, String> form) throws Exception {
    return mvc.perform(
            formRequest(path, form).header("X-Twilio-Signature", signature(BASE + path, form)))
        .andReturn();
  }

  private void signed(String path, Map<String, String> form, int expected) throws Exception {
    mvc.perform(formRequest(path, form).header("X-Twilio-Signature", signature(BASE + path, form)))
        .andExpect(status().is(expected));
  }

  private JsonNode messages(UUID conversation, String parameters) throws Exception {
    return call(
        get(API + "/conversations/" + conversation + "/messages?sort=createdAt" + parameters),
        admin,
        null,
        200);
  }

  private JsonNode message(UUID id) throws Exception {
    return messages(conversationId(), "&size=100")
        .path("items")
        .valueStream()
        .filter(item -> item.path("id").asString().equals(id.toString()))
        .findFirst()
        .orElseThrow();
  }

  private UUID templateReply(UUID conversation) throws Exception {
    JsonNode response =
        call(
            post(API + "/conversations/" + conversation + "/test-reply").with(csrf()),
            admin,
            Map.of("requestKey", UUID.randomUUID()),
            200);
    return UUID.fromString(response.path("id").asString());
  }

  private Map<String, String> statusForm(String sid, String status, String code) {
    var form = new LinkedHashMap<String, String>();
    form.put("AccountSid", ACCOUNT);
    form.put("MessageSid", sid);
    form.put("MessageStatus", status);
    form.put("From", SENDER);
    if (code != null) form.put("ErrorCode", code);
    return form;
  }

  private void callback(UUID id, String sid, String status, String code, int expected)
      throws Exception {
    signed(STATUS + "?messageId=" + id, statusForm(sid, status, code), expected);
  }
}
