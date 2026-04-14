package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.VitalSigns;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository for managing vital signs data.
 * Provides database operations for patient vital sign records.
 */
@Repository
public interface VitalSignsRepository extends JpaRepository<VitalSigns, Long> {
    Page<VitalSigns> findByAppointmentId(Long appointmentId, Pageable pageable);
    Page<VitalSigns> findByPatientId(Long patientId, Pageable pageable);
    Optional<VitalSigns> findByAppointmentIdAndPatientId(Long appointmentId, Long patientId);
}