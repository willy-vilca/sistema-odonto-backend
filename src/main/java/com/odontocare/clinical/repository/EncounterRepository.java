package com.odontocare.clinical.repository;

import com.odontocare.clinical.model.Encounter;
import java.util.*;
import org.springframework.data.jpa.repository.*;

public interface EncounterRepository
    extends JpaRepository<Encounter, UUID>, JpaSpecificationExecutor<Encounter> {
  boolean existsByAppointmentId(UUID appointmentId);
}
