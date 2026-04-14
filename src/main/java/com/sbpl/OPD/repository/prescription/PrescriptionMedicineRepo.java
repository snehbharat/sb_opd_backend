package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.PrescriptionMedicine;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Prescription Medicine Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface PrescriptionMedicineRepo extends JpaRepository<PrescriptionMedicine, Long> {
}
