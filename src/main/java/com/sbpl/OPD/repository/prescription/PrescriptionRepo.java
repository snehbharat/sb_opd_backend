package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.Prescription;
import com.sbpl.OPD.model.prescription.PrescriptionVersion;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Prescription Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface PrescriptionRepo extends JpaRepository<Prescription, Long> {

  /**
   * clinicId + doctorId + patientId.
   * ORDER BY createdAtMs DESC.
   */
  Page<PrescriptionVersion> findByClinicIdAndDoctorIdAndPatientIdOrderByCreatedAtMsDesc(
      Long clinicId,
      Long doctorId,
      Long patientId,
      Pageable pageable
  );

  /**
   * clinicId + doctorId.
   * ORDER BY createdAtMs DESC.
   */
  Page<PrescriptionVersion> findByClinicIdAndDoctorIdOrderByCreatedAtMsDesc(
      Long clinicId,
      Long doctorId,
      Pageable pageable
  );

  /**
   * ORDER BY createdAtMs DESC.
   */
  Page<PrescriptionVersion> findByDoctorIdOrderByCreatedAtMsDesc(
      Long doctorId,
      Pageable pageable
  );

  /**
   * ORDER BY createdAtMs DESC.
   */
  Page<PrescriptionVersion> findByClinicIdOrderByCreatedAtMsDesc(
      Long clinicId,
      Pageable pageable
  );

}
