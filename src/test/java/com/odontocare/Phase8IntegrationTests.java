package com.odontocare;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.reports.model.ReportType;
import com.odontocare.security.model.Permission;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.time.*;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.*;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;

@SpringBootTest(
    properties = {
      "odontocare.ai.worker-enabled=false",
      "odontocare.whatsapp.worker-enabled=false",
      "odontocare.kapso.worker-enabled=false"
    })
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(Phase8IntegrationTests.TimeConfig.class)
class Phase8IntegrationTests {
  @TestConfiguration
  static class TimeConfig {
    @Bean
    @Primary
    Clock phase8Clock() {
      LocalDate today = LocalDate.now(ZoneId.of("America/Lima"));
      return Clock.fixed(today.atStartOfDay(ZoneId.of("America/Lima")).toInstant(), ZoneOffset.UTC);
    }
  }

  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired Clock clock;
  MockHttpSession admin;
  String password;
  LocalDate today;
  JsonNode service, doctor, patient;

  @BeforeEach
  void prepare() throws Exception {
    assertThat(jdbc.queryForObject("SELECT current_database()", String.class))
        .isEqualTo("sistema_odonto_mini_test");
    jdbc.execute(
        "TRUNCATE"
            + " agent_appointment_reference,agent_change_proposal,agent_request_context,agent_supervision,kapso_webhook_event,kapso_message,kapso_conversation,agent_proposal,agent_slot,agent_step,agent_run,agent_message_source,agent_conversation_source,whatsapp_delivery_event,whatsapp_message,whatsapp_conversation,financial_content,financial_document,money_application,finance_operation,money_movement,installment,installment_schedule,cash_session,expense_category,charge_state,charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update(
        "UPDATE installation_profile SET display_name='Clínica"
            + " Ñandú',currency='PEN',time_zone='America/Lima',date_format='DMY',minimum_lead_minutes=0,appointment_gap_minutes=0,patient_next_number=1,budget_next_number=1,receipt_next_number=1,version=0");
    jdbc.update("DELETE FROM role_permission");
    for (var p : Permission.values())
      jdbc.update("INSERT INTO role_permission VALUES('ADMIN',?)", p.name());
    for (String role : List.of("DENTIST", "RECEPTION", "CASHIER"))
      for (String p :
          List.of(
              "PATIENTS_READ",
              "APPOINTMENTS_READ",
              "DENTISTS_READ",
              "SERVICES_READ",
              "SCHEDULES_READ",
              "PLANS_READ",
              "REPORTS_READ",
              "REPORTS_EXPORT")) jdbc.update("INSERT INTO role_permission VALUES(?,?)", role, p);
    for (String role : List.of("DENTIST", "CASHIER"))
      jdbc.update("INSERT INTO role_permission VALUES(?,'FINANCES_READ')", role);
    jdbc.update(
        "INSERT INTO role_permission"
            + " VALUES('DENTIST','CLINICAL_READ'),('RECEPTION','WHATSAPP_READ')");
    password = "Prueba-" + UUID.randomUUID();
    today = LocalDate.now(clock.withZone(ZoneId.of("America/Lima")));
    String user = "report-admin-" + UUID.randomUUID().toString().substring(0, 8);
    call(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", user, "displayName", "Administradora de reportes", "password", password),
        201);
    admin = login(user);
    var category =
        create("/api/v1/categories", Map.of("name", "Odontología general", "active", true));
    service =
        create(
            "/api/v1/services",
            Map.of(
                "name",
                "Limpieza dental",
                "categoryId",
                id(category),
                "price",
                200,
                "durationMinutes",
                60,
                "description",
                "",
                "bookableByAgent",
                true,
                "active",
                true));
    doctor = doctor("Julia Prueba");
    patient = patient("María Ñandú 10%_especial", "+51999000001");
  }

  @Test
  void allReportsHaveRealEmptyStatesAndThePanelRespectsDomains() throws Exception {
    assertThat(read("/api/v1/reports/catalog").path("reports").size()).isEqualTo(9);
    for (var type : ReportType.values()) {
      var report = report(type);
      assertThat(report.path("items").size()).isZero();
      assertThat(report.path("totalElements").asLong()).isZero();
    }
    var panel = read("/api/v1/dashboard");
    assertThat(panel.path("appointments").path("total").asLong()).isZero();
    assertThat(panel.path("money").get(0).path("collections").asString()).isEqualTo("0");
    var reception = roleSession("RECEPTION");
    var privatePanel = call(get("/api/v1/dashboard"), reception, null, 200);
    assertThat(privatePanel.path("money").isNull()).isTrue();
    assertThat(privatePanel.path("balances").isNull()).isTrue();
    assertThat(privatePanel.path("overdue").isNull()).isTrue();
    assertThat(call(get("/api/v1/reports/catalog"), reception, null, 200).path("reports").size())
        .isEqualTo(3);
    call(get("/api/v1/reports/CHARGES"), reception, null, 403);
    call(get("/api/v1/reports/COLLECTIONS/export").param("format", "CSV"), reception, null, 403);
    call(get("/api/v1/dashboard"), null, null, 401);
  }

