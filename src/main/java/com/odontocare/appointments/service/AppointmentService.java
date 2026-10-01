package com.odontocare.appointments.service;

import com.odontocare.appointments.dto.*;
import com.odontocare.appointments.model.*;
import com.odontocare.appointments.repository.*;
import com.odontocare.audit.service.AuditService;
import com.odontocare.catalog.repository.DentalServiceRepository;
import com.odontocare.dentists.model.Dentist;
import com.odontocare.dentists.repository.DentistRepository;
import com.odontocare.installation.repository.InstallationProfileRepository;
import com.odontocare.patients.repository.PatientRepository;
import com.odontocare.shared.web.ApiException;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

@Service
public class AppointmentService {
  private final AppointmentRepository appointments;
  private final PatientRepository patients;
  private final DentistRepository dentists;
  private final DentalServiceRepository services;
  private final InstallationProfileRepository profiles;
  private final AvailabilityService availability;
  private final BookingRules rules;
  private final AppointmentStateRules states;
  private final AppointmentHistoryService history;
  private final AuditService audit;
  private final Clock clock;
  private final ObjectMapper mapper;

  public AppointmentService(
      AppointmentRepository appointments,
      PatientRepository patients,
      DentistRepository dentists,
      DentalServiceRepository services,
      InstallationProfileRepository profiles,
      AvailabilityService availability,
      BookingRules rules,
      AppointmentStateRules states,
      AppointmentHistoryService history,
      AuditService audit,
      Clock clock,
      ObjectMapper mapper) {
    this.appointments = appointments;
    this.patients = patients;
    this.dentists = dentists;
    this.services = services;
    this.profiles = profiles;
    this.availability = availability;
    this.rules = rules;
    this.states = states;
    this.history = history;
    this.audit = audit;
    this.clock = clock;
    this.mapper = mapper;
  }

  @Transactional
  public AppointmentResponse create(AppointmentRequest request) {
    var profile = profiles.readLockedInstallation().orElseThrow();
    var zone = ZoneId.of(profile.getTimeZone());
    var dentist = dentists.lockById(request.dentistId()).orElseThrow(ApiException::notFound);
    String fingerprint = fingerprint(request);
    var repeated = appointments.findByRequestKey(request.requestKey());
    if (repeated.isPresent()) {
      if (!repeated.get().getRequestFingerprint().equals(fingerprint))
        throw ApiException.conflict(
            "La clave de solicitud ya se utilizó para una reserva diferente.");
      return AppointmentResponse.of(repeated.get(), zone);
    }
    var patient = patients.findById(request.patientId()).orElseThrow(ApiException::notFound);
    if (!patient.getActive()) throw ApiException.badRequest("El paciente está inactivo.");
    var service =
        request.serviceId() == null
            ? null
            : services.findById(request.serviceId()).orElseThrow(ApiException::notFound);
    rules.eligible(dentist, service);
    int duration =
        service == null
            ? (request.durationMinutes() == null ? 0 : request.durationMinutes())
            : service.getDurationMinutes();
    if (duration < 1 || duration > 1440 || service == null && request.reason().isBlank())
      throw ApiException.badRequest("Una cita administrativa necesita motivo y duración positiva.");
    Instant start = rules.instant(request.localStart(), zone);
    availability.requireAvailable(dentist.getId(), start, duration, profile, null);
    var appointment = new Appointment();
    appointment.setPatient(patient);
    appointment.setDentist(dentist);
    appointment.setDentistName(dentist.getFullName());
    appointment.setService(service);
    appointment.setServiceName(service == null ? request.reason().strip() : service.getName());
    appointment.setDurationMinutes(duration);
    appointment.setGapMinutes(profile.getAppointmentGapMinutes());
    setInterval(appointment, start);
    appointment.setNotes(request.notes().strip());
    appointment.setRequestKey(request.requestKey());
    appointment.setRequestFingerprint(fingerprint);
    appointments.saveAndFlush(appointment);
    history.append(appointment, "CREATED", null, null, "Reserva manual");
    audit.record(
        "APPOINTMENT_CREATED",
        "APPOINTMENT",
        appointment.getId(),
        "Reservó una cita manual para ficha " + patient.getCode());
    return AppointmentResponse.of(appointment, zone);
  }

