package com.odontocare.clinical.service;

import com.odontocare.appointments.model.AppointmentStatus;
import com.odontocare.appointments.repository.AppointmentRepository;
import com.odontocare.catalog.repository.DentalServiceRepository;
import com.odontocare.clinical.dto.ClinicalContracts.*;
import com.odontocare.clinical.dto.EncounterWorkflowContracts.*;
import com.odontocare.clinical.repository.EncounterRepository;
import com.odontocare.installation.repository.InstallationProfileRepository;
import com.odontocare.shared.web.ApiException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class EncounterWorkflowService {
  private final AppointmentRepository appointments;
  private final EncounterRepository records;
  private final EncounterService encounters;
  private final ClinicalAccess access;
  private final DentalServiceRepository services;
  private final InstallationProfileRepository profiles;

  public EncounterWorkflowService(
      AppointmentRepository appointments,
      EncounterRepository records,
      EncounterService encounters,
      ClinicalAccess access,
      DentalServiceRepository services,
      InstallationProfileRepository profiles) {
    this.appointments = appointments;
    this.records = records;
    this.encounters = encounters;
    this.access = access;
    this.services = services;
    this.profiles = profiles;
  }

  @Transactional
  public Start fromAppointment(UUID id) {
    var appointment = appointments.findById(id).orElseThrow(ApiException::notFound);
    var dentist = access.requireDentist(appointment.getDentist().getId());
    var prior = records.findByAppointmentId(id);
    if (prior.isPresent())
      return new Start(encounters.get(prior.get().getId()), null, dentist.getFullName());
    if (Set.of(AppointmentStatus.CANCELLED, AppointmentStatus.NO_SHOW)
        .contains(appointment.getStatus()))
      throw ApiException.badRequest("Esta cita no admite una atención.");
    var service =
        appointment.getService() == null
            ? null
            : services.findById(appointment.getService().getId()).orElseThrow();
    var procedures =
        service == null
            ? List.<Procedure>of()
            : List.of(
                new Procedure(
                    service.getId(),
                    appointment.getServiceName(),
                    1,
                    null,
                    null,
                    service.getPrice()));
    var date =
        LocalDate.ofInstant(
            appointment.getStartsAt(),
            ZoneId.of(profiles.findById((short) 1).orElseThrow().getTimeZone()));
    if (date.isAfter(access.today())) date = access.today();
    return new Start(
        null,
        new EncounterRequest(
            appointment.getPatient().getId(),
            appointment.getDentist().getId(),
            id,
            date,
            appointment.getServiceName(),
            new EncounterContent("", "", "", "", procedures),
            0L),
        dentist.getFullName());
  }
}
