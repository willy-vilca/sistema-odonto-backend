package com.odontocare.appointments.service;

import com.odontocare.appointments.model.*;
import com.odontocare.appointments.repository.AppointmentHistoryRepository;
import com.odontocare.security.model.AccountPrincipal;
import java.time.Instant;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;

@Service
public class AppointmentHistoryService {
  private final AppointmentHistoryRepository history;

  public AppointmentHistoryService(AppointmentHistoryRepository history) {
    this.history = history;
  }

  @Transactional(propagation = Propagation.MANDATORY)
  public void append(
      Appointment appointment,
      String action,
      AppointmentStatus previous,
      Instant previousStart,
      String reason) {
    var entry = new AppointmentHistory();
    entry.setAppointment(appointment);
    entry.setAction(action);
    entry.setPreviousStatus(previous == null ? null : previous.name());
    entry.setStatus(appointment.getStatus().name());
    entry.setPreviousStart(previousStart);
    entry.setStartsAt(appointment.getStartsAt());
    entry.setEndsAt(appointment.getEndsAt());
    entry.setDentistId(appointment.getDentist().getId());
    entry.setDentistName(appointment.getDentistName());
    entry.setDurationMinutes(appointment.getDurationMinutes());
    var authentication = SecurityContextHolder.getContext().getAuthentication();
    entry.setActorName(
        authentication != null
                && authentication.getPrincipal() instanceof AccountPrincipal principal
            ? principal.getDisplayName()
            : "Sistema");
    entry.setReason(reason);
    history.save(entry);
  }
}
