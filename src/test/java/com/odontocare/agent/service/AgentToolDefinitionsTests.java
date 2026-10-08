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
}
