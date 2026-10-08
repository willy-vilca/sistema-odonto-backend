package com.odontocare;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.security.model.Permission;
import java.time.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(Phase4IntegrationTests.TimeConfig.class)
class Phase4IntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired TestClock clock;
  MockHttpSession admin;
  String username, password;
  JsonNode service, doctor, otherDoctor, patient;

  static class TestClock extends Clock {
    final AtomicReference<Instant> now =
        new AtomicReference<>(Instant.parse("2030-01-01T12:00:00Z"));

    public ZoneId getZone() {
      return ZoneOffset.UTC;
    }

    public Clock withZone(ZoneId z) {
      return Clock.fixed(instant(), z);
    }

    public Instant instant() {
      return now.get();
    }
  }

  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    TestClock testClock() {
      return new TestClock();
    }
  }

  @BeforeEach
  void prepare() throws Exception {
    assertThat(jdbc.queryForObject("select current_database()", String.class))
        .isEqualTo("sistema_odontologo_test");
    jdbc.execute(
        "TRUNCATE agent_appointment_reference,agent_change_proposal,agent_request_context,agent_supervision,"
            + " agent_proposal,agent_slot,agent_step,agent_run,agent_message_source,agent_conversation_source,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update("UPDATE agent_policy SET enabled=true,schedule='[]',version=0,change_lead_minutes=0,allow_reschedule=true,allow_cancel=true WHERE id=1");
    jdbc.update(
        "UPDATE installation_profile SET"
            + " time_zone='America/Lima',minimum_lead_minutes=0,appointment_gap_minutes=0,patient_prefix='PAC',patient_next_number=1,version=0");
    jdbc.update("DELETE FROM role_permission");
    for (var p : Permission.values())
      jdbc.update("INSERT INTO role_permission VALUES('ADMIN',?)", p.name());
    for (String r : List.of("DENTIST", "RECEPTION", "CASHIER"))
      for (String p :
          List.of(
              "SETTINGS_READ",
              "DENTISTS_READ",
              "SERVICES_READ",
              "SCHEDULES_READ",
              "PATIENTS_READ",
              "APPOINTMENTS_READ")) jdbc.update("INSERT INTO role_permission VALUES(?,?)", r, p);
    jdbc.update(
        "INSERT INTO role_permission"
            + " VALUES('RECEPTION','PATIENTS_WRITE'),('RECEPTION','APPOINTMENTS_WRITE')");
    jdbc.update("UPDATE document_policy SET max_file_mi_b=20,version=0");
    for (String permission :
        List.of(
            "CLINICAL_READ",
            "CLINICAL_WRITE",
            "DOCUMENTS_READ",
            "DOCUMENTS_WRITE",
            "CLINICAL_CONFIG_READ"))
      jdbc.update("INSERT INTO role_permission VALUES('DENTIST',?)", permission);
    jdbc.update("UPDATE installation_profile SET currency='PEN',budget_next_number=1");
    for (String p : List.of("PLANS_READ", "PLANS_WRITE", "FINANCES_READ"))
      jdbc.update("INSERT INTO role_permission VALUES('DENTIST',?)", p);
    clock.now.set(Instant.parse("2030-01-01T12:00:00Z"));
    username = "admin" + UUID.randomUUID().toString().substring(0, 8);
    password = "Test-" + UUID.randomUUID();
    call(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", username, "displayName", "Admin test", "password", password),
        201);
    admin = login(username);
    var category = create("/api/v1/categories", Map.of("name", "General", "active", true));
    service =
        create(
            "/api/v1/services",
            Map.of(
                "name",
                "Sesion completa",
                "categoryId",
                category.get("id").asText(),
                "price",
                100,
                "durationMinutes",
                60,
                "description",
                "",
                "bookableByAgent",
                true,
                "active",
                true));
    doctor = doctor("ana");
    otherDoctor = doctor("luis");
    patient =
        create(
            "/api/v1/patients",
            patientBody("Paciente adulto", "1990-01-01", "+51987654321", false));
  }

  MockHttpSession login(String user) throws Exception {
    var s = new MockHttpSession();
    mvc.perform(
            post("/api/v1/auth/login")
                .session(s)
                .with(csrf())
                .param("username", user)
                .param("password", password))
        .andExpect(status().isOk());
    return s;
  }

  JsonNode call(
      MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int expected)
      throws Exception {
    if (session != null) request.session(session);
    if (body != null)
      request.contentType("application/json").content(mapper.writeValueAsString(body));
    var response = mvc.perform(request).andExpect(status().is(expected)).andReturn().getResponse();
    return response.getContentAsString().isBlank()
        ? null
        : mapper.readTree(response.getContentAsString());
  }

  JsonNode create(String path, Object body) throws Exception {
    return call(post(path).with(csrf()), admin, body, 201);
  }

  JsonNode read(String path) throws Exception {
    return call(get(path), admin, null, 200);
  }

  Map<String, Object> copy(JsonNode n) {
    return mapper.convertValue(
        n, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
  }

  JsonNode doctor(String name) throws Exception {
    var u =
        create(
            "/api/v1/users",
            Map.of(
                "username",
                name,
                "displayName",
                name,
                "email",
                "",
                "password",
                password,
                "active",
                true,
                "roles",
                List.of("DENTIST")));
    var d =
        create(
            "/api/v1/dentists",
            Map.of(
                "userId",
                u.get("id").asText(),
                "fullName",
                name,
                "licenseNumber",
                name,
                "specialty",
                "",
                "active",
                true,
                "serviceIds",
                List.of(service.get("id").asText())));
    create(
        "/api/v1/schedules/periods",
        Map.of(
            "dentistId",
            d.get("id").asText(),
            "dayOfWeek",
            1,
            "kind",
            "WORK",
            "startMinute",
            540,
            "endMinute",
            1080,
            "active",
            true));
    return d;
  }

  Map<String, Object> patientBody(String name, String birth, String phone, boolean guardian) {
    var p = new HashMap<String, Object>();
    p.put("fullName", name);
    p.put("birthDate", birth);
    p.put("documentType", "");
    p.put("documentNumber", "");
    p.put("address", "");
    p.put("email", "");
    p.put("emergencyName", "");
    p.put("emergencyPhone", "");
    p.put("notes", "");
    p.put("active", true);
    p.put("provisional", birth == null);
    p.put(
        "contacts",
        List.of(
            Map.of(
                "phone",
                phone,
                "name",
                guardian ? "Madre" : "Contacto",
                "relationship",
                guardian ? "Madre" : "Paciente",
                "guardian",
                guardian,
                "payer",
                true)));
    return p;
  }

  Map<String, Object> booking(JsonNode d, String start) {
    var b = new HashMap<String, Object>();
    b.put("patientId", patient.get("id").asText());
    b.put("dentistId", d.get("id").asText());
    b.put("serviceId", service.get("id").asText());
    b.put("reason", "");
    b.put("localStart", start);
    b.put("notes", "");
    b.put("requestKey", UUID.randomUUID().toString());
    return b;
  }

  JsonNode reserve(JsonNode d, String start) throws Exception {
    return create("/api/v1/appointments", booking(d, start));
  }

  Map<String, Object> content(String evolution) {
    return Map.of(
        "anamnesis",
        "Anamnesis registrada",
        "evolution",
        evolution,
        "diagnoses",
        "Diagnóstico clínico",
        "indications",
        "Control",
        "procedures",
        List.of(
            Map.of(
                "serviceId",
                service.get("id").asText(),
                "description",
                "Servicio realizado",
                "quantity",
                1,
                "tooth",
                11)));
  }

  Map<String, Object> encounterBody() {
    return Map.of(
        "patientId",
        patient.get("id").asText(),
        "dentistId",
        doctor.get("id").asText(),
        "attendedOn",
        "2029-12-31",
        "reason",
        "Control clínico",
        "content",
        content("Original"),
        "version",
        0);
  }

  JsonNode encounter() throws Exception {
    return create("/api/v1/clinical/encounters", encounterBody());
  }

  JsonNode category() throws Exception {
    return create(
        "/api/v1/documents/categories", Map.of("name", "Fotos", "active", true, "version", 0));
  }

  JsonNode upload(byte[] bytes, String filename, JsonNode category, int expected) throws Exception {
    var metadata =
        new org.springframework.mock.web.MockMultipartFile(
            "metadata",
            "",
            "application/json",
            mapper.writeValueAsBytes(
                Map.of(
                    "patientId",
                    patient.get("id").asText(),
                    "categoryId",
                    category.get("id").asText(),
                    "recordedOn",
                    "2029-12-31",
                    "description",
                    "Original de prueba")));
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", filename, "application/octet-stream", bytes);
    var result =
        mvc.perform(
                multipart("/api/v1/documents")
                    .file(metadata)
                    .file(file)
                    .session(admin)
                    .with(csrf()))
            .andExpect(status().is(expected))
            .andReturn()
            .getResponse();
    return mapper.readTree(result.getContentAsString());
  }

  byte[] image(String format) throws Exception {
    var image = new java.awt.image.BufferedImage(4, 4, java.awt.image.BufferedImage.TYPE_INT_RGB);
    var output = new java.io.ByteArrayOutputStream();
    javax.imageio.ImageIO.write(image, format, output);
    return output.toByteArray();
  }

  byte[] pdf() throws Exception {
    try (var document = new org.apache.pdfbox.pdmodel.PDDocument();
        var output = new java.io.ByteArrayOutputStream()) {
      document.addPage(new org.apache.pdfbox.pdmodel.PDPage());
      document.save(output);
      return output.toByteArray();
    }
  }

  Map<String, Object> item(String price, int sessions) {
    return Map.of(
        "serviceId",
        service.get("id").asText(),
        "description",
        "Tratamiento acordado",
        "quantity",
        1,
        "sessions",
        sessions,
        "unitPrice",
        price,
        "tooth",
        11);
  }

  Map<String, Object> planBody(String price, int sessions) {
    return Map.of(
        "patientId",
        patient.get("id").asText(),
        "dentistId",
        doctor.get("id").asText(),
        "title",
        "Plan integral",
        "conditions",
        "Importe acordado expresamente",
        "items",
        List.of(item(price, sessions)),
        "version",
        0,
        "requestKey",
        UUID.randomUUID().toString());
  }

  JsonNode plan(String price, int sessions) throws Exception {
    return create("/api/v1/plans", planBody(price, sessions));
  }

  Map<String, Object> actionBody(JsonNode plan, boolean release) {
    return Map.of(
        "version",
        plan.get("version").asLong(),
        "requestKey",
        UUID.randomUUID().toString(),
        "reason",
        "Solicitud expresa del paciente",
        "acceptedBy",
        "Paciente responsable",
        "releaseUnperformed",
        release);
  }

  JsonNode action(JsonNode plan, String action) throws Exception {
    return call(
        post("/api/v1/plans/" + plan.get("id").asText() + "/actions/" + action).with(csrf()),
        admin,
        actionBody(plan, false),
        200);
  }

  JsonNode accepted(String price, int count) throws Exception {
    return action(action(plan(price, count), "propose"), "accept");
  }

  JsonNode itemOf(JsonNode plan) throws Exception {
    return read("/api/v1/plans/items?planId=" + plan.get("id").asText()).get("items").get(0);
  }

  JsonNode sessionEncounter(JsonNode item, int count) throws Exception {
    var body = new HashMap<>(encounterBody());
    var clinical = new HashMap<>(content("Evolución de sesión"));
    var procedure =
        new HashMap<>(
            Map.of(
                "serviceId",
                service.get("id").asText(),
                "description",
                "Sesión incluida",
                "quantity",
                count,
                "tooth",
                11,
                "planItemId",
                item.get("id").asText()));
    clinical.put("procedures", List.of(procedure));
    body.put("content", clinical);
    return create("/api/v1/clinical/encounters", body);
  }

  JsonNode finish(JsonNode encounter) throws Exception {
    return call(
        post("/api/v1/clinical/encounters/" + encounter.get("id").asText() + "/finalize")
            .with(csrf()),
        admin,
        Map.of("version", encounter.get("version").asLong()),
        200);
  }

  java.math.BigDecimal debt() {
    return jdbc.queryForObject(
        "select coalesce(sum(amount),0) from charge_entry where patient_id=?",
        java.math.BigDecimal.class,
        UUID.fromString(patient.get("id").asText()));
  }

  @Test
  void proposalCreatesNoDebtAndAcceptanceIsAtomicIdempotent() throws Exception {
    var p = action(plan("1200.00", 3), "propose");
    assertThat(debt()).isEqualByComparingTo("0");
    var req = actionBody(p, false);
    var url = "/api/v1/plans/" + p.get("id").asText() + "/actions/accept";
    var result = call(post(url).with(csrf()), admin, req, 200);
    call(post(url).with(csrf()), admin, req, 200);
    action(result, "accept");
    assertThat(debt()).isEqualByComparingTo("1200");
    assertThat(jdbc.queryForObject("select count(*) from charge_entry", Integer.class))
        .isEqualTo(1);
    var changed = new HashMap<>(req);
    changed.put("acceptedBy", "Otro responsable");
    call(post(url).with(csrf()), admin, changed, 409);
  }

  @Test
  void includedSessionsAdvanceWithoutAnotherDebtAndMustCompleteBeforeFinish() throws Exception {
    var p = accepted("1200", 3);
    var item = itemOf(p);
    call(
        post("/api/v1/plans/" + p.get("id").asText() + "/actions/finish").with(csrf()),
        admin,
        actionBody(p, false),
        409);
    finish(sessionEncounter(item, 1));
    finish(sessionEncounter(item, 2));
    assertThat(debt()).isEqualByComparingTo("1200");
    var refreshed = read("/api/v1/plans/" + p.get("id").asText());
    assertThat(refreshed.get("completedSessions").asInt()).isEqualTo(3);
    assertThat(action(refreshed, "finish").get("status").asText()).isEqualTo("FINISHED");
  }

  @Test
  void individualFinalizationAndCorrectionsKeepOneHistoricalCharge() throws Exception {
    var e = encounter();
    var finished = finish(e);
    finish(e);
    assertThat(debt()).isEqualByComparingTo("100");
    jdbc.update(
        "update dental_service set price=999 where id=?",
        UUID.fromString(service.get("id").asText()));
    call(
        post("/api/v1/clinical/encounters/" + e.get("id").asText() + "/corrections").with(csrf()),
        admin,
        Map.of(
            "version",
            finished.get("version").asLong(),
            "correctionReason",
            "Corrección clínica sin nuevo cobro",
            "content",
            content("Corregida")),
        200);
    assertThat(debt()).isEqualByComparingTo("100");
    assertThat(jdbc.queryForObject("select count(*) from charge_entry", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void explicitPriceAndExactDecimalAmountsAreUsed() throws Exception {
    var body = new HashMap<>(encounterBody());
    var c = new HashMap<>(content("Servicio"));
    c.put(
        "procedures",
        List.of(Map.of("description", "Procedimiento propio", "quantity", 3, "unitPrice", "0.10")));
    body.put("content", c);
    finish(create("/api/v1/clinical/encounters", body));
    assertThat(debt()).isEqualByComparingTo("0.30");
  }

  @Test
  void additionalAndCatalogChangesPreserveAcceptedValues() throws Exception {
    var p = accepted("1200", 3);
    var original = itemOf(p);
    jdbc.update(
        "update dental_service set price=800 where id=?",
        UUID.fromString(service.get("id").asText()));
    var request =
        Map.of(
            "version",
            p.get("version").asLong(),
            "requestKey",
            UUID.randomUUID().toString(),
            "reason",
            "Adicional autorizado",
            "item",
            item("200", 1));
    var url = "/api/v1/plans/" + p.get("id").asText() + "/additional";
    call(post(url).with(csrf()), admin, request, 200);
    call(post(url).with(csrf()), admin, request, 200);
    assertThat(debt()).isEqualByComparingTo("1400");
    assertThat(read("/api/v1/plans/" + p.get("id").asText()).get("status").asText())
        .isEqualTo("ACCEPTED");
    assertThat(read("/api/v1/plans/" + p.get("id").asText()).get("originalTotal").decimalValue())
        .isEqualByComparingTo("1200");
    assertThat(
            read("/api/v1/plans/items/" + original.get("id").asText())
                .get("unitPrice")
                .decimalValue())
        .isEqualByComparingTo("1200");
  }

  @Test
  void adjustmentsAreImmutableReferencedAndCannotErasePerformedValue() throws Exception {
    var p = accepted("1200", 3);
    finish(sessionEncounter(itemOf(p), 1));
    var charge =
        read("/api/v1/charges?patientId=" + patient.get("id").asText()).get("items").get(0);
    var url = "/api/v1/charges/" + charge.get("id").asText() + "/adjustments";
    var req =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "amount",
            "-200",
            "reason",
            "Reducción autorizada");
    call(post(url).with(csrf()), admin, req, 200);
    call(post(url).with(csrf()), admin, req, 200);
    assertThat(debt()).isEqualByComparingTo("1000");
    call(
        post(url).with(csrf()),
        admin,
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "amount",
            "-700",
            "reason",
            "Reducción inválida"),
        400);
    assertThatThrownBy(() -> jdbc.update("update charge_entry set amount=0"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(() -> jdbc.update("delete from charge_entry"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(() -> jdbc.update("update treatment_item set unit_price=1"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }

  @Test
  void cancellationRetainsPerformedPartAndHistory() throws Exception {
    var p = accepted("1200", 3);
    finish(sessionEncounter(itemOf(p), 1));
    p = read("/api/v1/plans/" + p.get("id").asText());
    var req = actionBody(p, true);
    var url = "/api/v1/plans/" + p.get("id").asText() + "/actions/cancel";
    call(post(url).with(csrf()), admin, req, 200);
    call(post(url).with(csrf()), admin, req, 200);
    assertThat(debt()).isEqualByComparingTo("400");
    assertThat(jdbc.queryForObject("select count(*) from charge_entry", Integer.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject("select count(*) from treatment_session", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void invalidSessionRollsBackAllChargesAndClinicalFinalization() throws Exception {
    var p = accepted("1200", 1);
    var item = itemOf(p);
    var e = sessionEncounter(item, 2);
    call(
        post("/api/v1/clinical/encounters/" + e.get("id").asText() + "/finalize").with(csrf()),
        admin,
        Map.of("version", e.get("version").asLong()),
        409);
    assertThat(read("/api/v1/clinical/encounters/" + e.get("id").asText()).get("status").asText())
        .isEqualTo("DRAFT");
    assertThat(jdbc.queryForObject("select count(*) from encounter_revision", Integer.class))
        .isZero();
    assertThat(debt()).isEqualByComparingTo("1200");
    var body = new HashMap<>(encounterBody());
    body.put(
        "patientId",
        create(
                "/api/v1/patients",
                patientBody("Otro paciente", "1990-01-01", "+51988776644", false))
            .get("id")
            .asText());
    body.put(
        "content",
        Map.of(
            "anamnesis",
            "",
            "evolution",
            "Registro",
            "diagnoses",
            "Diagnóstico",
            "indications",
            "",
            "procedures",
            List.of(
                Map.of(
                    "serviceId",
                    service.get("id").asText(),
                    "description",
                    "Sesión ajena",
                    "quantity",
                    1,
                    "tooth",
                    11,
                    "planItemId",
                    item.get("id").asText()))));
    var alien = create("/api/v1/clinical/encounters", body);
    call(
        post("/api/v1/clinical/encounters/" + alien.get("id").asText() + "/finalize").with(csrf()),
        admin,
        Map.of("version", alien.get("version").asLong()),
        400);
  }

  @Test
  void concurrentAcceptanceGeneratesOnlyOneCharge() throws Exception {
    var p = action(plan("1200", 2), "propose");
    var request = actionBody(p, false);
    var key = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var jobs = new ArrayList<Future<Integer>>();
      for (int i = 0; i < 2; i++)
        jobs.add(
            pool.submit(
                () -> {
                  key.await();
                  return mvc.perform(
                          post("/api/v1/plans/" + p.get("id").asText() + "/actions/accept")
                              .session(admin)
                              .with(csrf())
                              .contentType("application/json")
                              .content(mapper.writeValueAsString(request)))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      key.countDown();
      for (var job : jobs) assertThat(job.get(15, TimeUnit.SECONDS)).isEqualTo(200);
      assertThat(debt()).isEqualByComparingTo("1200");
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void paginationPermissionsAndUnacceptedSessionAreEnforced() throws Exception {
    var p = plan("1200", 2);
    var item = itemOf(p);
    var e = sessionEncounter(item, 1);
    call(
        post("/api/v1/clinical/encounters/" + e.get("id").asText() + "/finalize").with(csrf()),
        admin,
        Map.of("version", e.get("version").asLong()),
        409);
    read(
        "/api/v1/plans?patientId="
            + patient.get("id").asText()
            + "&search=integral&size=1&status=DRAFT");
    call(get("/api/v1/plans?size=101"), admin, null, 400);
    call(
        get("/api/v1/charges?patientId=" + patient.get("id").asText() + "&sort=password"),
        admin,
        null,
        400);
    mvc.perform(get("/api/v1/plans")).andExpect(status().isUnauthorized());
    var cashier =
        create(
            "/api/v1/users",
            Map.of(
                "username",
                "cashier4",
                "displayName",
                "Caja",
                "email",
                "",
                "password",
                password,
                "active",
                true,
                "roles",
                List.of("CASHIER")));
    var session = login(cashier.get("username").asText());
    call(post("/api/v1/plans").with(csrf()), session, planBody("1200", 1), 403);
  }

  @Test
  void documentsCanLinkOnlyTreatmentOfSamePatient() throws Exception {
    var p = plan("1200", 1);
    var category = category();
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "foto.png", "image/png", image("png"));
    var metadata =
        new HashMap<String, Object>(
            Map.of(
                "patientId",
                patient.get("id").asText(),
                "planId",
                p.get("id").asText(),
                "categoryId",
                category.get("id").asText(),
                "recordedOn",
                "2029-12-31",
                "description",
                "Sustento del tratamiento"));
    var part =
        new org.springframework.mock.web.MockMultipartFile(
            "metadata", "", "application/json", mapper.writeValueAsBytes(metadata));
    mvc.perform(multipart("/api/v1/documents").file(file).file(part).session(admin).with(csrf()))
        .andExpect(status().isCreated());
    assertThat(
            read("/api/v1/documents?patientId=" + patient.get("id").asText())
                .get("items")
                .get(0)
                .get("planId")
                .asText())
        .isEqualTo(p.get("id").asText());
    var other =
        create(
            "/api/v1/patients", patientBody("Otro paciente", "1990-01-01", "+51988776644", false));
    metadata.put("patientId", other.get("id").asText());
    part =
        new org.springframework.mock.web.MockMultipartFile(
            "metadata", "", "application/json", mapper.writeValueAsBytes(metadata));
    mvc.perform(multipart("/api/v1/documents").file(file).file(part).session(admin).with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void changingOnlyItemsRejectsAStaleEditor() throws Exception {
    var p = plan("1200", 3);
    var first = new HashMap<>(planBody("1100", 3));
    first.put("version", p.get("version").asLong());
    var url = "/api/v1/plans/" + p.get("id").asText();
    var edited = call(put(url).with(csrf()), admin, first, 200);
    assertThat(edited.get("version").asLong()).isGreaterThan(p.get("version").asLong());
    var stale = new HashMap<>(first);
    stale.put("requestKey", UUID.randomUUID().toString());
    stale.put("items", List.of(item("900", 1)));
    call(put(url).with(csrf()), admin, stale, 409);
    assertThat(read(url).get("originalTotal").decimalValue()).isEqualByComparingTo("1100");
  }

  @Test
  void concurrentFinalSessionsCannotExceedPlanCapacity() throws Exception {
    var p = accepted("1200", 1);
    var item = itemOf(p);
    var one = sessionEncounter(item, 1);
    var two = sessionEncounter(item, 1);
    var gate = new CountDownLatch(1);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var jobs = new ArrayList<Future<Integer>>();
      for (var encounter : List.of(one, two))
        jobs.add(
            pool.submit(
                () -> {
                  gate.await();
                  return mvc.perform(
                          post("/api/v1/clinical/encounters/"
                                  + encounter.get("id").asText()
                                  + "/finalize")
                              .session(admin)
                              .with(csrf())
                              .contentType("application/json")
                              .content(
                                  mapper.writeValueAsString(
                                      Map.of("version", encounter.get("version").asLong()))))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      gate.countDown();
      var statuses = new ArrayList<Integer>();
      for (var job : jobs) statuses.add(job.get(15, TimeUnit.SECONDS));
      assertThat(statuses).containsExactlyInAnyOrder(200, 409);
      assertThat(jdbc.queryForObject("select count(*) from treatment_session", Integer.class))
          .isEqualTo(1);
      assertThat(debt()).isEqualByComparingTo("1200");
    } finally {
      pool.shutdownNow();
    }
  }
}
