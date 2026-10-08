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
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(Phase2IntegrationTests.TimeConfig.class)
class Phase2IntegrationTests {
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
      return this;
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

  @Test
  void sharedContactMinorsAndDuplicateProtection() throws Exception {
    var child =
        create("/api/v1/patients", patientBody("Hijo uno", "2020-03-01", "+51999888777", true));
    var second =
        create("/api/v1/patients", patientBody("Hijo dos", "2022-05-01", "+51999888777", true));
    assertThat(child.get("code").asText()).isNotEqualTo(second.get("code").asText());
    assertThat(read("/api/v1/patients?search=51999888777").get("totalElements").asInt())
        .isEqualTo(2);
    assertThat(
            call(
                    get("/api/v1/patients").param("phone", "+51999888777").param("size", "1"),
                    admin,
                    null,
                    200)
                .get("items")
                .size())
        .isEqualTo(1);
    call(
        post("/api/v1/patients").with(csrf()),
        admin,
        patientBody("Hijo uno", "2020-03-01", "+51999888777", true),
        409);
    call(
        post("/api/v1/patients").with(csrf()),
        admin,
        patientBody("Sin responsable", "2020-03-01", "+51999888777", false),
        400);
    var provisional =
        create("/api/v1/patients", patientBody("Pendiente", null, "+51999888777", false));
    assertThat(provisional.get("provisional").asBoolean()).isTrue();
  }

  @Test
  void documentsVersionsAndPagingAreValidated() throws Exception {
    var body = patientBody("Documento", "1990-01-01", "+51999888777", false);
    body.put("documentType", "DNI");
    body.put("documentNumber", "12345678");
    var p = create("/api/v1/patients", body);
    body.put("fullName", "Otra persona");
    call(post("/api/v1/patients").with(csrf()), admin, body, 409);
    var update = copy(p);
    update.put("active", false);
    var saved =
        call(put("/api/v1/patients/" + p.get("id").asText()).with(csrf()), admin, update, 200);
    assertThat(saved.get("active").asBoolean()).isFalse();
    call(put("/api/v1/patients/" + p.get("id").asText()).with(csrf()), admin, update, 409);
    var contactChange = copy(saved);
    contactChange.put(
        "contacts",
        List.of(
            Map.of(
                "phone",
                "+51999888777",
                "name",
                "Contacto actualizado",
                "relationship",
                "Paciente",
                "guardian",
                false,
                "payer",
                true)));
    var updatedContact =
        call(
            put("/api/v1/patients/" + p.get("id").asText()).with(csrf()),
            admin,
            contactChange,
            200);
    assertThat(updatedContact.get("version").asLong()).isGreaterThan(saved.get("version").asLong());
    call(put("/api/v1/patients/" + p.get("id").asText()).with(csrf()), admin, contactChange, 409);
    call(get("/api/v1/patients?size=101"), admin, null, 400);
    call(get("/api/v1/patients?sort=password"), admin, null, 400);
  }

