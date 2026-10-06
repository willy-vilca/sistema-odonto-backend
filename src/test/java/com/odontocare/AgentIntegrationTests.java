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
      "odontocare.whatsapp.worker-enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(AgentIntegrationTests.TimeConfig.class)
class AgentIntegrationTests {
  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    Clock agentClock() {
      return Clock.fixed(Instant.parse("2030-01-01T02:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired JdbcTemplate jdbc;
  @Autowired ObjectMapper mapper;
  @Autowired AgentQueueService queue;
  @Autowired BookingAgent agent;
  @Autowired AgentToolService tools;
  @Autowired AgentProperties config;
  @MockitoBean LanguageModelClient model;
  @MockitoBean TwilioSender sender;
  MockHttpSession admin;
  UUID serviceId, doctorId, patientId;
  String password, phone = "+51987654321";

  @BeforeEach
  void prepare() throws Exception {
    assertThat(jdbc.queryForObject("select current_database()", String.class))
        .isEqualTo("sistema_odontologo_test");
    jdbc.execute(
        "TRUNCATE"
            + " agent_proposal,agent_slot,agent_step,agent_run,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
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
    reset(model, sender);
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
    return queue.test(new TestMessage(phone, "Contacto Agente", text, UUID.randomUUID()));
  }

  private AgentRun process() {
    var run = queue.claim().orElseThrow();
    agent.process(run);
    return queue.detail(run.id()).run();
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

  @Test
  void proposalUsesRealToolsAndLimaTomorrowThenConfirmationPersistsExactlyOnce() {
    var p = propose(patientId, "Paciente Agente");
    assertThat(p.summary()).contains("01/01/2030 09:00", "60 minutos", "Doctora Demo");
    assertThat(appointments()).isZero();
    submit("CONFIRMO " + p.confirmationCode());
    var done = process();
    assertThat(done.state()).isEqualTo("COMPLETED");
    assertThat(appointments()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT status||':'||origin FROM appointment", String.class))
        .isEqualTo("CONFIRMED:AI_TEST");
    assertThat(jdbc.queryForObject("SELECT actor_name FROM appointment_history", String.class))
        .isEqualTo("Agente IA");
    submit("CONFIRMO " + p.confirmationCode());
    process();
    assertThat(appointments()).isEqualTo(1);
    verifyNoInteractions(sender);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM charge_entry", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM money_movement", Long.class)).isZero();
  }

  @Test
  void incompletePriceOrNegationDoesNotCreatePatientOrAppointment() {
    when(model.reply(anyList(), anyList()))
        .thenReturn(
            new LanguageModelClient.Reply(
                "Necesito datos para preparar una propuesta. No se reserva todavía.",
                List.of(),
                10,
                10));
    for (String text :
        List.of("¿Cuánto cuesta la limpieza?", "No me reserves todavía", "Quiero una cita")) {
      submit(text);
      process();
    }
    assertThat(appointments()).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM agent_proposal", Long.class)).isZero();
    assertThat(jdbc.queryForObject("SELECT count(*) FROM patient", Long.class)).isEqualTo(1);
  }

  @Test
  void unknownToolsAndCrossContactPatientAreRejected() {
    var result = submit("Intento de herramienta");
    var run = queue.claim().orElseThrow();
    assertThatThrownBy(() -> tools.execute(run, "leer_finanzas", mapper.readTree("{}")))
        .isInstanceOf(com.odontocare.shared.web.ApiException.class);
    var slots =
        tools.execute(
            run,
            "consultar_horarios",
            mapper.valueToTree(Map.of("service_id", serviceId, "days_from_today", 1)));
    var slot = mapper.valueToTree(slots).path("items").path(0).path("slot_id").asString();
    jdbc.update("UPDATE patient_contact SET phone='+51999999999'");
    assertThatThrownBy(
            () ->
                tools.execute(
                    run,
                    "proponer_cita",
                    mapper.valueToTree(
                        Map.of(
                            "slot_id",
                            slot,
                            "patient_id",
                            patientId,
                            "patient_name",
                            "Paciente Agente"))))
        .isInstanceOf(com.odontocare.shared.web.ApiException.class);
    assertThat(queue.proposal(result.conversationId())).isNull();
    assertThat(appointments()).isZero();
  }

  @Test
  void durationAndEligibilityChangesAfterOfferAreRejected() {
    var p = propose(patientId, "Paciente Agente");
    jdbc.update("UPDATE dental_service SET duration_minutes=30 WHERE id=?", serviceId);
    submit("CONFIRMO " + p.confirmationCode());
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(appointments()).isZero();
    jdbc.update(
        "UPDATE dental_service SET duration_minutes=60,bookable_by_agent=false WHERE id=?",
        serviceId);
    submit("CONFIRMO " + p.confirmationCode());
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(appointments()).isZero();
  }

  @Test
  void newProvisionalPatientIsCreatedOnlyAtConfirmation() {
    var p = propose(null, "Nuevo Paciente");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM patient", Long.class)).isEqualTo(1);
    submit("CONFIRMO " + p.confirmationCode());
    assertThat(process().state()).isEqualTo("COMPLETED");
    assertThat(jdbc.queryForObject("SELECT count(*) FROM patient WHERE provisional", Long.class))
        .isEqualTo(1);
    assertThat(appointments()).isEqualTo(1);
  }

  @Test
  void occupiedSlotRollsBackNewPatientAndPreservesReceptionBooking() throws Exception {
    var proposal = propose(null, "Nuevo Paciente");
    postData(
        "/api/v1/appointments",
        Map.of(
            "patientId",
            patientId,
            "dentistId",
            doctorId,
            "serviceId",
            serviceId,
            "reason",
            "",
            "localStart",
            "2030-01-01T09:00",
            "notes",
            "",
            "requestKey",
            UUID.randomUUID()));
    submit("CONFIRMO " + proposal.confirmationCode());
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(appointments()).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT count(*) FROM patient", Long.class)).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT origin FROM appointment", String.class))
        .isEqualTo("MANUAL");
    assertThat(
            jdbc.queryForObject(
                "SELECT patient_next_number FROM installation_profile", Integer.class))
        .isEqualTo(2);
  }

  @Test
  void anotherConversationCannotConfirmAProposal() {
    var proposal = propose(patientId, "Paciente Agente");
    reset(model);
    queue.test(
        new TestMessage(
            "+51999990000",
            "Otro contacto",
            "CONFIRMO " + proposal.confirmationCode(),
            UUID.randomUUID()));
    assertThat(process().state()).isEqualTo("FAILED");
    assertThat(appointments()).isZero();
    verifyNoInteractions(model);
  }

  @Test
  void expiredProposalIsNotBooked() {
    var p = propose(patientId, "Paciente Agente");
    jdbc.update("UPDATE agent_proposal SET expires_at='2029-12-31T00:00:00Z' WHERE id=?", p.id());
    submit("CONFIRMO " + p.confirmationCode());
    assertThat(process().responseText()).contains("venció");
    assertThat(appointments()).isZero();
  }

  @Test
  void repeatedTestRequestAndProviderFailureAreTraceableAndRetryBounded() {
    UUID key = UUID.randomUUID();
    var r = new TestMessage(phone, "Contacto", "Quiero cita", key);
    var first = queue.test(r);
    assertThat(queue.test(r).runId()).isEqualTo(first.runId());
    assertThatThrownBy(() -> queue.test(new TestMessage(phone, "Contacto", "Otro texto", key)))
        .isInstanceOf(com.odontocare.shared.web.ApiException.class);
    when(model.reply(anyList(), anyList()))
        .thenThrow(new ModelFailure("RATE_LIMIT", "Cuota alcanzada"));
    assertThat(process().errorCode()).isEqualTo("RATE_LIMIT");
    queue.retry(first.runId());
    process();
    queue.retry(first.runId());
    process();
    assertThatThrownBy(() -> queue.retry(first.runId()))
        .isInstanceOf(com.odontocare.shared.web.ApiException.class);
    verifyNoInteractions(sender);
  }

  @Test
  void apiEnforcesPermissionsCsrfPagingAndDoesNotExposeKey() throws Exception {
    var configJson = call(get("/api/v1/whatsapp/agent/configuration"), admin, null, 200);
    assertThat(configJson.toString()).doesNotContain("gsk_test_key");
    call(
        post("/api/v1/whatsapp/agent/test-messages"),
        admin,
        Map.of(
            "phone",
            phone,
            "contactName",
            "Contacto",
            "body",
            "Prueba",
            "requestKey",
            UUID.randomUUID()),
        403);
    var first = submit("Página agente 0");
    for (int i = 1; i < 21; i++) submit("Página agente " + i);
    var list =
        call(
            get("/api/v1/whatsapp/conversations/" + first.conversationId() + "/agent/runs")
                .param("size", "10")
                .param("page", "2")
                .param("search", "Página agente")
                .param("state", "QUEUED"),
            admin,
            null,
            200);
    assertThat(list.path("items").size()).isEqualTo(1);
    assertThat(list.path("totalElements").asInt()).isEqualTo(21);
    call(
        get("/api/v1/whatsapp/conversations/" + first.conversationId() + "/agent/runs")
            .param("sort", "api_key"),
        admin,
        null,
        400);
    jdbc.update("DELETE FROM role_permission WHERE permission='AGENT_TEST_WRITE'");
    call(
        post("/api/v1/whatsapp/agent/test-messages").with(csrf()),
        admin,
        Map.of(
            "phone",
            phone,
            "contactName",
            "Contacto",
            "body",
            "Prueba",
            "requestKey",
            UUID.randomUUID()),
        403);
  }
}
