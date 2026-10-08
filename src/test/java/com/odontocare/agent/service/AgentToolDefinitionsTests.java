package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;

class AgentToolDefinitionsTests {
  private final AgentToolDefinitions definitions = new AgentToolDefinitions();

  @Test
  void changeProposalRequiresVerifiedIdentityAppointmentAndActualSlots() {
    var evidence = new ArrayList<Map<String, Object>>();
    assertThat(names(evidence))
        .contains("verificar_paciente", "consultar_servicios", "derivar_recepcion")
        .doesNotContain(
            "consultar_mis_citas",
            "proponer_reprogramacion",
            "proponer_cancelacion",
            "proponer_cita",
            "consultar_horarios");
    evidence.add(Map.of("name", "verificar_paciente", "result", Map.of("verified", true)));
    assertThat(names(evidence))
        .contains("consultar_mis_citas")
        .doesNotContain("proponer_reprogramacion", "verificar_paciente");
    evidence.add(
        Map.of(
            "name",
            "consultar_mis_citas",
            "result",
            Map.of("items", List.of(Map.of("appointment_ref", "own")))));
    assertThat(names(evidence))
        .contains("consultar_horarios", "proponer_cancelacion")
        .doesNotContain("proponer_reprogramacion");
    evidence.add(
        Map.of(
            "name",
            "consultar_horarios",
            "result",
            Map.of("items", List.of(Map.of("slot_id", "slot")))));
    assertThat(names(evidence)).contains("proponer_reprogramacion", "proponer_cita");
  }

  @Test
  void reschedulingMustFindOwnAppointmentEvenIfTheModelAlreadyLookedUpTheCatalog() {
    var evidence = new ArrayList<Map<String, Object>>();
    evidence.add(Map.of("name", "verificar_paciente", "result", Map.of("verified", true)));
    evidence.add(
        Map.of(
            "name",
            "consultar_servicios",
            "result",
            Map.of("items", List.of(Map.of("id", "service")))));
    String request = "Quiero reprogramar mi limpieza para el martes a las 09:00";
    assertThat(names(evidence, request)).containsExactly("consultar_mis_citas");
    evidence.add(
        Map.of(
            "name",
            "consultar_mis_citas",
            "result",
            Map.of("items", List.of(Map.of("appointment_ref", "own")))));
    assertThat(names(evidence, request))
        .contains("consultar_horarios")
        .doesNotContain("proponer_cita", "proponer_reprogramacion");
  }

  @Test
  void negationAndInformationDoNotForceChangeQueriesOrExposeProposalTools() {
    var evidence =
        List.<Map<String, Object>>of(
            Map.of("name", "verificar_paciente", "result", Map.of("verified", true)),
            Map.of(
                "name",
                "consultar_horarios",
                "result",
                Map.of("items", List.of(Map.of("slot_id", "slot")))));
    var names = names(evidence, "No reprogrames mi cita. Solo quiero información.");
    assertThat(names)
        .contains("consultar_servicios", "derivar_recepcion")
        .doesNotContain("proponer_cita", "proponer_reprogramacion", "proponer_cancelacion");
    assertThat(names).hasSizeGreaterThan(1);
  }

  @Test
  void optionalNullsMatchOmissionSemanticsWhileRequiredIdentifiersRemainRequiredStrings() {
    var slotTool =
        definitions.all().stream()
            .filter(
                tool -> ((Map<?, ?>) tool.get("function")).get("name").equals("consultar_horarios"))
            .findFirst()
            .orElseThrow();
    var function = (Map<?, ?>) slotTool.get("function");
    var parameters = (Map<?, ?>) function.get("parameters");
    var properties = (Map<?, ?>) parameters.get("properties");
    assertThat(((Map<?, ?>) properties.get("appointment_ref")).get("type"))
        .isEqualTo(List.of("string", "null"));
    assertThat(((Map<?, ?>) properties.get("days_from_today")).get("type"))
        .isEqualTo(List.of("integer", "null"));
    assertThat(((Map<?, ?>) properties.get("service_id")).get("type")).isEqualTo("string");
    assertThat(parameters.get("required")).isEqualTo(List.of("service_id"));
    assertThat(parameters.get("additionalProperties")).isEqualTo(false);
  }

  @Test
  void newBookingCanQueryCatalogButCannotProposeAnUnverifiedPatientOrMissingSlot() {
    var evidence = new ArrayList<Map<String, Object>>();
    evidence.add(
        Map.of(
            "name",
            "consultar_servicios",
            "result",
            Map.of("items", List.of(Map.of("id", "service")))));
    assertThat(names(evidence))
        .contains("consultar_horarios", "verificar_paciente")
        .doesNotContain("proponer_cita");
    evidence.add(Map.of("name", "verificar_paciente", "result", Map.of("verified", false)));
    evidence.add(Map.of("name", "consultar_horarios", "result", Map.of("items", List.of())));
    assertThat(names(evidence)).doesNotContain("proponer_cita", "proponer_reprogramacion");
  }

  private List<String> names(List<Map<String, Object>> evidence) {
    return definitions.available(evidence).stream()
        .map(tool -> ((Map<?, ?>) tool.get("function")).get("name").toString())
        .toList();
  }

  private List<String> names(List<Map<String, Object>> evidence, String request) {
    return definitions.available(evidence, request).stream()
        .map(tool -> ((Map<?, ?>) tool.get("function")).get("name").toString())
        .toList();
  }
}
