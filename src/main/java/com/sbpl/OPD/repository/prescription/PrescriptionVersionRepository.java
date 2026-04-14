package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.PrescriptionVersion;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is An Prescription Version Repository.
 *
 * @author Kousik Manik
 */
@Repository
public interface PrescriptionVersionRepository
    extends JpaRepository<PrescriptionVersion, Long> {

}
