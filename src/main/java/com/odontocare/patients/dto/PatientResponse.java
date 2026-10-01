package com.odontocare.patients.dto;

import com.odontocare.patients.model.Patient;
import java.time.LocalDate;
import java.util.*;

public record PatientResponse(
    UUID id,
    String code,
    String fullName,
    String label,
    LocalDate birthDate,
    String documentType,
    String documentNumber,
    String address,
    String email,
    String emergencyName,
    String emergencyPhone,
    String notes,
    boolean provisional,
    boolean active,
    List<PatientRequest.ContactRequest> contacts,
    long version) {
  public static PatientResponse of(Patient p) {
    return new PatientResponse(
        p.getId(),
        p.getCode(),
        p.getFullName(),
        p.getCode() + " · " + p.getFullName(),
        p.getBirthDate(),
        p.getDocumentType(),
        p.getDocumentNumber(),
        p.getAddress(),
        p.getEmail(),
        p.getEmergencyName(),
        p.getEmergencyPhone(),
        p.getNotes(),
        p.getProvisional(),
        p.getActive(),
        p.getContacts().stream()
            .map(
                c ->
                    new PatientRequest.ContactRequest(
                        c.getPhone(),
                        c.getName(),
                        c.getRelationship(),
                        c.getGuardian(),
                        c.getPayer()))
            .toList(),
        p.getVersion());
  }
}
