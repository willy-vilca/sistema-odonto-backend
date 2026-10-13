package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import java.util.*;
import org.junit.jupiter.api.Test;
import tools.jackson.databind.ObjectMapper;

class AgentAvailabilityReplyTests {
  @Test
  void emptyDayHasOneClearMessageWithTheActualDate() {
    var reply = new AgentAvailabilityReply();
    reply.record(
        "consultar_horarios",
        new ObjectMapper().valueToTree(Map.of()),
        Map.of(
            "date",
            "2026-10-17",
            "items",
            List.of(),
            "preferred_time",
            "",
            "preferred_time_available",
            false,
            "professional_found",
            true));
    assertThat(reply.response().orElseThrow())
        .contains("sábado 17/10/2026", "No hay horarios")
        .doesNotContain("Estos son los horarios disponibles");
  }

  @Test
  void aMissingProfessionalDoesNotClaimTheHourIsOccupied() {
    var reply = new AgentAvailabilityReply();
    reply.record(
        "consultar_horarios",
        new ObjectMapper().valueToTree(Map.of()),
        Map.of(
            "date",
            "2026-10-13",
            "items",
            List.of(),
            "preferred_time",
            "10:00",
            "preferred_time_available",
            false,
            "professional_found",
            false));
    assertThat(reply.response().orElseThrow())
        .contains("Confirma con qué odontólogo")
        .doesNotContain("10:00 no está disponible");
  }
}
