package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.Test;

class AgentConfirmationTests {
  @Test
  void acceptsExplicitNaturalConfirmationAndReferenceCode() {
    for (String text :
        new String[] {
          "Sí, confirmo",
          "Confirmo esa cita",
          "sí quiero reservar la cita",
          "¡Sí, confirmo la cita, por favor!"
        }) assertThat(AgentConfirmation.natural(text)).as(text).isTrue();
    assertThat(AgentConfirmation.code("CONFIRMO A1B2C3D4")).contains("A1B2C3D4");
  }

  @Test
  void questionsNegationsAndGenericAcknowledgementsNeverConfirm() {
    for (String text :
        new String[] {
          "sí",
          "ok",
          "no confirmo",
          "Sí, pero no reserves",
          "¿Confirmo?",
          "Si confirmo, cuánto cuesta",
          "\"Sí, confirmo\"",
          "Confirmo pago"
        }) assertThat(AgentConfirmation.natural(text)).as(text).isFalse();
  }

  @Test
  void identifiesShortAcknowledgementsForClarificationWithoutTreatingThemAsConsent() {
    for (String text : new String[] {"sí", "OK", "vale", "¿Confirmo?"})
      assertThat(AgentConfirmation.ambiguous(text)).as(text).isTrue();
    assertThat(AgentConfirmation.ambiguous("No confirmo")).isFalse();
    assertThat(AgentConfirmation.ambiguous("Sí, confirmo la cita")).isFalse();
  }
}
