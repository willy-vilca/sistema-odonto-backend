package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class AgentRequestedDateTests {
  @Test
  void resolvesWeekdaysFromWednesdayWithoutTrustingModelArithmetic() {
    var received = LocalDate.of(2026, 10, 7);
    assertThat(AgentRequestedDate.resolve("el próximo lunes a las 9", received))
        .contains(LocalDate.of(2026, 10, 12));
    assertThat(AgentRequestedDate.resolve("el próximo martes", received))
        .contains(LocalDate.of(2026, 10, 13));
    assertThat(AgentRequestedDate.resolve("el jueves", received))
        .contains(LocalDate.of(2026, 10, 8));
    assertThat(AgentRequestedDate.resolve("próximo miércoles", received))
        .contains(LocalDate.of(2026, 10, 14));
    assertThat(AgentRequestedDate.resolve("el miércoles", received)).contains(received);
  }

  @Test
  void handlesYearBoundaryAndLeavesAmbiguousDatesToClarification() {
    assertThat(AgentRequestedDate.resolve("próximo lunes", LocalDate.of(2023, 12, 31)))
        .contains(LocalDate.of(2024, 1, 1));
    assertThat(AgentRequestedDate.resolve("lunes o martes", LocalDate.of(2026, 10, 7))).isEmpty();
    assertThat(AgentRequestedDate.resolve("¿Cuánto cuesta?", LocalDate.of(2026, 10, 7))).isEmpty();
  }

  @Test
  void respectsAbsoluteDatesAndNextWeekInsteadOfOverridingThemWithNearestWeekday() {
    var received = LocalDate.of(2026, 10, 7);
    assertThat(AgentRequestedDate.resolve("lunes 19/10/2026", received))
        .contains(LocalDate.of(2026, 10, 19));
    assertThat(AgentRequestedDate.resolve("2026-10-13", received))
        .contains(LocalDate.of(2026, 10, 13));
    assertThat(AgentRequestedDate.resolve("viernes de la próxima semana", received))
        .contains(LocalDate.of(2026, 10, 16));
    assertThat(AgentRequestedDate.resolve("lunes 19 de octubre", received)).isEmpty();
    org.assertj.core.api.Assertions.assertThatThrownBy(
            () -> AgentRequestedDate.resolve("lunes 13/10/2026", received))
        .hasMessageContaining("no coinciden");
  }
}
