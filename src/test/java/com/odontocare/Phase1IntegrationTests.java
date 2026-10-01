package com.odontocare;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.odontocare.security.model.Permission;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.util.*;
import java.util.concurrent.*;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.*;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class Phase1IntegrationTests {
  @Autowired MockMvc mvc;
  @Autowired ObjectMapper mapper;
  @Autowired JdbcTemplate jdbc;
  @Autowired PasswordEncoder encoder;
  private String password;
  private String adminUsername;
  private MockHttpSession admin;

  @BeforeEach
  void prepareIsolatedDatabase() throws Exception {
    assertThat(jdbc.queryForObject("select current_database()", String.class))
        .isEqualTo("sistema_odontologo_test");
    jdbc.execute(
        "TRUNCATE"
            + " document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
    jdbc.update(
        "UPDATE installation_profile SET display_name='Mi"
            + " consultorio',time_zone='America/Lima',currency='PEN',patient_next_number=1,receipt_next_number=1,budget_next_number=1,version=0,logo_revision=0");
    jdbc.update("UPDATE role_definition SET name=code,version=0");
    jdbc.update("DELETE FROM role_permission");
    for (Permission permission : Permission.values())
      jdbc.update(
          "INSERT INTO role_permission(role_code,permission) VALUES ('ADMIN',?)",
          permission.name());
    for (String code : List.of("DENTIST", "RECEPTION", "CASHIER")) {
      for (String permission :
          List.of("SETTINGS_READ", "DENTISTS_READ", "SERVICES_READ", "SCHEDULES_READ")) {
        jdbc.update(
            "INSERT INTO role_permission(role_code,permission) VALUES (?,?)", code, permission);
      }
    }
    password = "Test-" + UUID.randomUUID();
    adminUsername = "admin" + UUID.randomUUID().toString().substring(0, 8);
    var setup = new HashMap<String, Object>();
    setup.put("username", adminUsername);
    setup.put("displayName", "Administrador de prueba");
    setup.put("password", password);
    perform(post("/api/v1/auth/setup").with(csrf()), null, setup, 201);
    admin = login(adminUsername, password);
  }

  private MockHttpSession login(String username, String secret) throws Exception {
    var session = new MockHttpSession();
    String originalId = session.getId();
    var result =
        mvc.perform(
                post("/api/v1/auth/login")
                    .session(session)
                    .with(csrf())
                    .param("username", username)
                    .param("password", secret))
            .andExpect(status().isOk())
            .andReturn();
    var authenticated = (MockHttpSession) result.getRequest().getSession(false);
    assertThat(authenticated.getId()).isNotEqualTo(originalId);
    return authenticated;
  }

  private JsonNode perform(
      MockHttpServletRequestBuilder request, MockHttpSession session, Object body, int status)
      throws Exception {
    if (session != null) request.session(session);
    if (body != null)
      request.contentType("application/json").content(mapper.writeValueAsString(body));
    var result = mvc.perform(request).andExpect(status().is(status)).andReturn().getResponse();
    return result.getContentAsString().isBlank()
        ? null
        : mapper.readTree(result.getContentAsString());
  }

  private JsonNode get(String path) throws Exception {
    return perform(getRequest(path), admin, null, 200);
  }

  private MockHttpServletRequestBuilder getRequest(String path) {
    return org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get(path);
  }

  private JsonNode create(String path, Object body) throws Exception {
    return perform(post(path).with(csrf()), admin, body, 201);
  }

  private Map<String, Object> user(String username, String role) {
    var payload = new HashMap<String, Object>();
    payload.put("username", username);
    payload.put("displayName", username);
    payload.put("email", "");
    payload.put("password", password);
    payload.put("roles", List.of(role));
    payload.put("active", true);
    return payload;
  }

  private Map<String, Object> copy(JsonNode node) {
    return mapper.convertValue(
        node, new tools.jackson.core.type.TypeReference<Map<String, Object>>() {});
  }

  private JsonNode category() throws Exception {
    return create("/api/v1/categories", Map.of("name", "Evaluación", "active", true));
  }

  private Map<String, Object> serviceBody(String categoryId, String name) {
    return new HashMap<>(
        Map.of(
            "name",
            name,
            "categoryId",
            categoryId,
            "price",
            "150.25",
            "durationMinutes",
            60,
            "description",
            "Evaluación inicial",
            "bookableByAgent",
            true,
            "active",
            true));
  }

  private JsonNode service(String categoryId, String name) throws Exception {
    return create("/api/v1/services", serviceBody(categoryId, name));
  }

  private JsonNode dentist(String suffix, String serviceId) throws Exception {
    var account = create("/api/v1/users", user("doctor" + suffix, "DENTIST"));
    return create(
        "/api/v1/dentists",
        Map.of(
            "userId",
            account.get("id").asString(),
            "fullName",
            "Doctora " + suffix,
            "licenseNumber",
            "COP-" + suffix,
            "specialty",
            "General",
            "active",
            true,
            "serviceIds",
            List.of(serviceId)));
  }

  private Map<String, Object> period(String dentistId, int start, int end, String kind) {
    return new HashMap<>(
        Map.of(
            "dentistId",
            dentistId,
            "dayOfWeek",
            1,
            "kind",
            kind,
            "startMinute",
            start,
            "endMinute",
            end,
            "active",
            true));
  }

  @Test
  void authenticationCsrfLogoutAndPasswordsAreProtected() throws Exception {
    var session = get("/api/v1/auth/session");
    assertThat(session.get("user").get("username").asString()).isEqualTo(adminUsername);
    String stored =
        jdbc.queryForObject(
            "select password_hash from user_account where username=?", String.class, adminUsername);
    assertThat(stored).isNotEqualTo(password);
    assertThat(encoder.matches(password, stored)).isTrue();
    var settings = get("/api/v1/settings");
    perform(put("/api/v1/settings"), admin, copy(settings), 403);
    perform(post("/api/v1/auth/logout").with(csrf()), admin, null, 204);
    perform(getRequest("/api/v1/settings"), null, null, 401);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_event where action='LOGOUT'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void setupCannotReplaceAdministratorAndPasswordBytesAreValidated() throws Exception {
    perform(
        post("/api/v1/auth/setup").with(csrf()),
        null,
        Map.of("username", "another", "displayName", "Otro", "password", password),
        409);
    var payload = user("shortpassword", "RECEPTION");
    payload.put("password", "123");
    perform(post("/api/v1/users").with(csrf()), admin, payload, 400);
    payload.put("password", "á".repeat(37));
    perform(post("/api/v1/users").with(csrf()), admin, payload, 400);
    assertThat(jdbc.queryForObject("select count(*) from user_account", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void rolePermissionsAreEnforcedAndRevokedOnAnExistingSession() throws Exception {
    create("/api/v1/users", user("reception", "RECEPTION"));
    var receptionist = login("reception", password);
    perform(getRequest("/api/v1/services"), receptionist, null, 200);
    perform(
        post("/api/v1/categories").with(csrf()),
        receptionist,
        Map.of("name", "No permitido", "active", true),
        403);
    perform(getRequest("/api/v1/users"), receptionist, null, 403);
    perform(getRequest("/api/v1/audit-events"), receptionist, null, 403);
    var roles = get("/api/v1/roles");
    JsonNode role = null;
    for (JsonNode item : roles.get("items"))
      if (item.get("code").asString().equals("RECEPTION")) role = item;
    perform(
        put("/api/v1/roles/RECEPTION").with(csrf()),
        admin,
        Map.of(
            "name",
            "Recepción",
            "permissions",
            List.of("SETTINGS_READ"),
            "version",
            role.get("version").asLong()),
        200);
    perform(getRequest("/api/v1/services"), receptionist, null, 403);
  }

  @Test
  void lastAdministratorAndSelfDeactivationAreRejected() throws Exception {
    var account = get("/api/v1/users?search=" + adminUsername).get("items").get(0);
    var payload = copy(account);
    payload.put("password", "");
    payload.put("roles", List.of("RECEPTION"));
    perform(put("/api/v1/users/" + account.get("id").asString()).with(csrf()), admin, payload, 409);
    payload.put("roles", List.of("ADMIN"));
    payload.put("active", false);
    perform(put("/api/v1/users/" + account.get("id").asString()).with(csrf()), admin, payload, 400);
    assertThat(jdbc.queryForObject("select count(*) from user_account where active", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void deactivatingAccountRevokesAnExistingSession() throws Exception {
    var account = create("/api/v1/users", user("cashier", "CASHIER"));
    var cashier = login("cashier", password);
    var payload = copy(account);
    payload.put("password", "");
    payload.put("active", false);
    perform(put("/api/v1/users/" + account.get("id").asString()).with(csrf()), admin, payload, 200);
    perform(getRequest("/api/v1/services"), cashier, null, 401);
    mvc.perform(
            post("/api/v1/auth/login")
                .with(csrf())
                .param("username", "cashier")
                .param("password", password))
        .andExpect(status().isUnauthorized());
  }

  @Test
  void configurationChangesArePersistedAndRejectStaleEditsAndInvalidValues() throws Exception {
    var current = get("/api/v1/settings");
    var payload = copy(current);
    payload.put("displayName", "Consultorio configurable");
    payload.put("minimumLeadMinutes", 180);
    payload.put("appointmentGapMinutes", 10);
    payload.put("currency", "USD");
    payload.put("timeZone", "America/Bogota");
    payload.put("patientNextNumber", 100);
    var saved = perform(put("/api/v1/settings").with(csrf()), admin, payload, 200);
    assertThat(get("/api/v1/system/installation").get("displayName").asString())
        .isEqualTo("Consultorio configurable");
    perform(put("/api/v1/settings").with(csrf()), admin, payload, 409);
    payload = copy(saved);
    payload.put("timeZone", "Not/AZone");
    perform(put("/api/v1/settings").with(csrf()), admin, payload, 400);
    payload.put("timeZone", "America/Lima");
    payload.put("patientNextNumber", 1);
    perform(put("/api/v1/settings").with(csrf()), admin, payload, 400);
  }

  @Test
  void realLogoIsStoredInByteaAndUnsupportedFilesAreRejected() throws Exception {
    var settings = get("/api/v1/settings");
    var output = new ByteArrayOutputStream();
    ImageIO.write(new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB), "png", output);
    byte[] image = output.toByteArray();
    mvc.perform(
            multipart("/api/v1/settings/logo")
                .file(new MockMultipartFile("file", "logo.png", "image/png", image))
                .param("version", settings.get("version").asString())
                .session(admin)
                .with(csrf()))
        .andExpect(status().isNoContent());
    var download =
        mvc.perform(getRequest("/api/v1/system/logo"))
            .andExpect(status().isOk())
            .andExpect(content().contentType("image/png"))
            .andReturn()
            .getResponse();
    assertThat(download.getContentAsByteArray()).isEqualTo(image);
    assertThat(
            jdbc.queryForObject(
                "select octet_length(content) from installation_logo", Integer.class))
        .isEqualTo(image.length);
    var fresh = get("/api/v1/settings");
    mvc.perform(
            multipart("/api/v1/settings/logo")
                .file(new MockMultipartFile("file", "logo.svg", "image/png", "<svg/>".getBytes()))
                .param("version", fresh.get("version").asString())
                .session(admin)
                .with(csrf()))
        .andExpect(status().isBadRequest());
    assertThat(get("/api/v1/system/installation").toString())
        .doesNotContain("contentType", "content");
  }

  @Test
  void serverPaginationFiltersAndSortAllowlistAreApplied() throws Exception {
    var category = category();
    for (int index = 0; index < 5; index++)
      service(category.get("id").asString(), "Servicio " + index);
    var page = get("/api/v1/services?size=2&page=1&search=Servicio&active=true");
    assertThat(page.get("items").size()).isEqualTo(2);
    assertThat(page.get("totalElements").asLong()).isEqualTo(5);
    assertThat(page.get("totalPages").asInt()).isEqualTo(3);
    perform(getRequest("/api/v1/services?size=101"), admin, null, 400);
    perform(getRequest("/api/v1/services?sort=passwordHash"), admin, null, 400);
    assertThat(get("/api/v1/services?search=%25").get("totalElements").asLong()).isZero();
  }

  @Test
  void invalidServicePriceDurationAndDuplicatesDoNotCreateRowsOrAuditEvents() throws Exception {
    var category = category();
    var payload = serviceBody(category.get("id").asString(), "Servicio inválido");
    payload.put("price", "-1");
    perform(post("/api/v1/services").with(csrf()), admin, payload, 400);
    payload.put("price", "10.123");
    perform(post("/api/v1/services").with(csrf()), admin, payload, 400);
    payload.put("price", "10.00");
    payload.put("durationMinutes", 0);
    perform(post("/api/v1/services").with(csrf()), admin, payload, 400);
    assertThat(jdbc.queryForObject("select count(*) from dental_service", Integer.class)).isZero();
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_event where action='SERVICE_CREATED'", Integer.class))
        .isZero();
    payload.put("durationMinutes", 30);
    create("/api/v1/services", payload);
    payload.put("name", "servicio inválido");
    perform(post("/api/v1/services").with(csrf()), admin, payload, 409);
  }

  @Test
  void twoDentistsHaveDistinctServicesAndDeactivationKeepsRelationsAndSchedules() throws Exception {
    var category = category();
    var firstService = service(category.get("id").asString(), "Evaluación");
    var secondService = service(category.get("id").asString(), "Limpieza");
    var first = dentist("one", firstService.get("id").asString());
    var second = dentist("two", secondService.get("id").asString());
    assertThat(first.get("services").has(firstService.get("id").asString())).isTrue();
    assertThat(second.get("services").has(firstService.get("id").asString())).isFalse();
    create("/api/v1/schedules/periods", period(first.get("id").asString(), 480, 1020, "WORK"));
    var servicePayload = copy(firstService);
    servicePayload.put("active", false);
    perform(
        put("/api/v1/services/" + firstService.get("id").asString()).with(csrf()),
        admin,
        servicePayload,
        200);
    var dentistPayload = copy(first);
    dentistPayload.put("active", false);
    dentistPayload.put("serviceIds", List.of(firstService.get("id").asString()));
    perform(
        put("/api/v1/dentists/" + first.get("id").asString()).with(csrf()),
        admin,
        dentistPayload,
        200);
    assertThat(jdbc.queryForObject("select count(*) from dentist_service", Integer.class))
        .isEqualTo(2);
    assertThat(jdbc.queryForObject("select count(*) from weekly_period", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void workAndBreaksRejectOverlapsAndOrphanBreaks() throws Exception {
    var category = category();
    var service = service(category.get("id").asString(), "Evaluación");
    var doctor = dentist("hours", service.get("id").asString());
    String id = doctor.get("id").asString();
    var work = create("/api/v1/schedules/periods", period(id, 480, 1080, "WORK"));
    create("/api/v1/schedules/periods", period(id, 720, 780, "BREAK"));
    perform(
        post("/api/v1/schedules/periods").with(csrf()), admin, period(id, 1000, 1100, "WORK"), 409);
    perform(
        post("/api/v1/schedules/periods").with(csrf()),
        admin,
        period(id, 1100, 1120, "BREAK"),
        409);
    var disabled = copy(work);
    disabled.put("active", false);
    perform(
        put("/api/v1/schedules/periods/" + work.get("id").asString()).with(csrf()),
        admin,
        disabled,
        409);
    create("/api/v1/schedules/periods", period(id, 1080, 1140, "WORK"));
    assertThat(
            jdbc.queryForObject("select count(*) from weekly_period where active", Integer.class))
        .isEqualTo(3);
  }

  @Test
  void holidaysAndPartialAbsencesValidateDatesAndKeepAudit() throws Exception {
    var category = category();
    var service = service(category.get("id").asString(), "Evaluación");
    var doctor = dentist("absence", service.get("id").asString());
    create(
        "/api/v1/schedules/exceptions",
        Map.of(
            "kind",
            "HOLIDAY",
            "startDate",
            "2026-12-25",
            "endDate",
            "2026-12-25",
            "reason",
            "Navidad",
            "active",
            true));
    var absence = new HashMap<String, Object>();
    absence.put("kind", "ABSENCE");
    absence.put("dentistId", doctor.get("id").asString());
    absence.put("startDate", "2026-10-05");
    absence.put("endDate", "2026-10-05");
    absence.put("startMinute", 540);
    absence.put("endMinute", 600);
    absence.put("reason", "Ausencia parcial");
    absence.put("active", true);
    create("/api/v1/schedules/exceptions", absence);
    absence.put("endDate", "2026-10-06");
    perform(post("/api/v1/schedules/exceptions").with(csrf()), admin, absence, 400);
    var events = get("/api/v1/audit-events?entityType=EXCEPTION&search=Ausencia");
    assertThat(events.get("totalElements").asLong()).isEqualTo(1);
    assertThat(events.toString()).doesNotContain(password, "passwordHash");
    Assertions.assertThrows(
        org.springframework.dao.DataAccessException.class,
        () -> jdbc.update("UPDATE audit_event SET summary='changed'"));
  }

  @Test
  void concurrentOverlappingSchedulesHaveOnlyOneWinner() throws Exception {
    var doctor =
        dentist(
            "concurrent",
            service(category().get("id").asString(), "Evaluación concurrente")
                .get("id")
                .asString());
    String doctorId = doctor.get("id").asString();
    var ready = new CountDownLatch(2);
    var start = new CountDownLatch(1);
    try (var executor = Executors.newFixedThreadPool(2)) {
      var futures = new ArrayList<Future<Integer>>();
      for (int i = 0; i < 2; i++)
        futures.add(
            executor.submit(
                () -> {
                  ready.countDown();
                  start.await();
                  return mvc.perform(
                          post("/api/v1/schedules/periods")
                              .session(admin)
                              .with(csrf())
                              .contentType("application/json")
                              .content(
                                  mapper.writeValueAsString(period(doctorId, 540, 720, "WORK"))))
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
          .containsExactlyInAnyOrder(201, 409);
    }
    assertThat(
            jdbc.queryForObject("select count(*) from weekly_period where active", Integer.class))
        .isEqualTo(1);
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_event where action='PERIOD_CREATED'", Integer.class))
        .isEqualTo(1);
  }

  @Test
  void eligibleUserPickerIsPagedMinimalAndProtected() throws Exception {
    create("/api/v1/users", user("doctorone", "DENTIST"));
    create("/api/v1/users", user("doctortwo", "DENTIST"));
    create("/api/v1/users", user("receptiononly", "RECEPTION"));
    var page = get("/api/v1/dentists/eligible-users?page=1&size=1");
    assertThat(page.get("items").size()).isEqualTo(1);
    assertThat(page.get("totalElements").asLong()).isEqualTo(2);
    assertThat(page.toString()).doesNotContain("password", "email", "roles", "username");
    var receptionist = login("receptiononly", password);
    perform(getRequest("/api/v1/dentists/eligible-users"), receptionist, null, 403);
  }

  @Test
  void invalidPermissionPolicyAndUnreadableBrandAreRejected() throws Exception {
    var roles = get("/api/v1/roles");
    JsonNode reception = null;
    for (JsonNode item : roles.get("items"))
      if (item.get("code").asString().equals("RECEPTION")) reception = item;
    perform(
        put("/api/v1/roles/RECEPTION").with(csrf()),
        admin,
        Map.of(
            "name",
            "Recepción",
            "permissions",
            List.of("SERVICES_WRITE"),
            "version",
            reception.get("version").asLong()),
        400);
    var current = get("/api/v1/settings");
    var settings = copy(current);
    settings.put("brandColor", "#ffffff");
    perform(put("/api/v1/settings").with(csrf()), admin, settings, 400);
    assertThat(get("/api/v1/settings").get("brandColor")).isEqualTo(current.get("brandColor"));
    perform(delete("/api/v1/services/" + UUID.randomUUID()).with(csrf()), admin, null, 405);
  }

  @Test
  void databaseRejectsIncompletePartialDayBlock() {
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () ->
                jdbc.update(
                    "INSERT INTO"
                        + " schedule_exception(id,kind,start_date,end_date,start_minute,reason)"
                        + " VALUES (?,'HOLIDAY','2026-12-01','2026-12-01',540,'Prueba de"
                        + " integridad')",
                    UUID.randomUUID()))
        .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    assertThat(jdbc.queryForObject("select count(*) from schedule_exception", Integer.class))
        .isZero();
  }
}
