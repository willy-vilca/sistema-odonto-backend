package com.odontocare.agent.service;

import com.odontocare.agent.dto.AgentContracts.*;
import com.odontocare.agent.model.AgentRun;
import com.odontocare.agent.repository.*;
import com.odontocare.appointments.dto.AppointmentRequest;
import com.odontocare.appointments.service.AppointmentService;
import com.odontocare.audit.service.AuditService;
import com.odontocare.installation.repository.InstallationProfileRepository;
import com.odontocare.patients.dto.PatientRequest;
import com.odontocare.patients.service.PatientService;
import com.odontocare.shared.web.ApiException;
import com.odontocare.whatsapp.repository.WhatsAppRepository;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AgentBookingService {
  private final AgentRepository runs;
  private final AgentCatalogRepository catalog;
  private final WhatsAppRepository messages;
  private final AppointmentService appointments;
  private final PatientService patients;
  private final InstallationProfileRepository profiles;
  private final AuditService audit;
  private final Clock clock;

  public AgentBookingService(
      AgentRepository runs,
      AgentCatalogRepository catalog,
      WhatsAppRepository messages,
      AppointmentService appointments,
      PatientService patients,
      InstallationProfileRepository profiles,
      AuditService audit,
      Clock clock) {
    this.runs = runs;
    this.catalog = catalog;
    this.messages = messages;
    this.appointments = appointments;
    this.patients = patients;
    this.profiles = profiles;
    this.audit = audit;
    this.clock = clock;
  }

  @Transactional
  public Map<String, Object> confirm(AgentRun run, String code) {
    var message = messages.message(run.messageId(), false).orElseThrow(ApiException::notFound);
    if (!message.direction().equals("INBOUND")
        || !message.body().strip().matches("(?i)CONFIRMO\\s+" + code + "[.!]?"))
      throw ApiException.forbidden();
    var conversation =
        messages.conversation(run.conversationId(), true).orElseThrow(ApiException::notFound);
    if (!Objects.equals(runs.latestInbound(run.conversationId()), message.id()))
      throw ApiException.conflict(
          "Llegó otro mensaje; no se reserva con una confirmación anterior.");
    var p =
        runs.proposalByCode(run.conversationId(), code)
            .orElseThrow(
                () ->
                    ApiException.badRequest(
                        "El código no corresponde a una propuesta de este contacto."));
    if (p.state().equals("CONFIRMED"))
      return Map.of(
          "appointment_id",
          p.appointmentId(),
          "response",
          "La cita ya estaba registrada. Referencia: " + p.appointmentId() + ". " + p.summary(),
          "repeated",
          true);
    if (!p.state().equals("PENDING"))
      throw ApiException.conflict(
          "Esa propuesta fue descartada o ya no está vigente; solicita otra.");
    if (!p.expiresAt().isAfter(clock.instant())) {
      runs.proposalState(p.id(), "EXPIRED");
      return Map.of(
          "response",
          "La propuesta venció. Envía nuevamente el servicio y horario para consultar"
              + " disponibilidad.",
          "appointment_created",
          false);
    }
    var slot = runs.slot(p.slotId()).orElseThrow();
    var profile = profiles.findById((short) 1).orElseThrow();
    var service =
        catalog
            .service(slot.serviceId())
            .orElseThrow(
                () ->
                    ApiException.conflict(
                        "El servicio ya no está habilitado; solicita otra propuesta."));
    if (!profile.getTimeZone().equals(slot.timeZone())
        || ((Number) service.get("duration_minutes")).intValue() != slot.durationMinutes())
      throw ApiException.conflict(
          "Cambió la duración o zona horaria; solicita una nueva propuesta.");
    UUID patient = p.patientId();
    var dentist = catalog.dentists(slot.serviceId(), slot.dentistId());
    if (dentist.isEmpty()
        || !slot.serviceName().equals(service.get("name"))
        || !slot.dentistName().equals(dentist.getFirst().get("full_name")))
      throw ApiException.conflict(
          "Cambió el servicio o profesional; solicita una propuesta actualizada.");
    if (patient != null) {
      var currentPatient =
          catalog.patient(conversation.phone(), patient).orElseThrow(ApiException::forbidden);
      if (!currentPatient.get("full_name").toString().equals(p.patientName()))
        throw ApiException.conflict(
            "Cambió la ficha del paciente; confirma una propuesta actualizada.");
    } else {
      var existing =
          catalog.patients(conversation.phone(), p.patientName(), 0).stream()
              .filter(x -> x.get("full_name").toString().equalsIgnoreCase(p.patientName()))
              .toList();
      if (existing.size() > 1)
        throw ApiException.conflict(
            "Hay varias fichas con ese nombre. Selecciona explícitamente una antes de reservar.");
      if (existing.size() == 1) patient = (UUID) existing.getFirst().get("id");
      else
        patient =
            patients
                .create(
                    new PatientRequest(
                        null,
                        p.patientName(),
                        null,
                        "",
                        "",
                        "",
                        "",
                        "",
                        "",
                        "Ficha provisional creada tras confirmar propuesta del agente.",
                        true,
                        true,
                        List.of(
                            new PatientRequest.ContactRequest(
                                conversation.phone(),
                                conversation.contactName().isBlank()
                                    ? p.patientName()
                                    : conversation.contactName(),
                                "Contacto WhatsApp",
                                false,
                                true))))
                .id();
    }
    var booking =
        appointments.createByAgent(
            new AppointmentRequest(
                patient,
                slot.dentistId(),
                slot.serviceId(),
                "",
                null,
                slot.localStart(),
                "Propuesta del agente " + p.id(),
                p.id()),
            message.source().equals("APP_TEST"));
    runs.confirmed(p.id(), booking.id(), message.id());
    audit.recordAs(
        null,
        "Agente IA",
        "AGENT_BOOKING_CONFIRMED",
        "AGENT_PROPOSAL",
        p.id(),
        "Guardó cita y confirmación vinculadas.");
    Map<String, Object> result =
        Map.of(
            "appointment_id",
            booking.id(),
            "patient_id",
            booking.patientId(),
            "status",
            booking.status(),
            "origin",
            booking.origin(),
            "response",
            "Cita registrada y confirmada. "
                + p.summary()
                + " Referencia: "
                + booking.id()
                + ". Esta respuesta está preparada en el sistema y aún no se envió a WhatsApp.");
    runs.step(
        run.id(),
        "BOOKING",
        "crear_cita_confirmada",
        Map.of("proposal_id", p.id()),
        result,
        "OK",
        clock.instant());
    runs.finish(
        run.id(), "COMPLETED", result.get("response").toString(), null, null, clock.instant());
    return result;
  }
}
