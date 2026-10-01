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
  public AppointmentResponse create(AppointmentRequest r) {
    var profile = profiles.readLockedInstallation().orElseThrow();
    var zone = ZoneId.of(profile.getTimeZone());
    var dentist = dentists.lockById(r.dentistId()).orElseThrow(ApiException::notFound);
    String fingerprint = fingerprint(r);
    var repeated = appointments.findByRequestKey(r.requestKey());
    if (repeated.isPresent()) {
      if (!repeated.get().getRequestFingerprint().equals(fingerprint))
        throw ApiException.conflict(
            "La clave de solicitud ya se utilizó para una reserva diferente.");
      return AppointmentResponse.of(repeated.get(), zone);
    }
    var patient = patients.findById(r.patientId()).orElseThrow(ApiException::notFound);
    if (!patient.getActive()) throw ApiException.badRequest("El paciente está inactivo.");
    var service =
        r.serviceId() == null
            ? null
            : services.findById(r.serviceId()).orElseThrow(ApiException::notFound);
    rules.eligible(dentist, service);
    int duration =
        service == null
            ? (r.durationMinutes() == null ? 0 : r.durationMinutes())
            : service.getDurationMinutes();
    if (duration < 1 || duration > 1440 || service == null && r.reason().isBlank())
      throw ApiException.badRequest("Una cita administrativa necesita motivo y duración positiva.");
    Instant start = rules.instant(r.localStart(), zone);
    availability.requireAvailable(dentist.getId(), start, duration, profile, null);
    var a = new Appointment();
    a.setPatient(patient);
    a.setDentist(dentist);
    a.setDentistName(dentist.getFullName());
    a.setService(service);
    a.setServiceName(service == null ? r.reason().strip() : service.getName());
    a.setDurationMinutes(duration);
    a.setGapMinutes(profile.getAppointmentGapMinutes());
    setInterval(a, start);
    a.setNotes(r.notes().strip());
    a.setRequestKey(r.requestKey());
    a.setRequestFingerprint(fingerprint);
    appointments.saveAndFlush(a);
    history.append(a, "CREATED", null, null, "Reserva manual");
    audit.record(
        "APPOINTMENT_CREATED",
        "APPOINTMENT",
        a.getId(),
        "Reservó una cita manual para ficha " + patient.getCode());
    return AppointmentResponse.of(a, zone);
  }

  @Transactional
  public AppointmentResponse reschedule(UUID id, RescheduleRequest r) {
    var p = profiles.readLockedInstallation().orElseThrow();
    var a = appointments.lockById(id).orElseThrow(ApiException::notFound);
    a.checkVersion(r.version());
    states.requireReschedulable(a);
    Map<UUID, Dentist> locked = new HashMap<>();
    java.util.stream.Stream.of(a.getDentist().getId(), r.dentistId())
        .distinct()
        .sorted()
        .forEach(
            key -> locked.put(key, dentists.lockById(key).orElseThrow(ApiException::notFound)));
    var d = locked.get(r.dentistId());
    rules.eligible(d, a.getService());
    if (!a.getPatient().getActive()) throw ApiException.badRequest("El paciente está inactivo.");
    int duration =
        r.useCurrentDuration() && a.getService() != null
            ? a.getService().getDurationMinutes()
            : a.getDurationMinutes();
    ZoneId zone = ZoneId.of(p.getTimeZone());
    Instant start = rules.instant(r.localStart(), zone);
    availability.requireAvailable(d.getId(), start, duration, p, id);
    var previousStart = a.getStartsAt();
    var previousStatus = a.getStatus();
    a.setDentist(d);
    a.setDentistName(d.getFullName());
    a.setDurationMinutes(duration);
    a.setGapMinutes(p.getAppointmentGapMinutes());
    setInterval(a, start);
    a.setStatus(AppointmentStatus.RESERVED);
    appointments.saveAndFlush(a);
    history.append(a, "RESCHEDULED", previousStatus, previousStart, r.reason().strip());
    audit.record(
        "APPOINTMENT_RESCHEDULED",
        "APPOINTMENT",
        id,
        "Reprogramó cita y solicitó una nueva confirmación");
    return AppointmentResponse.of(a, zone);
  }

  @Transactional
  public AppointmentResponse changeStatus(UUID id, StatusRequest r) {
    var p = profiles.readLockedInstallation().orElseThrow();
    var a = appointments.lockById(id).orElseThrow(ApiException::notFound);
    a.checkVersion(r.version());
    dentists.lockById(a.getDentist().getId()).orElseThrow(ApiException::notFound);
    states.requireTransition(a, r.status(), r.reason(), clock);
    var previous = a.getStatus();
    a.setStatus(r.status());
    appointments.saveAndFlush(a);
    history.append(
        a,
        r.status() == AppointmentStatus.CANCELLED ? "CANCELLED" : "STATUS_CHANGED",
        previous,
        a.getStartsAt(),
        r.reason().strip());
    audit.record(
        "APPOINTMENT_STATUS",
        "APPOINTMENT",
        id,
        "Cambió estado de " + previous + " a " + r.status());
    return AppointmentResponse.of(a, ZoneId.of(p.getTimeZone()));
  }

  private void setInterval(Appointment a, Instant start) {
    a.setStartsAt(start);
    a.setEndsAt(start.plusSeconds(a.getDurationMinutes() * 60L));
    a.setBlockedUntil(a.getEndsAt().plusSeconds(a.getGapMinutes() * 60L));
  }

  private String fingerprint(AppointmentRequest r) {
    try {
      return HexFormat.of()
          .formatHex(
              MessageDigest.getInstance("SHA-256")
                  .digest(mapper.writeValueAsString(r).getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
