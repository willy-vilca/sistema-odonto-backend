package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;

class AgentCatalogEvidenceTests {
  @Test
  void rejectsInventedClaimsAndAcceptsOnlyCurrentToolValues() {
    var evidence = new AgentCatalogEvidence();
    assertThat(evidence.supports("S/. 120 y 50 minutos")).isFalse();
    assertThat(evidence.supports("¿Para quién es la cita?")).isTrue();
    evidence.record(
        "consultar_servicios",
        Map.of(
            "items", List.of(Map.of("price", new BigDecimal("200.00"), "duration_minutes", 60))));
    assertThat(evidence.supports("**PEN 200.00**, 60 minutos.")).isTrue();
    assertThat(evidence.supports("S/ 200,00 y 60 min.")).isTrue();
    assertThat(evidence.supports("200 soles y 60 minutos.")).isTrue();
    assertThat(evidence.supports("S/. 120 y 60 minutos.")).isFalse();
    assertThat(evidence.supports("S/. 200 y 50 minutos.")).isFalse();
  }

  @Test
  void unrelatedOrRejectedToolsDoNotSupplyCatalogueEvidence() {
    var evidence = new AgentCatalogEvidence();
    evidence.record("pacientes_contacto", Map.of("items", List.of(Map.of("price", 200))));
    evidence.record("consultar_servicios", Map.of("error", "No disponible"));
    assertThat(evidence.supports("El precio es PEN 200.")).isFalse();
  }
}
