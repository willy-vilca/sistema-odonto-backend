package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class AgentRequestedTimeTests {
  @Test
  void readsExplicitClockAndMeridianWithoutParsingTheDate() {
    assertThat(AgentRequestedTime.resolve("Elijo el martes 20/10/2026 a las 09:00 con Julia"))
        .contains(LocalTime.of(9, 0));
    assertThat(AgentRequestedTime.resolve("mañana a las 9:30 a. m.")).contains(LocalTime.of(9, 30));
    assertThat(AgentRequestedTime.resolve("el martes a las 3 pm")).contains(LocalTime.of(15, 0));
    assertThat(AgentRequestedTime.resolve("a las 12:00 de la tarde")).contains(LocalTime.NOON);
    assertThat(AgentRequestedTime.resolve("a las 12 am")).contains(LocalTime.MIDNIGHT);
  }

  @Test
  void doesNotChooseFromRangesInvalidTimesOrDateNumbers() {
    assertThat(AgentRequestedTime.resolve("el martes 20/10/2026 por la mañana")).isEmpty();
    assertThat(AgentRequestedTime.resolve("entre las 09:00 y las 11:00")).isEmpty();
    assertThat(AgentRequestedTime.resolve("a las 25:00")).isEmpty();
    assertThat(AgentRequestedTime.resolve("a las 09:80")).isEmpty();
  }
}
