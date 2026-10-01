package com.odontocare.patients.service;

import com.odontocare.audit.service.AuditService;
import com.odontocare.installation.repository.InstallationProfileRepository;
import com.odontocare.patients.dto.*;
import com.odontocare.patients.model.*;
import com.odontocare.patients.repository.*;
import com.odontocare.shared.pagination.*;
import com.odontocare.shared.web.ApiException;
import java.time.ZoneId;
import java.util.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PatientService {
  private final PatientRepository patients;
  private final InstallationProfileRepository profiles;
  private final PatientValidator validator;
  private final AuditService audit;

  public PatientService(
      PatientRepository patients,
      InstallationProfileRepository profiles,
      PatientValidator validator,
      AuditService audit) {
    this.patients = patients;
    this.profiles = profiles;
    this.validator = validator;
    this.audit = audit;
  }

  @Transactional(readOnly = true)
  public PageResponse<PatientResponse> list(
      PageQuery q, Boolean active, Boolean provisional, String phone) {
    Specification<Patient> spec =
        SearchSpecifications.<Patient>text(q.getSearch(), "fullName", "code", "documentNumber")
            .or(contactSearch(q.getSearch()))
            .and(SearchSpecifications.equal("active", active))
            .and(SearchSpecifications.equal("provisional", provisional));
    if (phone != null && !phone.isBlank())
      spec = spec.and((root, query, cb) -> cb.equal(root.join("contacts").get("phone"), phone));
    spec =
        spec.and(
            (root, query, cb) -> {
              query.distinct(true);
              return cb.conjunction();
            });
    return PageResponse.of(
        patients
            .findAll(
                spec,
                q.pageable(Map.of("name", "fullName", "code", "code", "createdAt", "createdAt")))
            .map(PatientResponse::of));
  }

  @Transactional(readOnly = true)
  public PatientResponse get(UUID id) {
    return PatientResponse.of(patients.findById(id).orElseThrow(ApiException::notFound));
  }

  @Transactional
  public PatientResponse create(PatientRequest r) {
    var profile = profiles.lockInstallation().orElseThrow();
    var p = new Patient();
    int number = profile.getPatientNextNumber();
    String code;
    do {
      code = profile.getPatientPrefix() + "-" + String.format(Locale.ROOT, "%06d", number++);
    } while (patients.existsByCode(code));
    profile.setPatientNextNumber(number);
    p.setCode(code);
    apply(p, r, profile.getTimeZone());
    patients.saveAndFlush(p);
    audit.record("PATIENT_CREATED", "PATIENT", p.getId(), "Creó ficha " + code);
    return PatientResponse.of(p);
  }

  @Transactional
  public PatientResponse update(UUID id, PatientRequest r) {
    var profile = profiles.lockInstallation().orElseThrow();
    var p = patients.findById(id).orElseThrow(ApiException::notFound);
    p.checkVersion(r.version());
    apply(p, r, profile.getTimeZone());
    patients.saveAndFlush(p);
    audit.record("PATIENT_UPDATED", "PATIENT", id, "Actualizó ficha " + p.getCode());
    return PatientResponse.of(p);
  }

  private Specification<Patient> contactSearch(String search) {
    return (root, q, cb) -> {
      if (search.isBlank()) return cb.conjunction();
      String pattern =
          "%"
              + search
                  .toLowerCase(Locale.ROOT)
                  .replace("\\", "\\\\")
                  .replace("%", "\\%")
                  .replace("_", "\\_")
              + "%";
      var c = root.join("contacts");
      return cb.or(
          cb.like(cb.lower(c.get("phone")), pattern, '\\'),
          cb.like(cb.lower(c.get("name")), pattern, '\\'));
    };
  }

  private void apply(Patient p, PatientRequest r, String zone) {
    validator.validate(r, ZoneId.of(zone));
    String number = r.documentNumber().strip().toUpperCase(Locale.ROOT);
    String key = validator.duplicateKey(r);
    UUID except = p.getId() == null ? new UUID(0, 0) : p.getId();
    if (!number.isEmpty()
        && patients.existsByDocumentTypeAndDocumentNumberAndIdNot(r.documentType(), number, except))
      throw ApiException.conflict("Ya existe un paciente con ese documento. Busca su ficha.");
    String name = r.fullName().strip().replaceAll("\\s+", " ");
    var phones = r.contacts().stream().map(PatientRequest.ContactRequest::phone).toList();
    Specification<Patient> probable =
        (root, q, cb) -> {
          var contact = root.join("contacts");
          return cb.and(
              cb.notEqual(root.get("id"), except),
              cb.equal(cb.lower(root.get("fullName")), name.toLowerCase(Locale.ROOT)),
              r.birthDate() == null
                  ? cb.isNull(root.get("birthDate"))
                  : cb.equal(root.get("birthDate"), r.birthDate()),
              contact.get("phone").in(phones));
        };
    if (patients.exists(probable))
      throw ApiException.conflict(
          "Ya existe una ficha con el mismo nombre, nacimiento y teléfono. Revisa el paciente antes"
              + " de duplicarlo.");
    p.setFullName(name);
    p.setBirthDate(r.birthDate());
    p.setDocumentType(r.documentType());
    p.setDocumentNumber(number);
    p.setAddress(r.address().strip());
    p.setEmail(r.email().strip());
    p.setEmergencyName(r.emergencyName().strip());
    p.setEmergencyPhone(r.emergencyPhone());
    p.setNotes(r.notes().strip());
    p.setProvisional(r.provisional());
    p.setActive(r.active());
    p.setDuplicateKey(key);
    p.getContacts().clear();
    patients.flush();
    r.contacts()
        .forEach(
            c ->
                p.getContacts()
                    .add(
                        new PatientContact(
                            p,
                            c.phone(),
                            c.name().strip(),
                            c.relationship().strip(),
                            c.guardian(),
                            c.payer())));
  }
}
