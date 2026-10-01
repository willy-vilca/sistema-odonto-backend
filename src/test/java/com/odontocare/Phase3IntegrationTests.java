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
@Import(Phase3IntegrationTests.TimeConfig.class)
class Phase3IntegrationTests {
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
        "TRUNCATE"
            + " charge_entry,treatment_session,treatment_operation,treatment_item,treatment_plan,document_consent,document_content,patient_document,document_category,encounter_revision,clinical_encounter,clinical_state,clinical_template,appointment_history,appointment,patient_contact,patient,installation_logo,audit_event,user_role,dentist_service,weekly_period,schedule_exception,dentist,dental_service,service_category,user_account");
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

  @Test
  void draftsFinalizationCorrectionsPreserveOriginalAndSnapshot() throws Exception {
    var draft = encounter();
    String url = "/api/v1/clinical/encounters/" + draft.get("id").asText();
    var original =
        call(
            post(url + "/finalize").with(csrf()),
            admin,
            Map.of("version", draft.get("version").asLong()),
            200);
    var correction =
        call(
            post(url + "/corrections").with(csrf()),
            admin,
            Map.of(
                "version",
                original.get("version").asLong(),
                "correctionReason",
                "Precisión de evolución",
                "content",
                content("Corrección")),
            200);
    assertThat(correction.get("revision").asInt()).isEqualTo(2);
    var history = read(url + "/versions?sort=number");
    assertThat(history.get("items").get(0).get("content").get("evolution").asText())
        .isEqualTo("Original");
    assertThat(history.get("items").get(1).get("content").get("evolution").asText())
        .isEqualTo("Corrección");
    assertThat(history.get("items").get(0).get("procedures").get(0).get("unitPrice").decimalValue())
        .isEqualByComparingTo("100");
    var update = copy(patient);
    update.put("fullName", "Nombre modificado");
    call(put("/api/v1/patients/" + patient.get("id").asText()).with(csrf()), admin, update, 200);
    assertThat(read(url + "/versions").get("items").get(0).get("patientName").asText())
        .isEqualTo("Paciente adulto");
    call(put(url).with(csrf()), admin, encounterBody(), 409);
    call(
        post(url + "/corrections").with(csrf()),
        admin,
        Map.of(
            "version",
            original.get("version").asLong(),
            "correctionReason",
            "Vieja",
            "content",
            content("Otra")),
        409);
    assertThatThrownBy(() -> jdbc.update("UPDATE encounter_revision SET reason='borrado'"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    assertThatThrownBy(
            () -> jdbc.update("UPDATE clinical_encounter SET draft='{}' WHERE status='FINAL'"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
  }

  @Test
  void clinicalStatesAreImmutableValidateTeethAndRejectLostUpdate() throws Exception {
    String path = "/api/v1/clinical/states/ODONTOGRAM";
    var body = new HashMap<String, Object>();
    body.put("patientId", patient.get("id").asText());
    body.put("dentistId", doctor.get("id").asText());
    body.put("recordedOn", "2029-12-30");
    body.put("reason", "Evaluación inicial");
    body.put(
        "odontogram",
        Map.of(
            "notes",
            "",
            "marks",
            List.of(
                Map.of(
                    "tooth", 11, "surface", "O", "finding", "CARIES", "note", "Hallazgo informado"),
                Map.of(
                    "tooth",
                    51,
                    "surface",
                    "V",
                    "finding",
                    "RESTORATION",
                    "note",
                    "Dentición temporal"))));
    var first = create(path, body);
    body.put("previousId", first.get("id").asText());
    body.put("reason", "Control posterior");
    body.put("recordedOn", "2029-12-31");
    body.put(
        "odontogram",
        Map.of(
            "notes",
            "Control",
            "marks",
            List.of(
                Map.of(
                    "tooth", 11, "surface", "O", "finding", "RESTORATION", "note", "Realizado"))));
    create(path, body);
    call(post(path).with(csrf()), admin, body, 409);
    var states =
        read(
            path
                + "?patientId="
                + patient.get("id").asText()
                + "&size=1&sort=createdAt&direction=desc");
    assertThat(states.get("totalElements").asInt()).isEqualTo(2);
    assertThat(states.get("items").size()).isEqualTo(1);
    assertThatThrownBy(() -> jdbc.update("DELETE FROM clinical_state"))
        .isInstanceOf(org.springframework.dao.DataAccessException.class);
    body.put(
        "previousId",
        read(path + "?patientId=" + patient.get("id").asText() + "&sort=createdAt&direction=desc")
            .get("items")
            .get(0)
            .get("id")
            .asText());
    body.put(
        "odontogram",
        Map.of(
            "notes",
            "",
            "marks",
            List.of(Map.of("tooth", 19, "surface", "O", "finding", "CARIES", "note", ""))));
    call(post(path).with(csrf()), admin, body, 400);
    body.remove("odontogram");
    body.remove("previousId");
    body.put(
        "background",
        Map.of(
            "antecedents",
            "Antecedente",
            "allergies",
            "Alergia",
            "medications",
            "Medicamento",
            "anamnesis",
            "Anamnesis"));
    create("/api/v1/clinical/states/BACKGROUND", body);
  }

  @Test
  void originalsForPngJpegWebpPdfRemainInPostgresqlAndListsExcludeBytes() throws Exception {
    var category = category();
    byte[] webp =
        Base64.getDecoder().decode("UklGRiIAAABXRUJQVlA4IBYAAAAwAQCdASoBAAEADsD+JaQAA3AAAAAA");
    for (var input :
        List.of(
            Map.entry("foto.png", image("png")),
            Map.entry("foto.jpg", image("jpeg")),
            Map.entry("estudio.pdf", pdf()),
            Map.entry("foto.webp", webp))) {
      var uploaded = upload(input.getValue(), input.getKey(), category, 201);
      String id = uploaded.get("id").asText();
      var response =
          mvc.perform(get("/api/v1/documents/" + id + "/content").session(admin))
              .andExpect(status().isOk())
              .andExpect(header().string("Cache-Control", "no-store"))
              .andReturn()
              .getResponse();
      assertThat(response.getContentAsByteArray()).isEqualTo(input.getValue());
      assertThat(
              jdbc.queryForObject(
                  "select octet_length(content) from document_content where id=?",
                  Integer.class,
                  UUID.fromString(id)))
          .isEqualTo(input.getValue().length);
    }
    var list =
        read(
            "/api/v1/documents?patientId="
                + patient.get("id").asText()
                + "&size=2&sort=recordedOn");
    assertThat(list.get("items").size()).isEqualTo(2);
    assertThat(list.toString()).doesNotContain("content", "bytes", "base64");
    assertThat(
            jdbc.queryForObject(
                "select count(*) from audit_event where action='DOCUMENT_VIEWED'", Integer.class))
        .isEqualTo(4);
    var id =
        read("/api/v1/documents?patientId=" + patient.get("id").asText())
            .get("items")
            .get(0)
            .get("id")
            .asText();
    mvc.perform(get("/api/v1/documents/" + id + "/content?download=true").session(admin))
        .andExpect(status().isOk())
        .andExpect(
            header().string("Content-Disposition", org.hamcrest.Matchers.startsWith("attachment")));
  }

  @Test
  void formatsSizesActivePdfAndCategoryPolicyAreValidated() throws Exception {
    var category = category();
    upload("<html>bad</html>".getBytes(), "falso.pdf", category, 400);
    upload(image("png"), "archivo.jpg", category, 400);
    upload(image("png"), "archivo.svg", category, 400);
    upload(new byte[0], "vacio.png", category, 400);
    var policy = read("/api/v1/documents/policy");
    call(
        put("/api/v1/documents/policy").with(csrf()),
        admin,
        Map.of("maxFileMiB", 1, "version", policy.get("version").asLong()),
        200);
    upload(new byte[1048577], "grande.png", category, 413);
    call(
        put("/api/v1/documents/policy").with(csrf()),
        admin,
        Map.of("maxFileMiB", 61, "version", 0),
        400);
    try (var pdf = new org.apache.pdfbox.pdmodel.PDDocument();
        var output = new java.io.ByteArrayOutputStream()) {
      pdf.addPage(new org.apache.pdfbox.pdmodel.PDPage());
      pdf.getDocumentCatalog()
          .setOpenAction(
              new org.apache.pdfbox.pdmodel.interactive.action.PDActionJavaScript("alert(1)"));
      pdf.save(output);
      upload(output.toByteArray(), "activo.pdf", category, 400);
    }
    assertThat(jdbc.queryForObject("select count(*) from patient_document", Integer.class))
        .isZero();
  }

  @Test
  void clinicalAndDocumentPermissionsDenyReceptionCashierAndOtherDentistWrites() throws Exception {
    var uploaded = upload(image("png"), "foto.png", category(), 201);
    for (String role : List.of("RECEPTION", "CASHIER")) {
      String user = role.toLowerCase();
      create(
          "/api/v1/users",
          Map.of(
              "username",
              user,
              "displayName",
              user,
              "email",
              "",
              "password",
              password,
              "active",
              true,
              "roles",
              List.of(role)));
      var session = login(user);
      call(
          get("/api/v1/clinical/encounters?patientId=" + patient.get("id").asText()),
          session,
          null,
          403);
      call(get("/api/v1/documents?patientId=" + patient.get("id").asText()), session, null, 403);
      call(
          get("/api/v1/documents/" + uploaded.get("id").asText() + "/content"), session, null, 403);
      call(post("/api/v1/clinical/encounters").with(csrf()), session, encounterBody(), 403);
    }
    var own = login("ana");
    call(post("/api/v1/clinical/encounters").with(csrf()), own, encounterBody(), 201);
    call(post("/api/v1/clinical/encounters").with(csrf()), login("luis"), encounterBody(), 403);
    call(get("/api/v1/documents/" + uploaded.get("id").asText() + "/content"), null, null, 401);
  }

  @Test
  void consentLinksSamePatientAndCategoryDeactivationPreservesOriginal() throws Exception {
    var category = category();
    var doc = upload(pdf(), "consentimiento.pdf", category, 201);
    var consent =
        new HashMap<String, Object>(
            Map.of(
                "patientId",
                patient.get("id").asText(),
                "documentId",
                doc.get("id").asText(),
                "name",
                "Consentimiento",
                "responsible",
                "Paciente adulto",
                "relationship",
                "Paciente",
                "signedOn",
                "2029-12-31"));
    create("/api/v1/documents/consents", consent);
    var other =
        create(
            "/api/v1/patients", patientBody("Otro paciente", "1991-01-01", "+51922223333", false));
    consent.put("patientId", other.get("id").asText());
    call(post("/api/v1/documents/consents").with(csrf()), admin, consent, 400);
    call(
        put("/api/v1/documents/categories/" + category.get("id").asText()).with(csrf()),
        admin,
        Map.of("name", "Fotos", "active", false, "version", category.get("version").asLong()),
        200);
    mvc.perform(get("/api/v1/documents/" + doc.get("id").asText() + "/content").session(admin))
        .andExpect(status().isOk());
    assertThat(
            read("/api/v1/documents/consents?patientId=" + patient.get("id").asText())
                .get("totalElements")
                .asInt())
        .isEqualTo(1);
  }

  @Test
  void clinicalDateValidationTemplatesAndLinkedAppointmentCompletion() throws Exception {
    var appointment = reserve(doctor, "2030-01-07T10:00");
    var body = new HashMap<>(encounterBody());
    body.put("appointmentId", appointment.get("id").asText());
    var encounter = create("/api/v1/clinical/encounters", body);
    String url = "/api/v1/clinical/encounters/" + encounter.get("id").asText();
    call(
        post(url + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.get("version").asLong()),
        400);
    assertThat(read(url).get("status").asText()).isEqualTo("DRAFT");
    clock.now.set(Instant.parse("2030-01-07T16:00:00Z"));
    call(
        post(url + "/finalize").with(csrf()),
        admin,
        Map.of("version", encounter.get("version").asLong()),
        200);
    assertThat(
            read("/api/v1/appointments/" + appointment.get("id").asText()).get("status").asText())
        .isEqualTo("ATTENDED");
    var invalid = new HashMap<>(encounterBody());
    invalid.put("attendedOn", "2031-01-01");
    call(post("/api/v1/clinical/encounters").with(csrf()), admin, invalid, 400);
    create(
        "/api/v1/clinical/templates",
        Map.of(
            "name",
            "Evaluación",
            "kind",
            "ENCOUNTER",
            "content",
            "Texto configurable",
            "active",
            true,
            "version",
            0));
    assertThat(
            read("/api/v1/clinical/templates?kind=ENCOUNTER&search=Texto")
                .get("totalElements")
                .asInt())
        .isEqualTo(1);
    call(get("/api/v1/clinical/templates?size=101"), admin, null, 400);
    call(
        get("/api/v1/documents?patientId=" + patient.get("id").asText() + "&sort=content"),
        admin,
        null,
        400);
  }

  @Test
  void concurrentClinicalStateWritesHaveOneWinner() throws Exception {
    var body =
        Map.of(
            "patientId",
            patient.get("id").asText(),
            "dentistId",
            doctor.get("id").asText(),
            "recordedOn",
            "2029-12-31",
            "reason",
            "Registro simultáneo",
            "background",
            Map.of(
                "antecedents", "", "allergies", "", "medications", "", "anamnesis", "Entrevista"));
    byte[] payload = mapper.writeValueAsBytes(body);
    var second = login(username);
    var gate = new CountDownLatch(1);
    var ready = new CountDownLatch(2);
    var pool = Executors.newFixedThreadPool(2);
    try {
      List<Future<Integer>> results = new ArrayList<>();
      for (var session : List.of(admin, second))
        results.add(
            pool.submit(
                () -> {
                  ready.countDown();
                  gate.await();
                  return mvc.perform(
                          post("/api/v1/clinical/states/BACKGROUND")
                              .session(session)
                              .with(csrf())
                              .contentType("application/json")
                              .content(payload))
                      .andReturn()
                      .getResponse()
                      .getStatus();
                }));
      assertThat(ready.await(5, TimeUnit.SECONDS)).isTrue();
      gate.countDown();
      assertThat(
              List.of(
                  results.get(0).get(10, TimeUnit.SECONDS),
                  results.get(1).get(10, TimeUnit.SECONDS)))
          .containsExactlyInAnyOrder(201, 409);
      assertThat(jdbc.queryForObject("select count(*) from clinical_state", Integer.class))
          .isEqualTo(1);
    } finally {
      gate.countDown();
      pool.shutdownNow();
    }
  }

  @Test
  void pdfPreviewIsAuthorizedPagedAndDoesNotReplaceOriginal() throws Exception {
    byte[] original = pdf();
    var file = upload(original, "documento.pdf", category(), 201);
    String id = file.get("id").asText();
    var response =
        mvc.perform(get("/api/v1/documents/" + id + "/preview?page=0").session(admin))
            .andExpect(status().isOk())
            .andExpect(header().string("X-Document-Pages", "1"))
            .andExpect(
                org.springframework.test.web.servlet.result.MockMvcResultMatchers.content()
                    .contentType("image/png"))
            .andReturn()
            .getResponse();
    assertThat(
            javax.imageio.ImageIO.read(
                new java.io.ByteArrayInputStream(response.getContentAsByteArray())))
        .isNotNull();
    mvc.perform(get("/api/v1/documents/" + id + "/preview?page=1").session(admin))
        .andExpect(status().isBadRequest());
    mvc.perform(get("/api/v1/documents/" + id + "/preview").session(new MockHttpSession()))
        .andExpect(status().isUnauthorized());
    assertThat(
            jdbc.queryForObject(
                "select content from document_content where id=?",
                byte[].class,
                UUID.fromString(id)))
        .isEqualTo(original);
  }
}
