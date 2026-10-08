package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AgentConsentTests {
  @Test
  void confirmationsCannotChangeTheirOperation() {
    assertThat(AgentConsent.matchesAction("Sí, confirmo la cancelación", "BOOK")).isFalse();
    assertThat(AgentConsent.matchesAction("Sí, quiero reservar esa cita", "CANCEL")).isFalse();
    assertThat(AgentConsent.matchesAction("Sí, confirmo la reprogramación", "RESCHEDULE")).isTrue();
    assertThat(AgentConsent.matchesAction("Sí, confirmo la cita", "CANCEL")).isTrue();
  }

  @Test
  void denialsAndInformationQueriesDoNotPrepareChanges() {
    for (String text :
        new String[] {
          "No canceles mi cita",
          "Por ahora solo quiero información",
          "No me reserves todavía",
          "Solo consultar disponibilidad"
        }) assertThat(AgentConsent.forbidsProposal(text)).isTrue();
    assertThat(AgentConsent.forbidsProposal("Quiero reprogramar por un viaje")).isFalse();
  }
}
