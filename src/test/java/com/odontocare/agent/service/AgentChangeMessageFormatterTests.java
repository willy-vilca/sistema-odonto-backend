package com.odontocare.agent.service;

import static org.assertj.core.api.Assertions.*;

import com.odontocare.appointments.dto.AppointmentResponse;
import java.time.*;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class AgentChangeMessageFormatterTests {
  private final AgentChangeMessageFormatter formatter = new AgentChangeMessageFormatter();

  @Test
  void confirmationShowsOnlyTheResultingDateAndReadableLines() {
    var a = appointment(LocalDateTime.of(2026, 10, 20, 9, 0), "CONFIRMED");
    var text = formatter.confirmed("RESCHEDULE", a);
    assertThat(text)
        .contains(
            "confirmada.\n\nPaciente:",
            "\nServicio: Limpieza dental",
            "\nProfesional: Julia",
            "\nFecha: martes 20/10/2026",
            "\nHorario: 09:00–10:00",
            "\nEstado: Confirmada",
            "\nReferencia: " + a.id())
        .doesNotContain("13/10/2026", "Fecha actual:", "Fecha anterior:", "Nuevo horario:");
  }

  @Test
  void repeatedOperationShowsCurrentDataAndCurrentStatus() {
    var text =
        formatter.repeated(
            "RESCHEDULE", appointment(LocalDateTime.of(2026, 10, 21, 14, 0), "CANCELLED"));
    assertThat(text)
        .contains(
            "ya estaba registrada",
            "datos actuales",
            "miércoles 21/10/2026",
            "Horario: 14:00–15:00",
            "Estado: Cancelada")
        .doesNotContain("20/10/2026", "Fecha anterior:");
  }

  @Test
  void overnightIntervalIdentifiesItsEndingDate() {
    var text =
        formatter.confirmed(
            "RESCHEDULE", appointment(LocalDateTime.of(2026, 10, 20, 23, 0), "CONFIRMED"));
    assertThat(text).contains("Fecha: martes 20/10/2026", "Horario: 23:00–00:00 del 21/10/2026");
  }

  private AppointmentResponse appointment(LocalDateTime start, String status) {
    var zone = ZoneId.of("America/Lima");
    var end = start.plusHours(1);
    return new AppointmentResponse(
        UUID.fromString("fc272d39-566d-4b65-a610-0302e86d50b5"),
        UUID.randomUUID(),
        "Willy Vilca Huaytalla",
        "P001",
        UUID.randomUUID(),
        "Julia",
        UUID.randomUUID(),
        "Limpieza dental",
        60,
        0,
        start.atZone(zone).toInstant(),
        end.atZone(zone).toInstant(),
        start,
        end,
        status,
        "WHATSAPP",
        "",
        1);
  }
}
