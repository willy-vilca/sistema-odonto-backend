package com.odontocare.appointments.dto;

import com.odontocare.appointments.model.Appointment;
import java.time.*;
import java.util.UUID;

public record AppointmentResponse(
    UUID id,
    UUID patientId,
    String patientName,
    String patientCode,
    UUID dentistId,
    String dentistName,
    UUID serviceId,
    String serviceName,
    int durationMinutes,
    int gapMinutes,
    Instant startsAt,
    Instant endsAt,
    LocalDateTime localStart,
    LocalDateTime localEnd,
    String status,
    String origin,
    String notes,
    long version) {
  public static AppointmentResponse of(Appointment a, ZoneId zone) {
    return new AppointmentResponse(
        a.getId(),
        a.getPatient().getId(),
        a.getPatient().getFullName(),
        a.getPatient().getCode(),
        a.getDentist().getId(),
        a.getDentistName(),
        a.getService() == null ? null : a.getService().getId(),
        a.getServiceName(),
        a.getDurationMinutes(),
        a.getGapMinutes(),
        a.getStartsAt(),
        a.getEndsAt(),
        LocalDateTime.ofInstant(a.getStartsAt(), zone),
        LocalDateTime.ofInstant(a.getEndsAt(), zone),
        a.getStatus().name(),
        a.getOrigin(),
        a.getNotes(),
        a.getVersion());
  }
}
