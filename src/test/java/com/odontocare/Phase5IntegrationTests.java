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
@Import(Phase5IntegrationTests.TimeConfig.class)
class Phase5IntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired TestClock clock;
  @Autowired com.odontocare.finance.service.FinancialPdfService pdfRenderer;
  @Autowired com.odontocare.installation.repository.InstallationProfileRepository profiles;
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
        "TRUNCATE"
            + " agent_proposal,agent_slot,agent_step,agent_run,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
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

  JsonNode charge() throws Exception {
    var p = accepted("1200.00", 3);
    return read("/api/v1/charges?patientId=" + patient.get("id").asText()).get("items").get(0);
  }

  Map<String, Object> paymentBody(String amount, String method, List<?> allocations) {
    var b = new HashMap<String, Object>();
    b.put("requestKey", UUID.randomUUID().toString());
    b.put("patientId", patient.get("id").asText());
    b.put("amount", amount);
    b.put("currency", "PEN");
    b.put("occurredOn", "2030-01-01");
    b.put("method", method);
    b.put("reference", "Referencia verificada");
    b.put("description", "Abono de prueba");
    b.put("allocations", allocations);
    return b;
  }

  Map<String, Object> allocation(JsonNode c, String amount) {
    return Map.of("chargeId", c.get("id").asText(), "amount", amount);
  }

  JsonNode pay(String amount, JsonNode c) throws Exception {
    return create(
        "/api/v1/finance/payments",
        paymentBody(amount, "TRANSFER", c == null ? List.of() : List.of(allocation(c, amount))));
  }

  JsonNode summary() throws Exception {
    return read("/api/v1/finance/summary?patientId=" + patient.get("id").asText()).get(0);
  }

  Map<String, Object> correction(String amount, List<?> releases) {
    return Map.of(
        "requestKey",
        UUID.randomUUID().toString(),
        "amount",
        amount,
        "occurredOn",
        "2030-01-01",
        "reason",
        "Corrección documentada",
        "releases",
        releases);
  }

  JsonNode open() throws Exception {
    return create(
        "/api/v1/finance/cash",
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "currency",
            "PEN",
            "opening",
            "100.00",
            "reason",
            "Fondo inicial"));
  }

  @Test
  void partialPaymentsLeaveSevenHundredPending() throws Exception {
    var c = charge();
    pay("300.00", c);
    pay("200.00", c);
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("700.00");
    assertThat(summary().get("received").decimalValue()).isEqualByComparingTo("500.00");
    assertThat(summary().get("advance").decimalValue()).isEqualByComparingTo("0");
  }

  @Test
  void applyingAdvanceCountsIncomeOnceAndRetriesOnce() throws Exception {
    var c = charge();
    var p = pay("300.00", null);
    var body =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "reason",
            "Aplicación confirmada",
            "allocations",
            List.of(allocation(c, "300.00")));
    for (int i = 0; i < 2; i++)
      call(
          post("/api/v1/finance/payments/" + p.get("id").asText() + "/apply").with(csrf()),
          admin,
          body,
          200);
    assertThat(summary().get("received").decimalValue()).isEqualByComparingTo("300.00");
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("900.00");
    assertThat(jdbc.queryForObject("select count(*) from money_application", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void repeatedPaymentAndChangedFingerprint() throws Exception {
    var c = charge();
    var body = paymentBody("300.00", "TRANSFER", List.of(allocation(c, "300.00")));
    var a = create("/api/v1/finance/payments", body);
    var b = create("/api/v1/finance/payments", body);
    assertThat(b.get("id")).isEqualTo(a.get("id"));
    body.put("amount", "301.00");
    call(post("/api/v1/finance/payments").with(csrf()), admin, body, 409);
    assertThat(jdbc.queryForObject("select count(*) from money_movement", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rejectsOverApplicationAndWrongPatientWithRollback() throws Exception {
    var c = charge();
    call(
        post("/api/v1/finance/payments").with(csrf()),
        admin,
        paymentBody("300.00", "TRANSFER", List.of(allocation(c, "301.00"))),
        400);
    assertThat(jdbc.queryForObject("select count(*) from money_movement", Integer.class)).isZero();
    var other =
        create(
            "/api/v1/patients", patientBody("Otra persona", "1991-01-01", "+51900000001", false));
    var body = paymentBody("100", "TRANSFER", List.of(allocation(c, "100")));
    body.put("patientId", other.get("id").asText());
    call(post("/api/v1/finance/payments").with(csrf()), admin, body, 400);
  }

  @Test
  void reversalPreservesOriginalAndRestoresBalance() throws Exception {
    var c = charge();
    var p = pay("300", c);
    var body = correction("300", List.of());
    var reversed =
        call(
            post("/api/v1/finance/payments/" + p.get("id").asText() + "/reverse").with(csrf()),
            admin,
            body,
            200);
    assertThat(reversed.get("originalId")).isEqualTo(p.get("id"));
    call(
        post("/api/v1/finance/payments/" + p.get("id").asText() + "/reverse").with(csrf()),
        admin,
        body,
        200);
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("1200");
    assertThat(summary().get("received").decimalValue()).isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("select count(*) from money_movement", Integer.class))
        .isEqualTo(2);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update money_movement set amount=1 where id=?::uuid", p.get("id").asText()))
        .isInstanceOf(Exception.class);
  }

  @Test
  void refundRequiresReleaseAndPreservesRemainingMoney() throws Exception {
    var c = charge();
    var p = pay("300", c);
    call(
        post("/api/v1/finance/payments/" + p.get("id").asText() + "/refund").with(csrf()),
        admin,
        correction("100", List.of()),
        400);
    call(
        post("/api/v1/finance/payments/" + p.get("id").asText() + "/refund").with(csrf()),
        admin,
        correction("100", List.of(allocation(c, "100"))),
        200);
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("1000");
    assertThat(summary().get("received").decimalValue()).isEqualByComparingTo("200");
  }

  @Test
  void releaseCreatesAdvanceWithoutChangingIncome() throws Exception {
    var c = charge();
    var p = pay("300", c);
    call(
        post("/api/v1/finance/payments/" + p.get("id").asText() + "/release").with(csrf()),
        admin,
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "reason",
            "Reasignar abono",
            "allocations",
            List.of(allocation(c, "100"))),
        200);
    assertThat(summary().get("advance").decimalValue()).isEqualByComparingTo("100");
    assertThat(summary().get("received").decimalValue()).isEqualByComparingTo("300");
  }

  @Test
  void installmentsDoNotCreateDebtAndPreservePreviousSchedule() throws Exception {
    var c = charge();
    pay("500", c);
    var body =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "chargeId",
            c.get("id").asText(),
            "reason",
            "Acuerdo de cuotas",
            "installments",
            List.of(
                Map.of("dueOn", "2029-12-31", "amount", "400"),
                Map.of("dueOn", "2030-02-01", "amount", "800")));
    create("/api/v1/finance/installments", body);
    create("/api/v1/finance/installments", body);
    var due =
        read("/api/v1/finance/installments?patientId="
                + patient.get("id").asText()
                + "&active=true")
            .get("items");
    assertThat(due.get(0).get("status").asText()).isEqualTo("PAID");
    assertThat(due.get(1).get("paid").decimalValue()).isEqualByComparingTo("100");
    assertThat(summary().get("debt").decimalValue()).isEqualByComparingTo("1200");
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("700");
    var next = new HashMap<String, Object>(body);
    next.put("requestKey", UUID.randomUUID().toString());
    create("/api/v1/finance/installments", next);
    assertThat(
            read("/api/v1/finance/installments?patientId="
                    + patient.get("id").asText()
                    + "&active=false")
                .get("totalElements")
                .asInt())
        .isEqualTo(2);
  }

  @Test
  void invalidInstallmentsAndDiscountRequireRealAvailableDebt() throws Exception {
    var c = charge();
    pay("1100", c);
    call(
        post("/api/v1/finance/installments").with(csrf()),
        admin,
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "chargeId",
            c.get("id").asText(),
            "reason",
            "Cuotas",
            "installments",
            List.of(Map.of("dueOn", "2030-02-01", "amount", "100"))),
        400);
    call(
        post("/api/v1/finance/charges/" + c.get("id").asText() + "/discount").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID().toString(), "amount", "200", "reason", "Descuento"),
        400);
    call(
        post("/api/v1/charges/" + c.get("id").asText() + "/adjustments").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID().toString(), "amount", "-200", "reason", "Ajuste"),
        400);
    call(
        post("/api/v1/finance/charges/" + c.get("id").asText() + "/discount").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID().toString(), "amount", "100", "reason", "Descuento"),
        200);
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("0");
  }

  @Test
  void cashCountsOnlyCashAndClosesWithDifference() throws Exception {
    var c = charge();
    var box = open();
    create("/api/v1/finance/payments", paymentBody("300", "CASH", List.of(allocation(c, "300"))));
    pay("200", c);
    var cat =
        create("/api/v1/finance/expense-categories", Map.of("name", "Insumos", "active", true));
    var expense =
        create(
            "/api/v1/finance/expenses",
            Map.of(
                "requestKey",
                UUID.randomUUID().toString(),
                "categoryId",
                cat.get("id").asText(),
                "amount",
                "50",
                "currency",
                "PEN",
                "occurredOn",
                "2030-01-01",
                "method",
                "CASH",
                "reference",
                "Factura interna",
                "description",
                "Materiales",
                "supplier",
                "Proveedor local"));
    assertThat(read("/api/v1/finance/cash/current").get("expected").decimalValue())
        .isEqualByComparingTo("350");
    assertThat(read("/api/v1/finance/cash/current").get("nonCashNet").decimalValue())
        .isEqualByComparingTo("200");
    var body =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "counted",
            "345",
            "reason",
            "Arqueo confirmado");
    var closed =
        call(
            post("/api/v1/finance/cash/" + box.get("id").asText() + "/close").with(csrf()),
            admin,
            body,
            200);
    assertThat(closed.get("difference").decimalValue()).isEqualByComparingTo("-5");
    call(
        post("/api/v1/finance/cash/" + box.get("id").asText() + "/close").with(csrf()),
        admin,
        body,
        200);
    call(
        post("/api/v1/finance/payments").with(csrf()),
        admin,
        paymentBody("10", "CASH", List.of()),
        409);
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "update cash_session set counted=1 where id=?::uuid", box.get("id").asText()))
        .isInstanceOf(Exception.class);
  }

  @Test
  void receiptIsHistoricalAndStatementContainsBalance() throws Exception {
    var c = charge();
    var p = pay("300", c);
    var doc = read("/api/v1/finance/documents/receipt/" + p.get("id").asText());
    var bytes =
        mvc.perform(
                get("/api/v1/finance/documents/" + doc.get("id").asText() + "/content")
                    .session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    jdbc.update(
        "update installation_profile set display_name='Identidad"
            + " modificada',receipt_prefix='NUEVO'");
    var again =
        mvc.perform(
                get("/api/v1/finance/documents/" + doc.get("id").asText() + "/content")
                    .session(admin))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(again).isEqualTo(bytes);
    try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
      assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf))
          .contains("300.00", "Paciente adulto", p.get("receiptCode").asText());
    }
    var statement =
        create(
            "/api/v1/finance/documents/statement",
            Map.of(
                "requestKey",
                UUID.randomUUID().toString(),
                "patientId",
                patient.get("id").asText(),
                "currency",
                "PEN"));
    var statementBytes =
        mvc.perform(
                get("/api/v1/finance/documents/" + statement.get("id").asText() + "/content")
                    .session(admin))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (var pdf = org.apache.pdfbox.Loader.loadPDF(statementBytes)) {
      assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf))
          .contains("Saldo pendiente: 900.00", "Identidad modificada");
    }
    mvc.perform(
            get("/api/v1/finance/documents/" + statement.get("id").asText() + "/preview")
                .session(admin))
        .andExpect(status().isOk())
        .andExpect(
            org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                .contentType("image/png"));
  }

  @Test
  void supportIsValidatedAndMetadataHasNoBinary() throws Exception {
    var c = charge();
    var p = pay("100", c);
    var metadata =
        new org.springframework.mock.web.MockMultipartFile(
            "metadata",
            "",
            "application/json",
            mapper.writeValueAsBytes(
                Map.of(
                    "movementId",
                    p.get("id").asText(),
                    "description",
                    "Transferencia verificada")));
    var file =
        new org.springframework.mock.web.MockMultipartFile(
            "file", "sustento.pdf", "application/pdf", pdf());
    mvc.perform(
            multipart("/api/v1/finance/documents")
                .file(metadata)
                .file(file)
                .session(admin)
                .with(csrf()))
        .andExpect(status().isCreated());
    var list = read("/api/v1/finance/documents?movementId=" + p.get("id").asText());
    assertThat(list.get("totalElements").asInt()).isEqualTo(2);
    assertThat(list.toString()).doesNotContain("content", "bytes");
    mvc.perform(
            multipart("/api/v1/finance/documents")
                .file(metadata)
                .file(
                    new org.springframework.mock.web.MockMultipartFile(
                        "file", "archivo.exe", "application/octet-stream", new byte[] {1}))
                .session(admin)
                .with(csrf()))
        .andExpect(status().isBadRequest());
  }

  @Test
  void cashierCanCollectButCannotCorrectOrReadClinical() throws Exception {
    var c = charge();
    var u =
        create(
            "/api/v1/users",
            Map.of(
                "username",
                "caja",
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
    for (var permission :
        List.of("FINANCES_READ", "PAYMENTS_WRITE", "CASH_READ", "CASH_WRITE", "EXPENSES_WRITE"))
      jdbc.update("INSERT INTO role_permission VALUES('CASHIER',?)", permission);
    var cashier = login("caja");
    call(
        post("/api/v1/finance/payments").with(csrf()),
        cashier,
        paymentBody("100", "TRANSFER", List.of(allocation(c, "100"))),
        201);
    call(
        post("/api/v1/finance/charges/" + c.get("id").asText() + "/discount").with(csrf()),
        cashier,
        Map.of("requestKey", UUID.randomUUID().toString(), "amount", "1", "reason", "Descuento"),
        403);
    call(get("/api/v1/documents?patientId=" + patient.get("id").asText()), cashier, null, 403);
    call(get("/api/v1/finance/summary?patientId=" + patient.get("id").asText()), null, null, 401);
  }

  @Test
  void concurrentPaymentsCannotOverpayCharge() throws Exception {
    var c = charge();
    var pool = Executors.newFixedThreadPool(2);
    try {
      var start = new CountDownLatch(1);
      var tasks = new ArrayList<Future<Integer>>();
      for (int i = 0; i < 2; i++) {
        var body = paymentBody("800", "TRANSFER", List.of(allocation(c, "800")));
        tasks.add(
            pool.submit(
                () -> {
                  start.await();
                  return mvc.perform(
                          post("/api/v1/finance/payments")
                              .session(admin)
                              .with(csrf())
                              .contentType("application/json")
                              .content(mapper.writeValueAsString(body)))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      start.countDown();
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) statuses.add(task.get(20, TimeUnit.SECONDS));
      assertThat(statuses).containsExactlyInAnyOrder(201, 400);
      assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("400");
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void monetaryDecimalsAndPaginationAreExact() throws Exception {
    var c = charge();
    pay("0.10", c);
    pay("0.20", c);
    assertThat(summary().get("received").decimalValue()).isEqualByComparingTo("0.30");
    assertThat(
            read("/api/v1/finance/movements?patientId="
                    + patient.get("id").asText()
                    + "&size=1&search=Referencia")
                .get("items")
                .size())
        .isEqualTo(1);
    call(get("/api/v1/finance/movements?sort=password"), admin, null, 400);
    call(get("/api/v1/finance/movements?size=101"), admin, null, 400);
    call(
        post("/api/v1/finance/payments").with(csrf()),
        admin,
        paymentBody("0.001", "TRANSFER", List.of()),
        400);
  }

  @Test
  void paymentCanCoverMultipleChargesWithoutCrossingAvailableBalance() throws Exception {
    var a = charge();
    var p = accepted("200", 1);
    var b =
        read("/api/v1/charges?patientId="
                + patient.get("id").asText()
                + "&planId="
                + p.get("id").asText())
            .get("items")
            .get(0);
    var payment =
        create(
            "/api/v1/finance/payments",
            paymentBody("400", "TRANSFER", List.of(allocation(a, "300"), allocation(b, "100"))));
    assertThat(payment.get("applied").decimalValue()).isEqualByComparingTo("400");
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("1000");
  }

  @Test
  void concurrentApplicationCannotSpendTheSameAdvanceTwice() throws Exception {
    var c = charge();
    var p = pay("300", null);
    var pool = Executors.newFixedThreadPool(2);
    try {
      var start = new CountDownLatch(1);
      var tasks = new ArrayList<Future<Integer>>();
      for (int i = 0; i < 2; i++) {
        var body =
            Map.of(
                "requestKey",
                UUID.randomUUID().toString(),
                "reason",
                "Aplicar anticipo",
                "allocations",
                List.of(allocation(c, "300")));
        tasks.add(
            pool.submit(
                () -> {
                  start.await();
                  return mvc.perform(
                          post("/api/v1/finance/payments/" + p.get("id").asText() + "/apply")
                              .session(admin)
                              .with(csrf())
                              .contentType("application/json")
                              .content(mapper.writeValueAsString(body)))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      }
      start.countDown();
      var statuses = new ArrayList<Integer>();
      for (var task : tasks) statuses.add(task.get(20, TimeUnit.SECONDS));
      assertThat(statuses).containsExactlyInAnyOrder(200, 400);
      assertThat(summary().get("advance").decimalValue()).isEqualByComparingTo("0");
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void cancellingPaidPlanDoesNotLosePlanOrApplications() throws Exception {
    var p = accepted("1200", 3);
    var c = read("/api/v1/charges?patientId=" + patient.get("id").asText()).get("items").get(0);
    pay("300", c);
    call(
        post("/api/v1/plans/" + p.get("id").asText() + "/actions/cancel").with(csrf()),
        admin,
        actionBody(p, true),
        400);
    assertThat(read("/api/v1/plans/" + p.get("id").asText()).get("status").asText())
        .isEqualTo("ACCEPTED");
    assertThat(summary().get("pending").decimalValue()).isEqualByComparingTo("900");
  }

  @Test
  void discountAndVoidAreIdempotentAndPreserveCargo() throws Exception {
    var c = charge();
    var r =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "amount",
            "100",
            "reason",
            "Descuento aprobado");
    for (int i = 0; i < 2; i++)
      call(
          post("/api/v1/finance/charges/" + c.get("id").asText() + "/discount").with(csrf()),
          admin,
          r,
          200);
    var v =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "amount",
            "1100",
            "reason",
            "Anulación aprobada");
    for (int i = 0; i < 2; i++)
      call(
          post("/api/v1/finance/charges/" + c.get("id").asText() + "/void").with(csrf()),
          admin,
          v,
          200);
    assertThat(summary().get("debt").decimalValue()).isEqualByComparingTo("0");
    assertThat(jdbc.queryForObject("select count(*) from charge_entry", Integer.class))
        .isEqualTo(3);
  }

  @Test
  void expenseReversalAndInactiveCategoriesKeepHistory() throws Exception {
    var box = open();
    var cat =
        create("/api/v1/finance/expense-categories", Map.of("name", "Gastos", "active", true));
    var r =
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "categoryId",
            cat.get("id").asText(),
            "amount",
            "30",
            "currency",
            "PEN",
            "occurredOn",
            "2030-01-01",
            "method",
            "CASH",
            "reference",
            "",
            "description",
            "Egreso",
            "supplier",
            "Proveedor");
    var e = create("/api/v1/finance/expenses", r);
    create("/api/v1/finance/expenses", r);
    var correction = correction("30", List.of());
    for (int i = 0; i < 2; i++)
      call(
          post("/api/v1/finance/expenses/" + e.get("id").asText() + "/reverse").with(csrf()),
          admin,
          correction,
          200);
    assertThat(read("/api/v1/finance/cash/current").get("expected").decimalValue())
        .isEqualByComparingTo("100");
    call(
        put("/api/v1/finance/expense-categories/" + cat.get("id").asText()).with(csrf()),
        admin,
        Map.of("version", cat.get("version").asLong(), "name", "Gastos antiguos", "active", false),
        200);
    var next = new HashMap<String, Object>(r);
    next.put("requestKey", UUID.randomUUID().toString());
    call(post("/api/v1/finance/expenses").with(csrf()), admin, next, 400);
    assertThat(
            read("/api/v1/finance/movements?kind=EXPENSE")
                .get("items")
                .get(0)
                .get("categoryName")
                .asText())
        .isEqualTo("Gastos");
  }

  @Test
  void pdfIsMultipageAndCashReportKeepsHistoricalIdentity() throws Exception {
    var c = charge();
    var p = pay("100", null);
    for (int i = 0; i < 45; i++)
      call(
          post("/api/v1/finance/payments/" + p.get("id").asText() + "/apply").with(csrf()),
          admin,
          Map.of(
              "requestKey",
              UUID.randomUUID().toString(),
              "reason",
              "Aplicación",
              "allocations",
              List.of(allocation(c, "1"))),
          200);
    var doc =
        create(
            "/api/v1/finance/documents/statement",
            Map.of(
                "requestKey",
                UUID.randomUUID().toString(),
                "patientId",
                patient.get("id").asText(),
                "currency",
                "PEN"));
    var statementBytes =
        mvc.perform(
                get("/api/v1/finance/documents/" + doc.get("id").asText() + "/content")
                    .session(admin))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (var document = org.apache.pdfbox.Loader.loadPDF(statementBytes)) {
      assertThat(document.getNumberOfPages()).isGreaterThan(1);
    }
    var box = open();
    call(
        post("/api/v1/finance/cash/" + box.get("id").asText() + "/close").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID().toString(), "counted", "95", "reason", "Arqueo"),
        200);
    var bytes =
        mvc.perform(
                get("/api/v1/finance/cash/" + box.get("id").asText() + "/report").session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    jdbc.update("update installation_profile set display_name='Nueva identidad'");
    assertThat(
            mvc.perform(
                    get("/api/v1/finance/cash/" + box.get("id").asText() + "/report")
                        .session(admin))
                .andReturn()
                .getResponse()
                .getContentAsByteArray())
        .isEqualTo(bytes);
    try (var pdf = org.apache.pdfbox.Loader.loadPDF(bytes)) {
      assertThat(new org.apache.pdfbox.text.PDFTextStripper().getText(pdf))
          .contains("Efectivo esperado: 100.00", "Diferencia: -5.00");
    }
  }

  @Test
  void pdfHandlesLongNamesLogoAndManyPages() throws Exception {
    var identity = profiles.findById((short) 1).orElseThrow();
    identity.setDisplayName("Clínica Ñandú · Atención de María");
    var lines =
        java.util.stream.IntStream.range(0, 160)
            .mapToObj(i -> "Concepto " + i + " · S/ 100.00 · " + "NombreSinEspacios".repeat(30))
            .toList();
    var bytes = pdfRenderer.render(identity, image("png"), "Estado de cuenta", "REC-000001", lines);
    try (var document = org.apache.pdfbox.Loader.loadPDF(bytes)) {
      assertThat(document.getNumberOfPages()).isGreaterThan(3);
      var text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
      assertThat(text).contains("Clínica Ñandú", "María", "Concepto 159");
      assertThat(document.getPage(0).getResources().getXObjectNames()).isNotEmpty();
    }
    java.nio.file.Files.createDirectories(java.nio.file.Path.of(".runtime/pdf-phase5"));
    try (var document = org.apache.pdfbox.Loader.loadPDF(bytes)) {
      var image = new org.apache.pdfbox.rendering.PDFRenderer(document).renderImage(0, 1);
      javax.imageio.ImageIO.write(
          image,
          "png",
          java.nio.file.Path.of(".runtime/pdf-phase5/estado-multipagina.png").toFile());
    }
    java.nio.file.Files.write(
        java.nio.file.Path.of(".runtime/pdf-phase5/estado-multipagina.pdf"), bytes);
  }

  @Test
  void filesOverLimitAndIncompatibleCashAreRejected() throws Exception {
    var c = charge();
    var p = pay("100", c);
    jdbc.update("update document_policy set max_file_mi_b=1");
    var meta =
        new org.springframework.mock.web.MockMultipartFile(
            "metadata",
            "",
            "application/json",
            mapper.writeValueAsBytes(
                Map.of("movementId", p.get("id").asText(), "description", "Sustento")));
    mvc.perform(
            multipart("/api/v1/finance/documents")
                .file(meta)
                .file(
                    new org.springframework.mock.web.MockMultipartFile(
                        "file", "grande.pdf", "application/pdf", new byte[2 * 1024 * 1024]))
                .session(admin)
                .with(csrf()))
        .andExpect(status().isPayloadTooLarge());
    open();
    var request = paymentBody("1", "CASH", List.of());
    request.put("currency", "USD");
    call(post("/api/v1/finance/payments").with(csrf()), admin, request, 400);
    request.put("currency", "PEN");
    request.put("occurredOn", "2029-12-31");
    call(post("/api/v1/finance/payments").with(csrf()), admin, request, 400);
  }

  @Test
  void restartingCorrelativeCannotReuseAnIssuedStatementCode() throws Exception {
    var c = charge();
    jdbc.update("update installation_profile set receipt_prefix='COL',receipt_next_number=1");
    create(
        "/api/v1/finance/documents/statement",
        Map.of(
            "requestKey",
            UUID.randomUUID().toString(),
            "patientId",
            patient.get("id").asText(),
            "currency",
            "PEN"));
    jdbc.update("update installation_profile set receipt_next_number=1");
    var body = paymentBody("100", "TRANSFER", List.of(allocation(c, "100")));
    call(post("/api/v1/finance/payments").with(csrf()), admin, body, 409);
    assertThat(jdbc.queryForObject("select count(*) from money_movement", Integer.class)).isZero();
    jdbc.update("update installation_profile set receipt_next_number=2");
    var payment = create("/api/v1/finance/payments", body);
    assertThat(payment.get("receiptCode").asText()).isEqualTo("COL-000002");
  }
}
