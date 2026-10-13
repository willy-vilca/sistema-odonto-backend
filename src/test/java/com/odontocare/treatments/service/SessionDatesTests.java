package com.odontocare.treatments.service;

import static org.assertj.core.api.Assertions.*;

import com.odontocare.treatments.dto.TreatmentContracts.Scheduling;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.Test;

class SessionDatesTests {
  final SessionDates dates = new SessionDates();

  @Test
  void monthsStayAnchoredToTheOriginalDayRatherThanDriftingAfterFebruary() {
    assertThat(
            dates.calculate(
                new Scheduling("FREQUENCY", List.of(), LocalDate.of(2028, 1, 31), 1, "MONTHS"), 4))
        .containsExactly(
            LocalDate.of(2028, 1, 31),
            LocalDate.of(2028, 2, 29),
            LocalDate.of(2028, 3, 31),
            LocalDate.of(2028, 4, 30));
  }

  @Test
  void explicitDatesMayLeaveSessionsUndatedAndNoneDoesNotInventDates() {
    assertThat(
            dates.calculate(
                new Scheduling(
                    "DATES", Arrays.asList(LocalDate.of(2030, 1, 1), null), null, null, null),
                3))
        .containsExactly(LocalDate.of(2030, 1, 1), null, null);
    assertThat(dates.calculate(null, 2)).containsExactly(null, null);
  }

  @Test
  void weeklyIntervalsAndInvalidFrequenciesAreValidated() {
    assertThat(
            dates.calculate(
                new Scheduling("FREQUENCY", List.of(), LocalDate.of(2030, 1, 1), 2, "WEEKS"), 2))
        .containsExactly(LocalDate.of(2030, 1, 1), LocalDate.of(2030, 1, 15));
    assertThatThrownBy(
            () ->
                dates.calculate(
                    new Scheduling("FREQUENCY", List.of(), LocalDate.now(), 0, "DAYS"), 2))
        .hasMessageContaining("frecuencia positiva");
    assertThatThrownBy(
            () ->
                dates.calculate(
                    new Scheduling(
                        "DATES", List.of(LocalDate.now(), LocalDate.now()), null, null, null),
                    1))
        .hasMessageContaining("superar");
  }
}
