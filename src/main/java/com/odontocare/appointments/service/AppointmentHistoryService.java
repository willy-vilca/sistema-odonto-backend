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
      Appointment a,
      String action,
      AppointmentStatus previous,
      Instant previousStart,
      String reason) {
    var h = new AppointmentHistory();
    h.setAppointment(a);
    h.setAction(action);
    h.setPreviousStatus(previous == null ? null : previous.name());
    h.setStatus(a.getStatus().name());
    h.setPreviousStart(previousStart);
    h.setStartsAt(a.getStartsAt());
    h.setEndsAt(a.getEndsAt());
    h.setDentistId(a.getDentist().getId());
    h.setDentistName(a.getDentistName());
    h.setDurationMinutes(a.getDurationMinutes());
    var auth = SecurityContextHolder.getContext().getAuthentication();
    h.setActorName(
        auth != null && auth.getPrincipal() instanceof AccountPrincipal p
            ? p.getDisplayName()
            : "Sistema");
    h.setReason(reason);
    history.save(h);
  }
}
