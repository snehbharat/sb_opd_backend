package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.PrescriptionAdvice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Prescription Advice Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface PrescriptionAdviceRepo extends JpaRepository<PrescriptionAdvice, Long> {
}