  @Test
  void budgetAndAcceptedDebtAreNotCollectionsAndCatalogChangesDoNotRewriteThem() throws Exception {
    var plan = plan("1200", 3);
    moneyMetric(report(ReportType.TREATMENTS), "budget", "PEN", "1200");
    assertThat(report(ReportType.CHARGES).path("totalElements").asLong()).isZero();
    accept(plan);
    moneyMetric(report(ReportType.CHARGES), "issued", "PEN", "1200");
    assertThat(report(ReportType.COLLECTIONS).path("totalElements").asLong()).isZero();
    jdbc.update(
        "UPDATE dental_service SET price=900,name='Nombre nuevo' WHERE id=?", uuid(service));
    moneyMetric(report(ReportType.TREATMENTS), "budget", "PEN", "1200");
    moneyMetric(report(ReportType.CHARGES), "amount", "PEN", "1200");
  }

  @Test
  void paymentsAdvanceApplicationsCorrectionsAndExpensesReconcileExactly() throws Exception {
    var plan = plan("1200", 3);
    accept(plan);
    var charge = charge();
    var first = pay("300", "PEN", List.of(allocation(charge, "300")));
    pay("200", "PEN", List.of(allocation(charge, "200")));
    moneyMetric(report(ReportType.BALANCES), "pending", "PEN", "700");
    var advance = pay("100", "PEN", List.of());
    moneyMetric(report(ReportType.BALANCES), "advance", "PEN", "100");
    call(
        post("/api/v1/finance/payments/" + id(advance) + "/apply").with(csrf()),
        admin,
        Map.of(
            "requestKey",
            UUID.randomUUID(),
            "reason",
            "Aplicar anticipo",
            "allocations",
            List.of(allocation(charge, "100"))),
        200);
    assertThat(report(ReportType.COLLECTIONS).path("totalElements").asLong()).isEqualTo(3);
    moneyMetric(report(ReportType.COLLECTIONS), "amount", "PEN", "600");
    call(
        post("/api/v1/finance/payments/" + id(first) + "/refund").with(csrf()),
        admin,
        correction("50", List.of(allocation(charge, "50"))),
        200);
    var category =
        create("/api/v1/finance/expense-categories", Map.of("name", "Materiales", "active", true));
    var expense =
        create(
            "/api/v1/finance/expenses",
            Map.of(
                "requestKey",
                UUID.randomUUID(),
                "categoryId",
                id(category),
                "amount",
                "100",
                "currency",
                "PEN",
                "occurredOn",
                today.toString(),
                "method",
                "TRANSFER",
                "reference",
                "SUST-1",
                "description",
                "Insumos de prueba",
                "supplier",
                "Proveedor ficticio"));
    call(
        post("/api/v1/finance/expenses/" + id(expense) + "/reverse").with(csrf()),
        admin,
        correction("100", List.of()),
        200);
    create(
        "/api/v1/finance/expenses",
        Map.of(
            "requestKey",
            UUID.randomUUID(),
            "categoryId",
            id(category),
            "amount",
            "80",
            "currency",
            "PEN",
            "occurredOn",
            today.toString(),
            "method",
            "TRANSFER",
            "reference",
            "SUST-2",
            "description",
            "Egreso corregido",
            "supplier",
            "Proveedor ficticio"));
    moneyMetric(report(ReportType.COLLECTIONS), "amount", "PEN", "550");
    moneyMetric(report(ReportType.EXPENSES), "amount", "PEN", "80");
    moneyMetric(report(ReportType.BALANCES), "pending", "PEN", "650");
    moneyMetric(report(ReportType.BALANCES), "advance", "PEN", "0");
    var panel = read("/api/v1/dashboard").path("money").get(0);
    assertMoney(panel.path("net").asString(), "470");
    var row = report(ReportType.COLLECTIONS).path("items").get(0);
    assertThat(row.path("documentId").isNull()).isFalse();
    byte[] pdf =
        mvc.perform(
                get("/api/v1/finance/documents/" + row.path("documentId").asString() + "/content")
                    .session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    assertThat(new String(pdf, 0, 4, StandardCharsets.US_ASCII)).isEqualTo("%PDF");
  }

  @Test
  void clinicalWorkUsesTheLatestRevisionWithoutBillingPlanSessionsAgain() throws Exception {
    var plan = plan("1200", 3);
    accept(plan);
    var item = read("/api/v1/plans/items?planId=" + id(plan)).path("items").get(0);
    var content = content("Sesión clínica registrada", id(item));
    var encounter =
        create(
            "/api/v1/clinical/encounters",
            Map.of(
                "patientId",
                id(patient),
                "dentistId",
                id(doctor),
                "attendedOn",
                today.toString(),
                "reason",
                "Control de prueba",
                "content",
                content,
                "version",
                0));
    encounter =
        call(
            post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
            admin,
            Map.of("version", encounter.path("version").asLong()),
            200);
    var changed = content("Sesión corregida", id(item));
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/corrections").with(csrf()),
        admin,
        Map.of(
            "version",
            encounter.path("version").asLong(),
            "correctionReason",
            "Precisión documental",
            "content",
            changed),
        200);
    var work = report(ReportType.SERVICES);
    assertThat(work.path("totalElements").asLong()).isEqualTo(1);
    assertThat(work.path("items").get(0).path("values").path("description").asString())
        .isEqualTo("Sesión corregida");
    assertThat(work.path("items").get(0).path("values").path("planSessions").asString())
        .isEqualTo("1");
    moneyMetric(report(ReportType.CHARGES), "amount", "PEN", "1200");
    var treatments = report(ReportType.TREATMENTS);
    assertThat(treatments.path("items").get(0).path("values").path("completed").asString())
        .isEqualTo("1");
    assertThat(work.toString()).doesNotContain("Diagnóstico ficticio", "Evolución ficticia");
  }

