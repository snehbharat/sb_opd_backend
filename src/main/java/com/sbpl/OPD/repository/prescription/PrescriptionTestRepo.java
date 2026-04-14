package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.PrescriptionTest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Prescription Test Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface PrescriptionTestRepo extends JpaRepository<PrescriptionTest, Long> {
}
