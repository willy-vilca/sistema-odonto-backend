package com.odontocare.appointments.service;

import com.odontocare.appointments.dto.*;
import com.odontocare.appointments.model.*;
import com.odontocare.appointments.repository.*;
import com.odontocare.installation.repository.InstallationProfileRepository;
import com.odontocare.shared.pagination.*;
import com.odontocare.shared.web.ApiException;
import java.time.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class AppointmentQueryService {
  private final AppointmentRepository appointments;
  private final AppointmentHistoryRepository history;
  private final InstallationProfileRepository profiles;

  public AppointmentQueryService(
      AppointmentRepository appointments,
      AppointmentHistoryRepository history,
      InstallationProfileRepository profiles) {
    this.appointments = appointments;
    this.history = history;
    this.profiles = profiles;
  }

  private ZoneId zone() {
    return ZoneId.of(profiles.findById((short) 1).orElseThrow().getTimeZone());
  }

  public AppointmentResponse get(UUID id) {
    return AppointmentResponse.of(
        appointments.findById(id).orElseThrow(ApiException::notFound), zone());
  }

  private Specification<Appointment> criteria(
      PageQuery q,
      UUID dentistId,
      UUID patientId,
      AppointmentStatus status,
      LocalDate from,
      LocalDate to,
      ZoneId zone) {
    if (from != null && to != null && to.isBefore(from))
      throw ApiException.badRequest("El rango de fechas no es válido.");
    Specification<Appointment> s =
        SearchSpecifications.text(
            q.getSearch(), "patient.fullName", "patient.code", "serviceName", "dentistName");
    if (dentistId != null) s = s.and((r, x, c) -> c.equal(r.get("dentist").get("id"), dentistId));
    if (patientId != null) s = s.and((r, x, c) -> c.equal(r.get("patient").get("id"), patientId));
    if (status != null) s = s.and(SearchSpecifications.equal("status", status));
    if (from != null)
      s =
          s.and(
              (r, x, c) ->
                  c.greaterThanOrEqualTo(r.get("startsAt"), from.atStartOfDay(zone).toInstant()));
    if (to != null)
      s =
          s.and(
              (r, x, c) ->
                  c.lessThan(r.get("startsAt"), to.plusDays(1).atStartOfDay(zone).toInstant()));
    return s;
  }

  public PageResponse<AppointmentResponse> list(
      PageQuery q,
      UUID dentistId,
      UUID patientId,
      AppointmentStatus status,
      LocalDate from,
      LocalDate to) {
    var zone = zone();
    return PageResponse.of(
        appointments
            .findAll(
                criteria(q, dentistId, patientId, status, from, to, zone),
                q.pageable(
                    Map.of(
                        "name",
                        "startsAt",
                        "startsAt",
                        "startsAt",
                        "patient",
                        "patient.fullName",
                        "status",
                        "status")))
            .map(a -> AppointmentResponse.of(a, zone)));
  }

  public record CalendarResponse(
      List<AppointmentResponse> items, String timeZone, LocalDate from, LocalDate to) {}

  public CalendarResponse calendar(UUID dentist, LocalDate from, LocalDate to) {
    if (to.isBefore(from) || java.time.temporal.ChronoUnit.DAYS.between(from, to) > 41)
      throw ApiException.badRequest("El calendario admite un intervalo máximo de 42 días.");
    var zone = zone();
    var page =
        appointments.findAll(
            criteria(new PageQuery(), dentist, null, null, from, to, zone),
            PageRequest.of(0, 1000, Sort.by("startsAt").and(Sort.by("id"))));
    if (page.getTotalElements() > 1000)
      throw ApiException.badRequest(
          "Reduce el intervalo o selecciona un odontólogo para consultar el calendario.");
    return new CalendarResponse(
        page.getContent().stream().map(a -> AppointmentResponse.of(a, zone)).toList(),
        zone.toString(),
        from,
        to);
  }

  public PageResponse<HistoryResponse> history(UUID id, PageQuery q) {
    if (!appointments.existsById(id)) throw ApiException.notFound();
    Specification<AppointmentHistory> s =
        SearchSpecifications.<AppointmentHistory>text(
                q.getSearch(), "reason", "actorName", "action")
            .and((r, x, c) -> c.equal(r.get("appointment").get("id"), id));
    return PageResponse.of(
        history
            .findAll(s, q.pageable(Map.of("name", "createdAt", "createdAt", "createdAt")))
            .map(HistoryResponse::of));
  }
}