  @Test
  void individualWorkAndProfessionalFiltersUseTheOriginalChargeAttribution() throws Exception {
    var encounter =
        create(
            "/api/v1/clinical/encounters",
            Map.of(
                "patientId",
                id(patient),
                "dentistId",
                id(doctor),
                "attendedOn",
                today.toString(),
                "reason",
                "Atención individual",
                "content",
                content("Servicio individual", null),
                "version",
                0));
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.path("version").asLong()),
        200);
    var charge = charge();
    call(
        post("/api/v1/finance/charges/" + id(charge) + "/discount").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID(), "amount", "20", "reason", "Descuento documentado"),
        200);
    var page =
        call(get("/api/v1/reports/CHARGES").param("dentistId", id(doctor)), admin, null, 200);
    assertThat(page.path("totalElements").asLong()).isEqualTo(2);
    moneyMetric(page, "amount", "PEN", "180");
    assertThat(
            call(
                    get("/api/v1/reports/CHARGES").param("dentistId", UUID.randomUUID().toString()),
                    admin,
                    null,
                    200)
                .path("totalElements")
                .asLong())
        .isZero();
    call(get("/api/v1/reports/COLLECTIONS").param("dentistId", id(doctor)), admin, null, 400);
    call(get("/api/v1/reports/EXPENSES").param("patientId", id(patient)), admin, null, 400);
    moneyMetric(report(ReportType.SERVICES), "units", null, "1");
  }

  @Test
  void currenciesAreSeparatedAndSmallMoneyValuesRemainExact() throws Exception {
    pay("0.10", "PEN", List.of());
    pay("0.20", "PEN", List.of());
    pay("20.00", "USD", List.of());
    var page = report(ReportType.COLLECTIONS);
    moneyMetric(page, "amount", "PEN", "0.30");
    moneyMetric(page, "amount", "USD", "20.00");
    var panel = read("/api/v1/dashboard").path("money");
    assertThat(panel.size()).isEqualTo(2);
    assertMoney(panel.get(0).path("net").asString(), "0.30");
    assertMoney(panel.get(1).path("net").asString(), "20");
    var usd = call(get("/api/v1/reports/COLLECTIONS").param("currency", "USD"), admin, null, 200);
    assertThat(usd.path("totalElements").asLong()).isEqualTo(1);
  }

  @Test
  void installmentDistributionAndOverduePanelDoNotDuplicateDebt() throws Exception {
    var plan = plan("1200", 3);
    accept(plan);
    var charge = charge();
    pay("300", "PEN", List.of(allocation(charge, "300")));
    create(
        "/api/v1/finance/installments",
        Map.of(
            "requestKey",
            UUID.randomUUID(),
            "chargeId",
            id(charge),
            "reason",
            "Cuotas acordadas",
            "installments",
            List.of(
                Map.of("amount", "400", "dueOn", today.minusDays(500).toString()),
                Map.of("amount", "400", "dueOn", today.minusDays(1).toString()),
                Map.of("amount", "400", "dueOn", today.plusDays(30).toString()))));
    var panel = read("/api/v1/dashboard");
    moneyMetric(panel.path("overdue"), "records", null, "2");
    moneyMetric(panel.path("overdue"), "pending", "PEN", "500");
    moneyMetric(report(ReportType.BALANCES), "pending", "PEN", "900");
    var overdue =
        call(
            get("/api/v1/reports/INSTALLMENTS")
                .param("status", "OVERDUE")
                .param("includeEarlier", "true"),
            admin,
            null,
            200);
    assertThat(overdue.path("totalElements").asLong()).isEqualTo(2);
    moneyMetric(overdue, "pending", "PEN", "500");
    call(
        post("/api/v1/finance/charges/" + id(charge) + "/discount").with(csrf()),
        admin,
        Map.of("requestKey", UUID.randomUUID(), "amount", "100", "reason", "Ajuste del acuerdo"),
        200);
    var review =
        call(
            get("/api/v1/reports/INSTALLMENTS")
                .param("status", "REVIEW")
                .param("includeEarlier", "true"),
            admin,
            null,
            200);
    assertThat(review.path("totalElements").asLong()).isEqualTo(2);
  }

  @Test
  void reportPagingTotalsAndLiteralSearchComeFromTheEntireFilteredResult() throws Exception {
    appointments(25);
    var page =
        call(
            get("/api/v1/reports/APPOINTMENTS").param("size", "10").param("sort", "date"),
            admin,
            null,
            200);
    assertThat(page.path("items").size()).isEqualTo(10);
    assertThat(page.path("totalElements").asLong()).isEqualTo(25);
    var next =
        call(
            get("/api/v1/reports/APPOINTMENTS").param("size", "10").param("page", "2"),
            admin,
            null,
            200);
    assertThat(next.path("items").size()).isEqualTo(5);
    moneyMetric(next, "records", null, "25");
    var literal =
        call(get("/api/v1/reports/APPOINTMENTS").param("search", "10%_especial"), admin, null, 200);
    assertThat(literal.path("totalElements").asLong()).isEqualTo(25);
    assertThat(
            call(
                    get("/api/v1/reports/APPOINTMENTS").param("search", "10___especial"),
                    admin,
                    null,
                    200)
                .path("totalElements")
                .asLong())
        .isZero();
    assertThat(read("/api/v1/dashboard").path("appointments").path("cancelled").asLong())
        .isEqualTo(25);
  }

  @Test
  void exportIncludesFilteredRowsBeyondTheVisiblePageAndProtectsSpreadsheetText() throws Exception {
    appointments(25);
    var bytes =
        mvc.perform(
                get("/api/v1/reports/APPOINTMENTS/export")
                    .param("format", "CSV")
                    .param("size", "10")
                    .param("page", "2")
                    .session(admin))
            .andExpect(status().isOk())
            .andExpect(header().string("Cache-Control", "no-store"))
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    String csv = new String(bytes, StandardCharsets.UTF_8);
    assertThat(csv.lines().count()).isEqualTo(26);
    jdbc.update("UPDATE patient SET full_name=' =HYPERLINK(\"evil\")' WHERE id=?", uuid(patient));
    csv =
        new String(
            mvc.perform(
                    get("/api/v1/reports/APPOINTMENTS/export")
                        .param("format", "CSV")
                        .session(admin))
                .andReturn()
                .getResponse()
                .getContentAsByteArray(),
            StandardCharsets.UTF_8);
    assertThat(csv).contains("\"' =HYPERLINK(\"\"evil\"\")\"");
    assertThat(
            jdbc.queryForObject(
                "SELECT count(*) FROM audit_event WHERE action='REPORT_EXPORTED'", Long.class))
        .isEqualTo(2);
  }

  @Test
  void pdfHasReadableSpanishFiltersTotalsAndMultiplePages() throws Exception {
    appointments(40);
    byte[] bytes =
        mvc.perform(
                get("/api/v1/reports/APPOINTMENTS/export")
                    .param("format", "PDF")
                    .param("patientId", id(patient))
                    .param("dentistId", id(doctor))
                    .session(admin))
            .andExpect(status().isOk())
            .andReturn()
            .getResponse()
            .getContentAsByteArray();
    try (var document = org.apache.pdfbox.Loader.loadPDF(bytes)) {
      assertThat(document.getNumberOfPages()).isGreaterThan(1);
      String text = new org.apache.pdfbox.text.PDFTextStripper().getText(document);
      assertThat(text)
          .contains(
              "Clínica Ñandú",
              "María Ñandú",
              "Julia Prueba",
              "Resumen",
              "Detalle del resultado filtrado",
              "Fecha y hora");
      assertThat(text).doesNotContain(id(patient), id(doctor));
      Files.createDirectories(Path.of(".runtime/phase8-pdf"));
      Files.write(Path.of(".runtime/phase8-pdf/citas.pdf"), bytes);
      var renderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
      javax.imageio.ImageIO.write(
          renderer.renderImage(0, 1.2f),
          "png",
          Path.of(".runtime/phase8-pdf/citas-1.png").toFile());
      javax.imageio.ImageIO.write(
          renderer.renderImage(document.getNumberOfPages() - 1, 1.2f),
          "png",
          Path.of(".runtime/phase8-pdf/citas-final.png").toFile());
    }
  }

  @Test
  void invalidRangesStatusesSortsAndHugePagesAreRejectedBeforeReading() throws Exception {
    for (var values :
        List.of(
            Map.of("size", "101"),
            Map.of("page", "-1"),
            Map.of("sort", "id; DROP TABLE patient"),
            Map.of("direction", "random"),
            Map.of("status", "INVENTED"),
            Map.of("from", "2000-01-01", "to", "2030-01-01"),
            Map.of("from", today.toString(), "to", today.minusDays(1).toString()),
            Map.of("includeEarlier", "true"),
            Map.of("search", "x".repeat(161)))) {
      var request = get("/api/v1/reports/APPOINTMENTS");
      values.forEach(request::param);
      call(request, admin, null, 400);
    }
    call(get("/api/v1/reports/APPOINTMENTS/export").param("format", "XML"), admin, null, 400);
  }

  @Test
  void exportLimitsAreEnforcedAndMissingExportPermissionCannotBeBypassed() throws Exception {
    appointments(501);
    call(get("/api/v1/reports/APPOINTMENTS/export").param("format", "PDF"), admin, null, 400);
    appointments(9500);
    call(get("/api/v1/reports/APPOINTMENTS/export").param("format", "CSV"), admin, null, 400);
    var reception = roleSession("RECEPTION");
    jdbc.update(
        "DELETE FROM role_permission WHERE role_code='RECEPTION' AND permission='REPORTS_EXPORT'");
    reception = login("phase8-reception");
    call(get("/api/v1/reports/APPOINTMENTS/export").param("format", "CSV"), reception, null, 403);
    assertThat(
            call(get("/api/v1/reports/APPOINTMENTS"), reception, null, 200)
                .path("totalElements")
                .asLong())
        .isEqualTo(10001);
  }

  @Test
  void appointmentCalendarCanBeScopedToThePatientWithoutChangingTheGlobalAgenda() throws Exception {
    appointments(2);
    var other = patient("Otro paciente", "+51999000002");
    appointments(
        1,
        uuid(other),
        "CONFIRMED",
        today.atTime(14, 0).atZone(ZoneId.of("America/Lima")).toInstant());
    var calendar =
        call(
            get("/api/v1/appointments/calendar")
                .param("from", today.toString())
                .param("to", today.toString())
                .param("patientId", id(patient)),
            admin,
            null,
            200);
    assertThat(calendar.path("items").size()).isEqualTo(2);
    assertThat(
            call(
                    get("/api/v1/appointments/calendar")
                        .param("from", today.toString())
                        .param("to", today.toString()),
                    admin,
                    null,
                    200)
                .path("items")
                .size())
        .isEqualTo(3);
  }

  @Test
  void historicalBalancesIncludeOlderDebtAndExcludeLaterRecordedApplications() throws Exception {
    LocalDate previous = today.withDayOfMonth(1).minusMonths(1);
    UUID rootCharge = seedCharge("1000", previous);
    UUID oldPayment = seedPayment("300", previous.plusDays(2), previous.plusDays(2));
    seedApplication(rootCharge, oldPayment, "300", previous.plusDays(2));
    LocalDate cut = previous.plusDays(10);
    var historical =
        call(
            get("/api/v1/reports/BALANCES")
                .param("from", cut.toString())
                .param("to", cut.toString()),
            admin,
            null,
            200);
    moneyMetric(historical, "debt", "PEN", "1000");
    moneyMetric(historical, "pending", "PEN", "700");
    UUID recentPayment = seedPayment("200", previous.plusDays(4), today);
    seedApplication(rootCharge, recentPayment, "200", today);
    jdbc.update(
        "INSERT INTO"
            + " charge_entry(id,patient_id,original_id,source_key,fingerprint,kind,description,currency,amount,reason,actor_name)"
            + " VALUES(?,?,?,?,'fixture','ADJUSTMENT','Ajuste"
            + " posterior','PEN',-100,'Descuento','Sistema de prueba')",
        UUID.randomUUID(),
        uuid(patient),
        rootCharge,
        "adjust:" + UUID.randomUUID());
    moneyMetric(
        call(
            get("/api/v1/reports/BALANCES")
                .param("from", cut.toString())
                .param("to", cut.toString()),
            admin,
            null,
            200),
        "pending",
        "PEN",
        "700");
    var current =
        call(
            get("/api/v1/reports/BALANCES")
                .param("from", today.toString())
                .param("to", today.toString()),
            admin,
            null,
            200);
    moneyMetric(current, "debt", "PEN", "900");
    moneyMetric(current, "pending", "PEN", "400");
    assertThat(report(ReportType.BALANCES).path("items").size()).isEqualTo(1);
  }

  @Test
  void localDayBoundariesAndInasistencesUseTheClinicZone() throws Exception {
    Instant previousLocalDay = today.atTime(3, 0).toInstant(ZoneOffset.UTC);
    Instant currentLocalDay = today.atTime(14, 0).toInstant(ZoneOffset.UTC);
    appointments(1, uuid(patient), "CANCELLED", previousLocalDay);
    appointments(1, uuid(patient), "NO_SHOW", currentLocalDay);
    var panel = call(get("/api/v1/dashboard").param("day", today.toString()), admin, null, 200);
    assertThat(panel.path("appointments").path("total").asLong()).isEqualTo(1);
    assertThat(panel.path("appointments").path("noShow").asLong()).isEqualTo(1);
    var absence =
        call(
            get("/api/v1/reports/ABSENCES")
                .param("from", today.toString())
                .param("to", today.toString()),
            admin,
            null,
            200);
    assertThat(absence.path("items").get(0).path("values").path("date").asString())
        .contains("T09:00");
    assertThat(absence.path("totalElements").asLong()).isEqualTo(1);
  }

  @Test
  void monetaryTotalsDoNotChangeWithPagination() throws Exception {
    for (int i = 0; i < 30; i++) seedPayment("0.10", today, today);
    var first = call(get("/api/v1/reports/COLLECTIONS").param("size", "10"), admin, null, 200);
    var last =
        call(
            get("/api/v1/reports/COLLECTIONS").param("size", "10").param("page", "2"),
            admin,
            null,
            200);
    assertThat(first.path("items").size()).isEqualTo(10);
    assertThat(last.path("items").size()).isEqualTo(10);
    moneyMetric(first, "amount", "PEN", "3.00");
    moneyMetric(last, "amount", "PEN", "3.00");
  }

  @Test
  void historicalScheduleSelectionDoesNotUseTheCurrentReplacement() throws Exception {
    LocalDate previous = today.withDayOfMonth(1).minusMonths(1);
    UUID charge = seedCharge("1000", previous);
    UUID old = UUID.randomUUID(), current = UUID.randomUUID();
    seedSchedule(old, charge, previous, false);
    seedDue(old, 1, previous.plusDays(2));
    seedDue(old, 2, previous.plusDays(5));
    seedSchedule(current, charge, today, true);
    seedDue(current, 1, today.plusDays(1));
    seedDue(current, 2, today.plusDays(30));
    LocalDate cut = previous.plusDays(10);
    var historical =
        call(
            get("/api/v1/reports/INSTALLMENTS")
                .param("from", cut.toString())
                .param("to", cut.toString())
                .param("includeEarlier", "true"),
            admin,
            null,
            200);
    assertThat(historical.path("totalElements").asLong()).isEqualTo(2);
    assertThat(read("/api/v1/dashboard").path("overdue").get(0).path("value").asString())
        .isEqualTo("0");
  }

  @Test
  void ongoingPlansAreIncludedForSessionsInThePeriodEvenWhenAcceptedEarlier() throws Exception {
    LocalDate previous = today.withDayOfMonth(1).minusMonths(1);
    UUID plan = UUID.randomUUID(), item = UUID.randomUUID();
    var at =
        java.sql.Timestamp.from(
            previous.atTime(9, 0).atZone(ZoneId.of("America/Lima")).toInstant());
    jdbc.update(
        "INSERT INTO"
            + " treatment_plan(id,patient_id,dentist_id,dentist_name,patient_name,code,title,conditions,currency,status,accepted_total,accepted_by,accepted_at,created_at,updated_at)"
            + " VALUES(?,?,?,'Julia Prueba','María Ñandú','HIST-PLAN','Plan en"
            + " seguimiento','','PEN','ACCEPTED',1200,'María Ñandú',?,?,?)",
        plan,
        uuid(patient),
        uuid(doctor),
        at,
        at,
        at);
    jdbc.update(
        "INSERT INTO"
            + " treatment_item(id,plan_id,service_id,service_name,description,quantity,sessions,unit_price,position,created_at,updated_at)"
            + " VALUES(?,?,?,'Limpieza dental','Tratamiento histórico',6,3,200,1,?,?)",
        item,
        plan,
        uuid(service),
        at,
        at);
    var encounter =
        create(
            "/api/v1/clinical/encounters",
            Map.of(
                "patientId",
                id(patient),
                "dentistId",
                id(doctor),
                "attendedOn",
                today.toString(),
                "reason",
                "Continuar plan",
                "content",
                content("Sesión de seguimiento", item.toString()),
                "version",
                0));
    call(
        post("/api/v1/clinical/encounters/" + id(encounter) + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.path("version").asLong()),
        200);
    var page =
        call(
            get("/api/v1/reports/TREATMENTS")
                .param("from", today.toString())
                .param("to", today.toString()),
            admin,
            null,
            200);
    assertThat(page.path("totalElements").asLong()).isEqualTo(1);
    assertThat(page.path("items").get(0).path("values").path("periodSessions").asString())
        .isEqualTo("1");
  }

  @Test
  void dashboardCanBeUsedWithoutReportPermissionButTheReportApiRemainsRestricted()
      throws Exception {
    roleSession("CASHIER");
    jdbc.update(
        "DELETE FROM role_permission WHERE role_code='CASHIER' AND permission"
            + " IN('REPORTS_READ','REPORTS_EXPORT')");
    var cashier = login("phase8-cashier");
    assertThat(call(get("/api/v1/dashboard"), cashier, null, 200).path("money").isArray()).isTrue();
    call(get("/api/v1/reports/catalog"), cashier, null, 403);
    call(get("/api/v1/reports/COLLECTIONS/export").param("format", "CSV"), cashier, null, 403);
  }

  void seedSchedule(UUID id, UUID charge, LocalDate registered, boolean active) {
    var at =
        java.sql.Timestamp.from(
            registered.atTime(9, 0).atZone(ZoneId.of("America/Lima")).toInstant());
    jdbc.update(
        "INSERT INTO"
            + " installment_schedule(id,patient_id,charge_id,total,active,reason,actor_name,created_at,updated_at)"
            + " VALUES(?,?,?,1000,?,'Acuerdo histórico','Sistema de prueba',?,?)",
        id,
        uuid(patient),
        charge,
        active,
        at,
        at);
  }

  void seedDue(UUID schedule, int position, LocalDate due) {
    jdbc.update(
        "INSERT INTO installment(id,schedule_id,position,due_on,amount) VALUES(?,?,?,?,500)",
        UUID.randomUUID(),
        schedule,
        position,
        java.sql.Date.valueOf(due));
  }

  UUID seedCharge(String amount, LocalDate at) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        "INSERT INTO"
            + " charge_entry(id,patient_id,source_key,fingerprint,kind,description,currency,amount,unit_price,quantity,reason,actor_name,created_at,updated_at)"
            + " VALUES(?,?,?,'fixture','SERVICE','Cargo"
            + " histórico','PEN',?::numeric,?::numeric,1,'Historial de prueba','Sistema de"
            + " prueba',?,?)",
        id,
        uuid(patient),
        "history:" + id,
        amount,
        amount,
        java.sql.Timestamp.from(at.atTime(12, 0).atZone(ZoneId.of("America/Lima")).toInstant()),
        java.sql.Timestamp.from(at.atTime(12, 0).atZone(ZoneId.of("America/Lima")).toInstant()));
    return id;
  }

  UUID seedPayment(String amount, LocalDate occurred, LocalDate registered) {
    UUID id = UUID.randomUUID();
    var at =
        java.sql.Timestamp.from(
            registered.atTime(12, 0).atZone(ZoneId.of("America/Lima")).toInstant());
    jdbc.update(
        "INSERT INTO"
            + " money_movement(id,patient_id,kind,currency,amount,occurred_on,method,reference,description,supplier,actor_name,category_name,created_at,updated_at)"
            + " VALUES(?,?,'PAYMENT','PEN',?::numeric,?,'TRANSFER','HIST','Pago"
            + " histórico','','Sistema de prueba','',?,?)",
        id,
        uuid(patient),
        amount,
        java.sql.Date.valueOf(occurred),
        at,
        at);
    return id;
  }

  void seedApplication(UUID charge, UUID payment, String amount, LocalDate registered) {
    UUID operation = UUID.randomUUID();
    var at =
        java.sql.Timestamp.from(
            registered.atTime(12, 0).atZone(ZoneId.of("America/Lima")).toInstant());
    jdbc.update(
        "INSERT INTO"
            + " finance_operation(id,request_key,fingerprint,action,result_id,reason,actor_name,created_at,updated_at)"
            + " VALUES(?,?,'fixture','APPLY',?,'Aplicación histórica','Sistema de prueba',?,?)",
        operation,
        UUID.randomUUID(),
        payment,
        at,
        at);
    jdbc.update(
        "INSERT INTO"
            + " money_application(id,patient_id,payment_id,charge_id,operation_id,amount,created_at,updated_at)"
            + " VALUES(?,?,?,?,?,?::numeric,?,?)",
        UUID.randomUUID(),
        uuid(patient),
        payment,
        charge,
        operation,
        amount,
        at,
        at);
  }

  JsonNode report(ReportType type) throws Exception {
    return read("/api/v1/reports/" + type);
  }

  JsonNode read(String path) throws Exception {
    return call(get(path), admin, null, 200);
  }

  JsonNode create(String path, Object body) throws Exception {
    return call(post(path).with(csrf()), admin, body, 201);
  }

  String id(JsonNode node) {
    return node.path("id").asString();
  }

  UUID uuid(JsonNode node) {
    return UUID.fromString(id(node));
  }

  JsonNode doctor(String name) throws Exception {
    var user =
        create(
            "/api/v1/users",
            Map.of(
                "username",
                "doctor-" + UUID.randomUUID().toString().substring(0, 8),
                "displayName",
                name,
                "password",
                password,
                "email",
                "",
                "roles",
                List.of("DENTIST"),
                "active",
                true));
    return create(
        "/api/v1/dentists",
        Map.of(
            "userId",
            id(user),
            "fullName",
            name,
            "licenseNumber",
            "COP-" + UUID.randomUUID().toString().substring(0, 8),
            "specialty",
            "Odontología general",
            "serviceIds",
            List.of(id(service)),
            "active",
            true));
  }

  JsonNode patient(String name, String phone) throws Exception {
    var body = new LinkedHashMap<String, Object>();
    body.put("fullName", name);
    body.put("birthDate", "1990-01-01");
    for (String key :
        List.of(
            "documentType",
            "documentNumber",
            "address",
            "email",
            "emergencyName",
            "emergencyPhone",
            "notes")) body.put(key, "");
    body.put("active", true);
    body.put("provisional", false);
    body.put(
        "contacts",
        List.of(
            Map.of(
                "phone",
                phone,
                "name",
                name,
                "relationship",
                "Paciente",
                "guardian",
                false,
                "payer",
                true)));
    return create("/api/v1/patients", body);
  }

  JsonNode plan(String price, int sessions) throws Exception {
    return create(
        "/api/v1/plans",
        Map.of(
            "patientId",
            id(patient),
            "dentistId",
            id(doctor),
            "title",
            "Plan de limpieza",
            "conditions",
            "Acuerdo ficticio",
            "version",
            0,
            "requestKey",
            UUID.randomUUID(),
            "items",
            List.of(
                Map.of(
                    "serviceId",
                    id(service),
                    "description",
                    "Tratamiento acordado",
                    "quantity",
                    1,
                    "sessions",
                    sessions,
                    "unitPrice",
                    price))));
  }

  void accept(JsonNode plan) throws Exception {
    plan =
        call(
            post("/api/v1/plans/" + id(plan) + "/actions/propose").with(csrf()),
            admin,
            Map.of(
                "version",
                plan.path("version").asLong(),
                "requestKey",
                UUID.randomUUID(),
                "reason",
                "Presentación del presupuesto"),
            200);
    call(
        post("/api/v1/plans/" + id(plan) + "/actions/accept").with(csrf()),
        admin,
        Map.of(
            "version",
            plan.path("version").asLong(),
            "requestKey",
            UUID.randomUUID(),
            "reason",
            "Aceptación explícita",
            "acceptedBy",
            "María Ñandú"),
        200);
  }

  JsonNode charge() throws Exception {
    return read("/api/v1/charges?patientId=" + id(patient)).path("items").get(0);
  }

  Map<String, Object> allocation(JsonNode charge, String amount) {
    return Map.of("chargeId", id(charge), "amount", amount);
  }

  JsonNode pay(String amount, String currency, List<?> allocations) throws Exception {
    return create(
        "/api/v1/finance/payments",
        Map.of(
            "requestKey",
            UUID.randomUUID(),
            "patientId",
            id(patient),
            "amount",
            amount,
            "currency",
            currency,
            "occurredOn",
            today.toString(),
            "method",
            "TRANSFER",
            "reference",
            "ABONO-PRUEBA",
            "description",
            "Pago de prueba",
            "allocations",
            allocations));
  }

  Map<String, Object> correction(String amount, List<?> releases) {
    return Map.of(
        "requestKey",
        UUID.randomUUID(),
        "amount",
        amount,
        "occurredOn",
        today.toString(),
        "reason",
        "Corrección documentada",
        "releases",
        releases);
  }

  Map<String, Object> content(String description, String item) {
    var procedure = new HashMap<String, Object>();
    procedure.put("serviceId", id(service));
    procedure.put("description", description);
    procedure.put("quantity", 1);
    procedure.put("unitPrice", "200");
    if (item != null) procedure.put("planItemId", item);
    return Map.of(
        "anamnesis",
        "",
        "evolution",
        "Evolución ficticia",
        "diagnoses",
        "Diagnóstico ficticio",
        "indications",
        "Seguimiento",
        "procedures",
        List.of(procedure));
  }

  void appointments(int count) {
    appointments(
        count,
        uuid(patient),
        "CANCELLED",
        today.atTime(9, 0).atZone(ZoneId.of("America/Lima")).toInstant());
  }

  void appointments(int count, UUID patient, String status, Instant at) {
    jdbc.update(
        "INSERT INTO"
            + " appointment(id,patient_id,dentist_id,service_id,service_name,dentist_name,duration_minutes,starts_at,ends_at,gap_minutes,blocked_until,status,origin,notes,request_key,request_fingerprint)"
            + " SELECT gen_random_uuid(),?,?,?,'Limpieza dental','Julia"
            + " Prueba',60,?::timestamptz,?::timestamptz,0,?::timestamptz,?,'MANUAL','',gen_random_uuid(),'fixture-report'"
            + " FROM generate_series(1,?)",
        patient,
        uuid(doctor),
        uuid(service),
        at.toString(),
        at.plusSeconds(3600).toString(),
        at.plusSeconds(3600).toString(),
        status,
        count);
  }

  MockHttpSession roleSession(String role) throws Exception {
    String user = "phase8-" + role.toLowerCase(Locale.ROOT);
    create(
        "/api/v1/users",
        Map.of(
            "username",
            user,
            "displayName",
            "Rol " + role,
            "password",
            password,
            "email",
            "",
            "roles",
            List.of(role),
            "active",
            true));
    return login(user);
  }

  MockHttpSession login(String user) throws Exception {
    return (MockHttpSession)
        mvc.perform(
                post("/api/v1/auth/login")
                    .with(csrf())
                    .param("username", user)
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
    var response = mvc.perform(request).andReturn();
    Throwable rootCause = response.getResolvedException();
    while (rootCause != null && rootCause.getCause() != null) rootCause = rootCause.getCause();
    assertThat(response.getResponse().getStatus())
        .as("HTTP result; cause: %s", rootCause)
        .isEqualTo(status);
    String result = response.getResponse().getContentAsString();
    return result.isBlank() ? null : mapper.readTree(result);
  }

  void moneyMetric(JsonNode page, String key, String currency, String expected) {
    JsonNode metrics = page.isArray() ? page : page.path("totals");
    for (var metric : metrics)
      if (metric.path("key").asString().equals(key)
          && (currency == null
              ? metric.path("currency").isNull()
              : currency.equals(metric.path("currency").asString()))) {
        assertMoney(metric.path("value").asString(), expected);
        return;
      }
    fail("No existe métrica " + key + "/" + currency + " en " + metrics);
  }

  void assertMoney(String actual, String expected) {
    assertThat(new BigDecimal(actual)).isEqualByComparingTo(expected);
  }
}
