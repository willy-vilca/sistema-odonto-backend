package com.odontocare.patients.repository;

import com.odontocare.patients.model.Patient;
import java.util.UUID;
import org.springframework.data.jpa.repository.*;

public interface PatientRepository
    extends JpaRepository<Patient, UUID>, JpaSpecificationExecutor<Patient> {
  boolean existsByCode(String code);

  boolean existsByDocumentTypeAndDocumentNumberAndIdNot(String type, String number, UUID id);
}
