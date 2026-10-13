package com.odontocare;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.security.model.Permission;
import java.math.BigDecimal;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;

@SpringBootTest(
    properties = {
      "odontocare.ai.worker-enabled=false",
      "odontocare.kapso.worker-enabled=false",
      "odontocare.whatsapp.worker-enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TreatmentPresentationIntegrationTests.TimeConfig.class)
class TreatmentPresentationIntegrationTests {
  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    Clock presentationClock() {
      return Clock.fixed(Instant.parse("2030-01-01T16:00:00Z"), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  MockHttpSession admin;
  JsonNode doctor, patient, service;
  String username, password;

  @BeforeEach
  void prepare() throws Exception {
    assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
        .isEqualTo("sistema_odonto_mini_test");
    jdbc.execute(
        "TRUNCATE"
            + " treatment_proforma_content,treatment_proforma,treatment_scheduled_session,agent_price_sequence,agent_appointment_reference,agent_change_proposal,agent_request_context,agent_supervision,kapso_webhook_event,kapso_message,kapso_conversation,agent_proposal,agent_slot,agent_step,agent_run,agent_message_source,agent_conversation_source,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_state,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update(
        "UPDATE installation_profile SET"
            + " time_zone='America/Lima',currency='PEN',minimum_lead_minutes=0,appointment_gap_minutes=0,patient_next_number=1,version=0");
    jdbc.update(
        "UPDATE agent_policy SET enabled=true,schedule='[]',max_price_services=3,version=0");
    jdbc.update("DELETE FROM role_permission");
    for (var permission : Permission.values())
      jdbc.update("INSERT INTO role_permission VALUES('ADMIN',?)", permission.name());
    username = "presentacion" + UUID.randomUUID().toString().substring(0, 8);
    password = "Test-" + UUID.randomUUID();
    call(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", username, "displayName", "Doctora Presentación", "password", password),
        201);
    admin = new MockHttpSession();
    mvc.perform(
            post("/api/v1/auth/login")
                .session(admin)
                .with(csrf())
                .param("username", username)
                .param("password", password))
        .andExpect(status().isOk());
    var category = create("/api/v1/categories", Map.of("name", "General", "active", true));
    service =
        create(
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
                "active",
                true,
                "bookableByAgent",
                true,
                "description",
                ""));
    UUID user =
        jdbc.queryForObject("SELECT id FROM user_account WHERE username=?", UUID.class, username);
    doctor =
        create(
            "/api/v1/dentists",
            Map.of(
                "userId",
                user,
                "fullName",
                "Doctora Presentación",
                "licenseNumber",
                "PRES-001",
                "specialty",
                "General",
                "serviceIds",
                List.of(service.path("id").asString()),
                "active",
                true));
    for (int day = 1; day <= 7; day++)
      create(
          "/api/v1/schedules/periods",
          Map.of(
              "dentistId",
              doctor.path("id").asString(),
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
    patient =
        create(
            "/api/v1/patients/quick",
            Map.of("fullName", "Paciente Presentación", "phone", "+51911122233"));
  }

  JsonNode call(
      MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int expected)
      throws Exception {
    if (session != null) request.session(session);
    if (body != null)
      request.contentType("application/json").content(mapper.writeValueAsString(body));
    var result = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();
    return result.getContentAsString().isBlank()
        ? null
        : mapper.readTree(result.getContentAsString());
  }

  JsonNode create(String url, Object body) throws Exception {
    return call(post(url).with(csrf()), admin, body, 201);
  }

  JsonNode read(String url) throws Exception {
    return call(get(url), admin, null, 200);
  }

  UUID id(JsonNode value) {
    return UUID.fromString(value.path("id").asString());
  }

  Map<String, Object> item(String price, int sessions, Object scheduling) {
    var value = new LinkedHashMap<String, Object>();
    value.put("serviceId", service.path("id").asString());
    value.put("description", "Tratamiento de demostración");
    value.put("quantity", 1);
    value.put("sessions", sessions);
    value.put("unitPrice", price);
    value.put("durationMinutes", 60);
    value.put("scheduling", scheduling);
    return value;
  }

  Map<String, Object> body(List<?> items, Object agreement) {
    var value = new LinkedHashMap<String, Object>();
    value.put("patientId", patient.path("id").asString());
    value.put("dentistId", doctor.path("id").asString());
    value.put("title", "Plan de demostración");
    value.put(
        "conditions", "Incluye las sesiones acordadas. Coordinación de horarios con el paciente.");
    value.put("version", 0);
    value.put("requestKey", UUID.randomUUID());
    value.put("items", items);
    value.put("paymentAgreement", agreement);
    return value;
  }

  Object dates() {
    return Map.of(
        "mode",
        "FREQUENCY",
        "firstDate",
        "2030-01-01",
        "interval",
        2,
        "unit",
        "WEEKS",
        "dates",
        List.of());
  }

  Object agreement() {
    return Map.of(
        "initialAmount",
        "200.00",
        "installments",
        List.of(
            Map.of("dueOn", "2030-01-20", "amount", "300.00"),
            Map.of("dueOn", "2030-02-20", "amount", "700.00")));
  }

  JsonNode action(JsonNode plan, String action, Object initial) throws Exception {
    var value = new LinkedHashMap<String, Object>();
    value.put("version", plan.path("version").asLong());
    value.put("requestKey", UUID.randomUUID());
    value.put("reason", "Acuerdo confirmado para demostración");
    value.put("acceptedBy", patient.path("fullName").asString());
    value.put("initialCollection", initial);
    return call(
        post("/api/v1/plans/" + id(plan) + "/actions/" + action).with(csrf()), admin, value, 200);
  }

  Object collection(String method) {
    return Map.of(
        "received", true, "occurredOn", "2030-01-01", "method", method, "reference", "DEMO-001");
  }

  JsonNode accepted(boolean payment) throws Exception {
    var plan =
        create(
            "/api/v1/plans", body(List.of(item("1200", 3, dates())), payment ? agreement() : null));
    return action(action(plan, "propose", null), "accept", payment ? collection("TRANSFER") : null);
  }

  JsonNode sessions(JsonNode plan) throws Exception {
    return read("/api/v1/treatment-sessions?planId=" + id(plan) + "&sort=plannedOn");
  }

  long count(String table) {
    return jdbc.queryForObject("SELECT count(*) FROM " + table, Long.class);
  }

  @Test
  void draftAndProformaDoNotCreateDebtAppointmentsOrMoney() throws Exception {
    var plan = create("/api/v1/plans", body(List.of(item("1200", 3, dates())), agreement()));
    var document =
        call(post("/api/v1/plans/" + id(plan) + "/proformas").with(csrf()), admin, Map.of(), 200);
    assertThat(count("charge_entry")).isZero();
    assertThat(count("money_movement")).isZero();
    assertThat(count("appointment")).isZero();
    assertThat(count("treatment_scheduled_session")).isZero();
    assertThat(document.path("planStatus").asString()).isEqualTo("DRAFT");
    assertThat(read("/api/v1/plans/" + id(plan) + "/proformas").toString())
        .doesNotContain("content", "JVBER");
    var bytes =
        mvc.perform(
                get("/api/v1/plans/" + id(plan) + "/proformas/" + id(document) + "/content")
                    .session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    var folder = Path.of(".runtime", "treatment-pdf-review");
    Files.createDirectories(folder);
    Files.write(folder.resolve("proforma-borrador.pdf"), bytes);
    try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
      assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf))
          .contains(
              "1,200.00",
              "200.00",
              "300.00",
              "700.00",
              "15/01/2030",
              "29/01/2030",
              "Hora por coordinar");
    }
  }

  @Test
  void acceptanceCreatesInitialPaymentAgreedDuesAndPendingSessionsExactlyOnce() throws Exception {
    var plan = accepted(true);
    assertThat(count("charge_entry")).isEqualTo(1);
    assertThat(count("money_movement")).isEqualTo(1);
    assertThat(count("installment")).isEqualTo(3);
    assertThat(jdbc.queryForObject("SELECT sum(amount) FROM money_application", BigDecimal.class))
        .isEqualByComparingTo("200");
    assertThat(count("appointment")).isZero();
    assertThat(sessions(plan).path("items").size()).isEqualTo(3);
    assertThat(sessions(plan).path("items").path(1).path("plannedOn").asString())
        .isEqualTo("2030-01-15");
    action(plan, "accept", collection("TRANSFER"));
    assertThat(count("money_movement")).isEqualTo(1);
    assertThat(count("installment")).isEqualTo(3);
    assertThat(count("treatment_scheduled_session")).isEqualTo(3);
    var dues = read("/api/v1/finance/installments?patientId=" + id(patient));
    assertThat(dues.path("items").path(0).path("amount").decimalValue())
        .isEqualByComparingTo("200");
    assertThat(dues.path("items").path(0).path("status").asString()).isEqualTo("PAID");
    assertThat(dues.path("items").path(1).path("pending").decimalValue())
        .isEqualByComparingTo("300");
    assertThat(dues.path("items").path(2).path("pending").decimalValue())
        .isEqualByComparingTo("700");
    var document =
        call(post("/api/v1/plans/" + id(plan) + "/proformas").with(csrf()), admin, Map.of(), 200);
    var bytes =
        mvc.perform(
                get("/api/v1/plans/" + id(plan) + "/proformas/" + id(document) + "/content")
                    .session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    Files.createDirectories(Path.of(".runtime", "treatment-pdf-review"));
    Files.write(Path.of(".runtime", "treatment-pdf-review", "acuerdo-aceptado.pdf"), bytes);
  }

  @Test
  void closedCashRollsBackTheEntireAcceptanceAndThenCanBeRetried() throws Exception {
    var plan =
        action(
            create("/api/v1/plans", body(List.of(item("1200", 3, dates())), agreement())),
            "propose",
            null);
    var request =
        Map.of(
            "version",
            plan.path("version").asLong(),
            "requestKey",
            UUID.randomUUID(),
            "reason",
            "Aceptación y cobro",
            "acceptedBy",
            "Paciente Presentación",
            "initialCollection",
            collection("CASH"));
    call(post("/api/v1/plans/" + id(plan) + "/actions/accept").with(csrf()), admin, request, 409);
    assertThat(count("charge_entry")).isZero();
    assertThat(count("money_movement")).isZero();
    assertThat(count("treatment_scheduled_session")).isZero();
    assertThat(read("/api/v1/plans/" + id(plan)).path("status").asString()).isEqualTo("PROPOSED");
    create(
        "/api/v1/finance/cash",
        Map.of(
            "requestKey",
            UUID.randomUUID(),
            "currency",
            "PEN",
            "opening",
            0,
            "reason",
            "Caja de prueba"));
    call(post("/api/v1/plans/" + id(plan) + "/actions/accept").with(csrf()), admin, request, 200);
    assertThat(read("/api/v1/finance/cash/current").path("expected").decimalValue())
        .isEqualByComparingTo("200");
  }

  @Test
  void invalidPaymentAgreementIsRejectedWithoutSavingAPartialBudget() throws Exception {
    call(
        post("/api/v1/plans").with(csrf()),
        admin,
        body(
            List.of(item("1200", 3, dates())),
            Map.of(
                "initialAmount",
                200,
                "installments",
                List.of(Map.of("dueOn", "2030-02-01", "amount", 100)))),
        400);
    assertThat(count("treatment_plan")).isZero();
    assertThat(count("treatment_item")).isZero();
  }

  @Test
  void pendingSessionDoesNotOccupyAndAssigningHourPreservesDurationAndIdempotency()
      throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").path(0);
    var before = read("/api/v1/appointments/calendar?from=2030-01-01&to=2030-01-02");
    assertThat(before.path("items").size()).isZero();
    assertThat(before.path("pendingSessions").size()).isEqualTo(1);
    var request =
        Map.of(
            "localStart",
            "2030-01-01T11:00",
            "version",
            session.path("version").asLong(),
            "requestKey",
            UUID.randomUUID());
    var appointment =
        call(
            post("/api/v1/treatment-sessions/" + id(session) + "/assign").with(csrf()),
            admin,
            request,
            200);
    assertThat(appointment.path("durationMinutes").asInt()).isEqualTo(60);
    assertThat(appointment.path("localEnd").asString()).isEqualTo("2030-01-01T12:00:00");
    assertThat(appointment.path("planItemId").asString())
        .isEqualTo(session.path("itemId").asString());
    call(
        post("/api/v1/treatment-sessions/" + id(session) + "/assign").with(csrf()),
        admin,
        request,
        200);
    assertThat(count("appointment")).isEqualTo(1);
    assertThat(sessions(plan).path("items").path(0).path("status").asString())
        .isEqualTo("SCHEDULED");
  }

  @Test
  void quickAttentionFromPlanAppointmentHasCorrectProcedureAndNoSecondDebt() throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").path(0);
    var appointment =
        call(
            post("/api/v1/treatment-sessions/" + id(session) + "/assign").with(csrf()),
            admin,
            Map.of("localStart", "2030-01-01T11:00", "version", 0, "requestKey", UUID.randomUUID()),
            200);
    var start = read("/api/v1/clinical/from-appointment/" + id(appointment));
    assertThat(
            start
                .path("defaults")
                .path("content")
                .path("procedures")
                .path(0)
                .path("planItemId")
                .asString())
        .isEqualTo(session.path("itemId").asString());
    var encounter = create("/api/v1/clinical/encounters", start.path("defaults"));
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.path("version").asLong()),
        200);
    assertThat(count("charge_entry")).isEqualTo(1);
    assertThat(read("/api/v1/plans/" + id(plan)).path("completedSessions").asInt()).isEqualTo(1);
    assertThat(sessions(plan).path("items").path(0).path("status").asString())
        .isEqualTo("COMPLETED");
  }

  @Test
  void removingTheTreatmentLinkCannotFinalizeTheSessionOrCreateASecondDebt() throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").get(0);
    var appointment =
        call(
            post("/api/v1/treatment-sessions/" + id(session) + "/assign").with(csrf()),
            admin,
            Map.of("localStart", "2030-01-01T11:00", "version", 0, "requestKey", UUID.randomUUID()),
            200);
    var defaults =
        (tools.jackson.databind.node.ObjectNode)
            read("/api/v1/clinical/from-appointment/" + id(appointment)).path("defaults");
    ((tools.jackson.databind.node.ObjectNode) defaults.path("content").path("procedures").get(0))
        .remove("planItemId");
    var encounter = create("/api/v1/clinical/encounters", defaults);
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.path("version").asLong()),
        400);
    assertThat(read("/api/v1/appointments/" + id(appointment)).path("status").asString())
        .isEqualTo("RESERVED");
    assertThat(count("charge_entry")).isEqualTo(1);
    assertThat(count("treatment_session")).isZero();
  }

  @Test
  void customServiceAppointmentPrefillsItsDescriptionAndPriceAndBillsNormally() throws Exception {
    var request = new LinkedHashMap<String, Object>();
    request.put("patientId", id(patient));
    request.put("dentistId", id(doctor));
    request.put("reason", "Reparación de bracket");
    request.put("durationMinutes", 30);
    request.put("administrative", false);
    request.put("agreedPrice", "75.50");
    request.put("localStart", "2030-01-01T11:00");
    request.put("notes", "");
    request.put("requestKey", UUID.randomUUID());
    var appointment = create("/api/v1/appointments", request);
    var start = read("/api/v1/clinical/from-appointment/" + id(appointment));
    assertThat(
            start
                .path("defaults")
                .path("content")
                .path("procedures")
                .path(0)
                .path("description")
                .asString())
        .isEqualTo("Reparación de bracket");
    var encounter = create("/api/v1/clinical/encounters", start.path("defaults"));
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.path("version").asLong()),
        200);
    assertThat(jdbc.queryForObject("SELECT amount FROM charge_entry", BigDecimal.class))
        .isEqualByComparingTo("75.50");
    assertThat(read("/api/v1/appointments/" + id(appointment) + "/contacts").toString())
        .contains("+51911122233", "Paciente Presentación");
  }

  @Test
  void conflictingReservationKeepsTheSessionPendingAndPreservesExistingAppointment()
      throws Exception {
    var plan = accepted(false);
    var first = sessions(plan).path("items").path(0);
    create(
        "/api/v1/appointments",
        Map.of(
            "patientId",
            id(patient),
            "dentistId",
            id(doctor),
            "serviceId",
            id(service),
            "reason",
            "",
            "localStart",
            "2030-01-01T11:00",
            "notes",
            "",
            "requestKey",
            UUID.randomUUID()));
    call(
        post("/api/v1/treatment-sessions/" + id(first) + "/assign").with(csrf()),
        admin,
        Map.of("localStart", "2030-01-01T11:00", "version", 0, "requestKey", UUID.randomUUID()),
        409);
    assertThat(count("appointment")).isEqualTo(1);
    assertThat(read("/api/v1/treatment-sessions/" + id(first)).path("status").asString())
        .isEqualTo("PENDING");
  }

  @Test
  void cancellationReleasesASessionAndPlanCancellationKeepsItsHistory() throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").path(1);
    var appointment =
        call(
            post("/api/v1/treatment-sessions/" + id(session) + "/assign").with(csrf()),
            admin,
            Map.of("localStart", "2030-01-15T11:00", "version", 0, "requestKey", UUID.randomUUID()),
            200);
    action(read("/api/v1/plans/" + id(plan)), "cancel", null);
    assertThat(read("/api/v1/appointments/" + id(appointment)).path("status").asString())
        .isEqualTo("CANCELLED");
    assertThat(sessions(plan).path("items").get(0).path("status").asString())
        .isEqualTo("CANCELLED");
    assertThat(count("appointment_history")).isEqualTo(2);
    assertThat(count("charge_entry")).isEqualTo(1);
  }

  @Test
  void previousClinicalNotesAreReadOnlyAndRestrictedToClinicalPermission() throws Exception {
    var content =
        Map.of(
            "anamnesis",
            "",
            "evolution",
            "Primera sesión realizada",
            "diagnoses",
            "Diagnóstico de demostración",
            "indications",
            "En la siguiente visita revisar la pieza 16",
            "procedures",
            List.of(Map.of("description", "Evaluación", "quantity", 1, "unitPrice", "0.00")));
    var encounter =
        create(
            "/api/v1/clinical/encounters",
            Map.of(
                "patientId",
                id(patient),
                "dentistId",
                id(doctor),
                "attendedOn",
                "2030-01-01",
                "reason",
                "Evaluación",
                "content",
                content,
                "version",
                0));
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.path("version").asLong()),
        200);
    var url =
        "/api/v1/clinical/patients/" + id(patient) + "/previous-encounter?dentistId=" + id(doctor);
    assertThat(read(url).toString()).contains("En la siguiente visita revisar la pieza 16");
    jdbc.update("DELETE FROM role_permission WHERE permission='CLINICAL_READ'");
    call(get(url), admin, null, 403);
    assertThat(count("clinical_encounter")).isEqualTo(1);
  }

  @Test
  void pendingDatesArePagedSearchableAndTheirChangesAreRecorded() throws Exception {
    var plan = accepted(false);
    var rows =
        read(
            "/api/v1/treatment-sessions?planId="
                + id(plan)
                + "&size=1&search=demostración&sort=plannedOn");
    assertThat(rows.path("items").size()).isEqualTo(1);
    assertThat(rows.path("totalElements").asInt()).isEqualTo(3);
    var session = rows.path("items").path(0);
    call(
        put("/api/v1/treatment-sessions/" + id(session) + "/date").with(csrf()),
        admin,
        Map.of("plannedOn", "2030-01-02", "version", 0, "reason", "Coordinación con paciente"),
        200);
    assertThat(read("/api/v1/plans/" + id(plan) + "/history").toString())
        .contains("SESSION_DATE", "2030-01-02");
    assertThat(count("appointment")).isZero();
  }

  @Test
  void twoConcurrentAssignmentsHaveOneWinnerAndOneAppointment() throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").path(0);
    var pool = Executors.newFixedThreadPool(2);
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try {
      var futures = new ArrayList<Future<Integer>>();
      for (int index = 0; index < 2; index++)
        futures.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return mvc.perform(
                          post("/api/v1/treatment-sessions/" + id(session) + "/assign")
                              .session(admin)
                              .with(csrf())
                              .contentType("application/json")
                              .content(
                                  mapper.writeValueAsString(
                                      Map.of(
                                          "localStart",
                                          "2030-01-01T11:00",
                                          "version",
                                          0,
                                          "requestKey",
                                          UUID.randomUUID()))))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      start.countDown();
      assertThat(
              List.of(
                  futures.get(0).get(10, TimeUnit.SECONDS),
                  futures.get(1).get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(200, 409);
      assertThat(count("appointment")).isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void futureOrUnconfirmedInitialPaymentCannotPartiallyAcceptAPlan() throws Exception {
    var plan =
        action(
            create("/api/v1/plans", body(List.of(item("1200", 3, dates())), agreement())),
            "propose",
            null);
    for (var collection :
        List.of(
            Map.of(
                "received",
                false,
                "occurredOn",
                "2030-01-01",
                "method",
                "TRANSFER",
                "reference",
                ""),
            Map.of(
                "received",
                true,
                "occurredOn",
                "2030-01-02",
                "method",
                "TRANSFER",
                "reference",
                ""))) {
      call(
          post("/api/v1/plans/" + id(plan) + "/actions/accept").with(csrf()),
          admin,
          Map.of(
              "version",
              plan.path("version").asLong(),
              "requestKey",
              UUID.randomUUID(),
              "reason",
              "Prueba",
              "acceptedBy",
              "Paciente Presentación",
              "initialCollection",
              collection),
          400);
    }
    assertThat(count("charge_entry")).isZero();
    assertThat(count("money_movement")).isZero();
    assertThat(count("installment")).isZero();
  }

  @Test
  void multipleChargesDistributeTheSingleInitialAndInstallmentsWithoutCreatingNewDebt()
      throws Exception {
    var plan =
        create(
            "/api/v1/plans",
            body(List.of(item("400", 1, null), item("800", 1, null)), agreement()));
    plan = action(action(plan, "propose", null), "accept", collection("TRANSFER"));
    assertThat(count("charge_entry")).isEqualTo(2);
    assertThat(count("money_movement")).isEqualTo(1);
    assertThat(jdbc.queryForObject("SELECT sum(amount) FROM installment", BigDecimal.class))
        .isEqualByComparingTo("1200");
    assertThat(jdbc.queryForObject("SELECT sum(amount) FROM money_application", BigDecimal.class))
        .isEqualByComparingTo("200");
    assertThat(jdbc.queryForObject("SELECT sum(amount) FROM charge_entry", BigDecimal.class))
        .isEqualByComparingTo("1200");
  }

  @Test
  void emittedProformaKeepsOriginalBytesAfterIdentityAndCatalogueChanges() throws Exception {
    var plan = accepted(false);
    var doc = read("/api/v1/plans/" + id(plan) + "/proformas").path("items").get(0);
    var url = "/api/v1/plans/" + id(plan) + "/proformas/" + id(doc) + "/content";
    var original =
        mvc.perform(get(url).session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    jdbc.update("UPDATE installation_profile SET display_name='Otra identidad'");
    jdbc.update("UPDATE dental_service SET price=999");
    assertThat(
            mvc.perform(get(url).session(admin))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
        .isEqualTo(original);
  }

  @Test
  void closedDayAndForeignPatientCannotReserveATreatmentSession() throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").get(0);
    jdbc.update(
        "INSERT INTO schedule_exception(id,kind,start_date,end_date,reason)"
            + " VALUES(?,'HOLIDAY','2030-01-01','2030-01-01','Cerrado')",
        UUID.randomUUID());
    call(
        post("/api/v1/treatment-sessions/" + id(session) + "/assign").with(csrf()),
        admin,
        Map.of("localStart", "2030-01-01T11:00", "version", 0, "requestKey", UUID.randomUUID()),
        409);
    var other =
        create(
            "/api/v1/patients/quick", Map.of("fullName", "Otro Paciente", "phone", "+51911122234"));
    var request = new LinkedHashMap<String, Object>();
    request.put("patientId", id(other));
    request.put("dentistId", id(doctor));
    request.put("serviceId", id(service));
    request.put("scheduledSessionId", id(session));
    request.put("localStart", "2030-01-02T11:00");
    request.put("requestKey", UUID.randomUUID());
    request.put("reason", "");
    request.put("notes", "");
    call(post("/api/v1/appointments").with(csrf()), admin, request, 400);
    assertThat(count("appointment")).isZero();
  }

  @Test
  void coordinationSummarySeparatesOverdueUpcomingAndUndatedSessions() throws Exception {
    var plan = accepted(false);
    var session = sessions(plan).path("items").get(0);
    call(
        put("/api/v1/treatment-sessions/" + id(session) + "/date").with(csrf()),
        admin,
        Map.of(
            "plannedOn", "2029-12-31", "version", 0, "reason", "Paciente pendiente de contactar"),
        200);
    var summary = read("/api/v1/treatment-sessions/coordination-summary?dentistId=" + id(doctor));
    assertThat(summary.path("overdue").asInt()).isEqualTo(1);
    assertThat(summary.path("upcoming").asInt()).isZero();
    var undated = create("/api/v1/plans", body(List.of(item("100", 1, null)), null));
    action(action(undated, "propose", null), "accept", null);
    var rows = read("/api/v1/treatment-sessions?status=PENDING&undated=true");
    assertThat(rows.path("totalElements").asInt()).isEqualTo(1);
    assertThat(rows.path("items").get(0).path("plannedOn").isNull()).isTrue();
  }
}
