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
  public static AppointmentResponse of(Appointment appointment, ZoneId zone) {
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getPatient().getId(),
        appointment.getPatient().getFullName(),
        appointment.getPatient().getCode(),
        appointment.getDentist().getId(),
        appointment.getDentistName(),
        appointment.getService() == null ? null : appointment.getService().getId(),
        appointment.getServiceName(),
        appointment.getDurationMinutes(),
        appointment.getGapMinutes(),
        appointment.getStartsAt(),
        appointment.getEndsAt(),
        LocalDateTime.ofInstant(appointment.getStartsAt(), zone),
        LocalDateTime.ofInstant(appointment.getEndsAt(), zone),
        appointment.getStatus().name(),
        appointment.getOrigin(),
        appointment.getNotes(),
        appointment.getVersion());
  }
}