  @Transactional
  public AppointmentResponse reschedule(UUID id, RescheduleRequest request) {
    var profile = profiles.readLockedInstallation().orElseThrow();
    var appointment = appointments.lockById(id).orElseThrow(ApiException::notFound);
    appointment.checkVersion(request.version());
    states.requireReschedulable(appointment);
    Map<UUID, Dentist> locked = new HashMap<>();
    java.util.stream.Stream.of(appointment.getDentist().getId(), request.dentistId())
        .distinct()
        .sorted()
        .forEach(
            key -> locked.put(key, dentists.lockById(key).orElseThrow(ApiException::notFound)));
    var dentist = locked.get(request.dentistId());
    rules.eligible(dentist, appointment.getService());
    if (!appointment.getPatient().getActive())
      throw ApiException.badRequest("El paciente está inactivo.");
    int duration =
        request.useCurrentDuration() && appointment.getService() != null
            ? appointment.getService().getDurationMinutes()
            : appointment.getDurationMinutes();
    ZoneId zone = ZoneId.of(profile.getTimeZone());
    Instant start = rules.instant(request.localStart(), zone);
    availability.requireAvailable(dentist.getId(), start, duration, profile, id);
    var previousStart = appointment.getStartsAt();
    var previousStatus = appointment.getStatus();
    appointment.setDentist(dentist);
    appointment.setDentistName(dentist.getFullName());
    appointment.setDurationMinutes(duration);
    appointment.setGapMinutes(profile.getAppointmentGapMinutes());
    setInterval(appointment, start);
    appointment.setStatus(AppointmentStatus.RESERVED);
    appointments.saveAndFlush(appointment);
    history.append(
        appointment, "RESCHEDULED", previousStatus, previousStart, request.reason().strip());
    audit.record(
        "APPOINTMENT_RESCHEDULED",
        "APPOINTMENT",
        id,
        "Reprogramó cita y solicitó una nueva confirmación");
    return AppointmentResponse.of(appointment, zone);
  }

  @Transactional
  public AppointmentResponse changeStatus(UUID id, StatusRequest request) {
    var profile = profiles.readLockedInstallation().orElseThrow();
    var appointment = appointments.lockById(id).orElseThrow(ApiException::notFound);
    appointment.checkVersion(request.version());
    dentists.lockById(appointment.getDentist().getId()).orElseThrow(ApiException::notFound);
    states.requireTransition(appointment, request.status(), request.reason(), clock);
    var previous = appointment.getStatus();
    appointment.setStatus(request.status());
    appointments.saveAndFlush(appointment);
    history.append(
        appointment,
        request.status() == AppointmentStatus.CANCELLED ? "CANCELLED" : "STATUS_CHANGED",
        previous,
        appointment.getStartsAt(),
        request.reason().strip());
    audit.record(
        "APPOINTMENT_STATUS",
        "APPOINTMENT",
        id,
        "Cambió estado de " + previous + " a " + request.status());
    return AppointmentResponse.of(appointment, ZoneId.of(profile.getTimeZone()));
  }

  @Transactional
  public void completeClinical(UUID id) {
    profiles.readLockedInstallation().orElseThrow();
    var appointment = appointments.lockById(id).orElseThrow(ApiException::notFound);
    if (appointment.getStatus() == AppointmentStatus.ATTENDED) return;
    if (Set.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        .contains(appointment.getStatus()))
      throw ApiException.conflict("La cita ya no admite una atención clínica.");
    if (appointment.getStartsAt().isAfter(clock.instant()))
      throw ApiException.badRequest(
          "La atención vinculada solo se finaliza al llegar la hora de la cita.");
    for (var next :
        List.of(
            AppointmentStatus.WAITING, AppointmentStatus.IN_PROGRESS, AppointmentStatus.ATTENDED)) {
      if (appointment.getStatus() == next) continue;
      if (next == AppointmentStatus.WAITING
          && appointment.getStatus() == AppointmentStatus.IN_PROGRESS) continue;
      changeStatus(
          id,
          new StatusRequest(appointment.getVersion(), next, "Finalización de atención clínica"));
    }
  }

  private void setInterval(Appointment appointment, Instant start) {
    appointment.setStartsAt(start);
    appointment.setEndsAt(start.plusSeconds(appointment.getDurationMinutes() * 60L));
    appointment.setBlockedUntil(
        appointment.getEndsAt().plusSeconds(appointment.getGapMinutes() * 60L));
  }

  private String fingerprint(AppointmentRequest request) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(mapper.writeValueAsString(request).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
