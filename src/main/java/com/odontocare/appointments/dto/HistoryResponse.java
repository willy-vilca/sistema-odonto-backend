package com.odontocare.appointments.dto;

import com.odontocare.appointments.model.AppointmentHistory;
import java.time.Instant;
import java.util.UUID;

public record HistoryResponse(
    UUID id,
    String action,
    String previousStatus,
    String status,
    Instant previousStart,
    Instant startsAt,
    Instant endsAt,
    String dentistName,
    int durationMinutes,
    String actorName,
    String reason,
    Instant createdAt) {
  public static HistoryResponse of(AppointmentHistory h) {
    return new HistoryResponse(
        h.getId(),
        h.getAction(),
        h.getPreviousStatus(),
        h.getStatus(),
        h.getPreviousStart(),
        h.getStartsAt(),
        h.getEndsAt(),
        h.getDentistName(),
        h.getDurationMinutes(),
        h.getActorName(),
        h.getReason(),
        h.getCreatedAt());
  }
}