  @Test
  void fullDurationOverlapPerDoctorAndDatabaseConstraint() throws Exception {
    var a = reserve(doctor, "2030-01-07T10:00");
    assertThat(a.get("localEnd").asText()).isEqualTo("2030-01-07T11:00:00");
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-07T10:30"), 409);
    reserve(otherDoctor, "2030-01-07T10:30");
    reserve(doctor, "2030-01-07T11:00");
    assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO"
                        + " appointment(id,patient_id,dentist_id,service_id,service_name,dentist_name,duration_minutes,starts_at,ends_at,gap_minutes,blocked_until,status,origin,notes,request_key,request_fingerprint)"
                        + " SELECT"
                        + " ?,patient_id,dentist_id,service_id,service_name,dentist_name,duration_minutes,starts_at+interval"
                        + " '15 minutes',ends_at+interval '15"
                        + " minutes',gap_minutes,blocked_until+interval '15"
                        + " minutes',status,origin,notes,?,request_fingerprint FROM appointment"
                        + " WHERE id=?",
                    UUID.randomUUID(),
                    UUID.randomUUID(),
                    UUID.fromString(a.get("id").asText())))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void breakAbsenceHolidayLeadGapAndEligibility() throws Exception {
    create(
        "/api/v1/schedules/periods",
        Map.of(
            "dentistId",
            doctor.get("id").asText(),
            "dayOfWeek",
            1,
            "kind",
            "BREAK",
            "startMinute",
            720,
            "endMinute",
            780,
            "active",
            true));
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-07T11:30"), 409);
    create(
        "/api/v1/schedules/exceptions",
        Map.of(
            "dentistId",
            doctor.get("id").asText(),
            "kind",
            "ABSENCE",
            "startDate",
            "2030-01-07",
            "endDate",
            "2030-01-07",
            "startMinute",
            840,
            "endMinute",
            900,
            "reason",
            "Ausencia",
            "active",
            true));
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-07T14:00"), 409);
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-07T08:30"), 409);
    jdbc.update("UPDATE installation_profile SET appointment_gap_minutes=15");
    reserve(doctor, "2030-01-07T10:00");
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-07T11:00"), 409);
    create(
        "/api/v1/schedules/exceptions",
        Map.of(
            "kind",
            "HOLIDAY",
            "startDate",
            "2030-01-14",
            "endDate",
            "2030-01-14",
            "reason",
            "Feriado",
            "active",
            true));
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-14T10:00"), 409);
    jdbc.update("UPDATE installation_profile SET minimum_lead_minutes=120");
    clock.now.set(Instant.parse("2030-01-07T13:30:00Z"));
    call(
        post("/api/v1/appointments").with(csrf()),
        admin,
        booking(otherDoctor, "2030-01-07T10:00"),
        409);
    jdbc.update(
        "DELETE FROM dentist_service WHERE dentist_id=?",
        UUID.fromString(otherDoctor.get("id").asText()));
    call(
        post("/api/v1/appointments").with(csrf()),
        admin,
        booking(otherDoctor, "2030-01-07T15:00"),
        400);
    jdbc.update("UPDATE dental_service SET active=false");
    call(
        post("/api/v1/appointments").with(csrf()), admin, booking(doctor, "2030-01-07T15:00"), 400);
  }

  @Test
  void manualServicesDoNotRequireAgentEligibility() throws Exception {
    jdbc.update("UPDATE dental_service SET bookable_by_agent=false");
    reserve(doctor, "2030-01-07T10:00");
    var b = booking(otherDoctor, "2030-01-07T10:00");
    b.put("serviceId", null);
    b.put("reason", "Entrega de documentos");
    b.put("durationMinutes", 30);
    assertThat(create("/api/v1/appointments", b).get("durationMinutes").asInt()).isEqualTo(30);
    b.put("requestKey", UUID.randomUUID().toString());
    b.put("durationMinutes", 0);
    call(post("/api/v1/appointments").with(csrf()), admin, b, 400);
  }

  @Test
  void concurrentReservationsHaveExactlyOneWinner() throws Exception {
    var pool = Executors.newFixedThreadPool(2);
    var gate = new CountDownLatch(1);
    var first = booking(doctor, "2030-01-07T10:00");
    var second = booking(doctor, "2030-01-07T10:15");
    var session2 = login(username);
    try {
      var f1 =
          pool.submit(
              () -> {
                gate.await();
                return mvc.perform(
                        post("/api/v1/appointments")
                            .session(admin)
                            .with(csrf())
                            .contentType("application/json")
                            .content(mapper.writeValueAsString(first)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      var f2 =
          pool.submit(
              () -> {
                gate.await();
                return mvc.perform(
                        post("/api/v1/appointments")
                            .session(session2)
                            .with(csrf())
                            .contentType("application/json")
                            .content(mapper.writeValueAsString(second)))
                    .andReturn()
                    .getResponse()
                    .getStatus();
              });
      gate.countDown();
      assertThat(List.of(f1.get(20, TimeUnit.SECONDS), f2.get(20, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM appointment", Integer.class))
          .isEqualTo(1);
      assertThat(jdbc.queryForObject("SELECT count(*) FROM appointment_history", Integer.class))
          .isEqualTo(1);
    } finally {
      pool.shutdownNow();
    }
  }

  @Test
  void retriesAreIdempotentAndConflictOnChangedPayload() throws Exception {
    var b = booking(doctor, "2030-01-07T10:00");
    var a = create("/api/v1/appointments", b);
    var repeated = create("/api/v1/appointments", b);
    assertThat(a.get("id")).isEqualTo(repeated.get("id"));
    assertThat(jdbc.queryForObject("select count(*) from appointment_history", Integer.class))
        .isEqualTo(1);
    b.put("notes", "Otra solicitud");
    call(post("/api/v1/appointments").with(csrf()), admin, b, 409);
  }

  @Test
  void durationSnapshotAndRejectedReschedulePreserveHistory() throws Exception {
    var a = reserve(doctor, "2030-01-07T10:00");
    reserve(doctor, "2030-01-07T13:00");
    var changed = copy(service);
    changed.put("durationMinutes", 30);
    call(put("/api/v1/services/" + service.get("id").asText()).with(csrf()), admin, changed, 200);
    assertThat(read("/api/v1/appointments/" + a.get("id").asText()).get("durationMinutes").asInt())
        .isEqualTo(60);
    var reschedule =
        new HashMap<String, Object>(
            Map.of(
                "version",
                a.get("version").asLong(),
                "dentistId",
                doctor.get("id").asText(),
                "localStart",
                "2030-01-07T13:15",
                "useCurrentDuration",
                false,
                "reason",
                "Solicitud del paciente"));
    call(
        put("/api/v1/appointments/" + a.get("id").asText() + "/reschedule").with(csrf()),
        admin,
        reschedule,
        409);
    assertThat(read("/api/v1/appointments/" + a.get("id").asText()).get("localStart"))
        .isEqualTo(a.get("localStart"));
    assertThat(
            read("/api/v1/appointments/" + a.get("id").asText() + "/history")
                .get("totalElements")
                .asInt())
        .isEqualTo(1);
    reschedule.put("localStart", "2030-01-07T15:00");
    var moved =
        call(
            put("/api/v1/appointments/" + a.get("id").asText() + "/reschedule").with(csrf()),
            admin,
            reschedule,
            200);
    assertThat(moved.get("durationMinutes").asInt()).isEqualTo(60);
    reschedule.put("version", moved.get("version").asLong());
    reschedule.put("localStart", "2030-01-07T16:00");
    reschedule.put("useCurrentDuration", true);
    assertThat(
            call(
                    put("/api/v1/appointments/" + a.get("id").asText() + "/reschedule")
                        .with(csrf()),
                    admin,
                    reschedule,
                    200)
                .get("durationMinutes")
                .asInt())
        .isEqualTo(30);
    assertThat(
            read("/api/v1/appointments/" + a.get("id").asText() + "/history")
                .get("totalElements")
                .asInt())
        .isEqualTo(3);
    assertThat(
            read("/api/v1/appointments/" + a.get("id").asText() + "/history?action=RESCHEDULED")
                .get("totalElements")
                .asInt())
        .isEqualTo(2);
    call(
        get("/api/v1/appointments/" + a.get("id").asText() + "/history?action=INVALID"),
        admin,
        null,
        400);
  }

  @Test
  void confirmationCancellationAndAttendanceKeepImmutableHistory() throws Exception {
    var a = reserve(doctor, "2030-01-07T10:00");
    String url = "/api/v1/appointments/" + a.get("id").asText() + "/status";
    var confirmed =
        call(
            put(url).with(csrf()),
            admin,
            Map.of(
                "version",
                a.get("version").asLong(),
                "status",
                "CONFIRMED",
                "reason",
                "Paciente confirmó"),
            200);
    call(
        put(url).with(csrf()),
        admin,
        Map.of(
            "version", confirmed.get("version").asLong(), "status", "WAITING", "reason", "Llegó"),
        400);
    clock.now.set(Instant.parse("2030-01-07T15:00:00Z"));
    var waiting =
        call(
            put(url).with(csrf()),
            admin,
            Map.of(
                "version",
                confirmed.get("version").asLong(),
                "status",
                "WAITING",
                "reason",
                "Llegó"),
            200);
    var progress =
        call(
            put(url).with(csrf()),
            admin,
            Map.of(
                "version",
                waiting.get("version").asLong(),
                "status",
                "IN_PROGRESS",
                "reason",
                "Inicia"),
            200);
    var attended =
        call(
            put(url).with(csrf()),
            admin,
            Map.of(
                "version",
                progress.get("version").asLong(),
                "status",
                "ATTENDED",
                "reason",
                "Terminó"),
            200);
    assertThat(attended.get("status").asText()).isEqualTo("ATTENDED");
    call(
        put(url).with(csrf()),
        admin,
        Map.of(
            "version", attended.get("version").asLong(), "status", "CANCELLED", "reason", "Cambio"),
        409);
    assertThatThrownBy(() -> jdbc.update("UPDATE appointment_history SET reason='editado'"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class)
        .hasMessageContaining("immutable");
    clock.now.set(Instant.parse("2030-01-01T12:00:00Z"));
    var cancelled = reserve(doctor, "2030-01-07T12:00");
    call(
        put("/api/v1/appointments/" + cancelled.get("id").asText() + "/status").with(csrf()),
        admin,
        Map.of(
            "version",
            cancelled.get("version").asLong(),
            "status",
            "CANCELLED",
            "reason",
            "Solicitud"),
        200);
    reserve(doctor, "2030-01-07T12:00");
  }

  @Test
  void boundedCalendarAvailabilityAndPermissions() throws Exception {
    reserve(doctor, "2030-01-07T10:00");
    var calendar = read("/api/v1/appointments/calendar?from=2030-01-07&to=2030-01-13");
    assertThat(calendar.get("items").size()).isEqualTo(1);
    call(get("/api/v1/appointments/calendar?from=2030-01-01&to=2030-03-01"), admin, null, 400);
    var slots =
        read(
            "/api/v1/appointments/availability?dentistId="
                + doctor.get("id").asText()
                + "&serviceId="
                + service.get("id").asText()
                + "&date=2030-01-07&size=5");
    assertThat(slots.get("items").size()).isEqualTo(5);
    assertThat(slots.get("totalElements").asInt()).isGreaterThan(5);
    for (var slot : slots.get("items"))
      assertThat(slot.get("localStart").asText()).isNotEqualTo("2030-01-07T10:00:00");
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
    var cashier = login("caja");
    call(
        post("/api/v1/appointments").with(csrf()),
        cashier,
        booking(otherDoctor, "2030-01-07T10:00"),
        403);
    call(
        post("/api/v1/patients").with(csrf()),
        cashier,
        patientBody("No autorizado", "1990-01-01", "+51999888777", false),
        403);
    var appointment = calendar.get("items").get(0);
    var ownSlots =
        read(
            "/api/v1/appointments/availability?dentistId="
                + doctor.get("id").asText()
                + "&durationMinutes=60&date=2030-01-07&appointmentId="
                + appointment.get("id").asText()
                + "&search=10:00");
    assertThat(ownSlots.get("totalElements").asInt()).isEqualTo(1);
    assertThat(
            read("/api/v1/appointments/availability?dentistId="
                    + doctor.get("id").asText()
                    + "&durationMinutes=60&date=2030-01-07&page=2147483647&size=100")
                .get("items")
                .size())
        .isZero();
    assertThat(read("/api/v1/appointments?size=1&search=Paciente").get("totalElements").asInt())
        .isEqualTo(1);
  }

  @Test
  void ambiguousAndNonexistentLocalTimesAreRejected() throws Exception {
    jdbc.update("UPDATE installation_profile SET time_zone='America/New_York'");
    var b = booking(doctor, "2030-03-10T02:30");
    call(post("/api/v1/appointments").with(csrf()), admin, b, 400);
    b.put("requestKey", UUID.randomUUID().toString());
    b.put("localStart", "2030-11-03T01:30");
    call(post("/api/v1/appointments").with(csrf()), admin, b, 400);
  }
}
