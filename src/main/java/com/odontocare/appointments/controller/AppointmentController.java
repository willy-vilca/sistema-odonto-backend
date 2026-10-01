package com.odontocare.appointments.controller;

import com.odontocare.appointments.dto.*;
import com.odontocare.appointments.model.AppointmentStatus;
import com.odontocare.appointments.service.*;
import com.odontocare.shared.pagination.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.time.LocalDate;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {
  private final AppointmentService commands;
  private final AppointmentQueryService queries;
  private final AvailabilityService availability;

  public AppointmentController(
      AppointmentService commands,
      AppointmentQueryService queries,
      AvailabilityService availability) {
    this.commands = commands;
    this.queries = queries;
    this.availability = availability;
  }

  @GetMapping
  @PreAuthorize("hasAuthority('APPOINTMENTS_READ')")
  public PageResponse<AppointmentResponse> list(
      @Valid @ModelAttribute PageQuery q,
      @RequestParam(required = false) UUID dentistId,
      @RequestParam(required = false) UUID patientId,
      @RequestParam(required = false) AppointmentStatus status,
      @RequestParam(required = false) LocalDate from,
      @RequestParam(required = false) LocalDate to) {
    return queries.list(q, dentistId, patientId, status, from, to);
  }

  @GetMapping("/calendar")
  @PreAuthorize("hasAuthority('APPOINTMENTS_READ')")
  public AppointmentQueryService.CalendarResponse calendar(
      @RequestParam(required = false) UUID dentistId,
      @RequestParam LocalDate from,
      @RequestParam LocalDate to) {
    return queries.calendar(dentistId, from, to);
  }

  @GetMapping("/availability")
  @PreAuthorize("hasAuthority('APPOINTMENTS_READ')")
  public PageResponse<AvailabilityService.Slot> availability(
      @RequestParam UUID dentistId,
      @RequestParam(required = false) UUID serviceId,
      @RequestParam(required = false) @Min(1) @Max(1440) Integer durationMinutes,
      @RequestParam LocalDate date,
      @RequestParam(required = false) UUID appointmentId,
      @Valid @ModelAttribute PageQuery q) {
    return availability.slots(dentistId, serviceId, durationMinutes, date, appointmentId, q);
  }

  @GetMapping("/{id}")
  @PreAuthorize("hasAuthority('APPOINTMENTS_READ')")
  public AppointmentResponse get(@PathVariable UUID id) {
    return queries.get(id);
  }

  @GetMapping("/{id}/history")
  @PreAuthorize("hasAuthority('APPOINTMENTS_READ')")
  public PageResponse<HistoryResponse> history(
      @PathVariable UUID id, @Valid @ModelAttribute PageQuery q) {
    return queries.history(id, q);
  }

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @PreAuthorize("hasAuthority('APPOINTMENTS_WRITE')")
  public AppointmentResponse create(@Valid @RequestBody AppointmentRequest r) {
    return commands.create(r);
  }

  @PutMapping("/{id}/reschedule")
  @PreAuthorize("hasAuthority('APPOINTMENTS_WRITE')")
  public AppointmentResponse reschedule(
      @PathVariable UUID id, @Valid @RequestBody RescheduleRequest r) {
    return commands.reschedule(id, r);
  }

  @PutMapping("/{id}/status")
  @PreAuthorize("hasAuthority('APPOINTMENTS_WRITE')")
  public AppointmentResponse status(@PathVariable UUID id, @Valid @RequestBody StatusRequest r) {
    return commands.changeStatus(id, r);
  }
}
