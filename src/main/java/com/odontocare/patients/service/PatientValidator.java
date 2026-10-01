package com.odontocare.patients.service;

import com.odontocare.patients.dto.PatientRequest;
import com.odontocare.shared.web.ApiException;
import java.time.*;
import java.util.*;
import org.springframework.stereotype.Component;

@Component
public class PatientValidator {
  private final Clock clock;

  public PatientValidator(Clock clock) {
    this.clock = clock;
  }

  public void validate(PatientRequest r, ZoneId zone) {
    if (!r.provisional() && r.birthDate() == null)
      throw ApiException.badRequest(
          "Completa la fecha de nacimiento o marca la ficha como provisional.");
    if (r.birthDate() != null && r.birthDate().isAfter(LocalDate.now(clock.withZone(zone))))
      throw ApiException.badRequest("La fecha de nacimiento no puede ser futura.");
    if (r.birthDate() != null
        && r.birthDate().isAfter(LocalDate.now(clock.withZone(zone)).minusYears(18))
        && r.contacts().stream().noneMatch(PatientRequest.ContactRequest::guardian))
      throw ApiException.badRequest("Un menor necesita un contacto identificado como responsable.");
    if (r.documentType().isBlank() != r.documentNumber().isBlank())
      throw ApiException.badRequest("Indica el tipo y el número del documento juntos.");
    if (r.contacts().stream().map(PatientRequest.ContactRequest::phone).distinct().count()
        != r.contacts().size())
      throw ApiException.badRequest("No repitas un teléfono dentro de la misma ficha.");
  }

  public String duplicateKey(PatientRequest r) {

    return r.fullName().strip().replaceAll("\\s+", " ").toLowerCase(Locale.ROOT)
        + "|"
        + r.birthDate()
        + "|"
        + r.contacts().stream()
            .map(PatientRequest.ContactRequest::phone)
            .sorted()
            .findFirst()
            .orElse("");
  }
}
