package com.sbpl.OPD.repository.prescription;


import com.sbpl.OPD.model.prescription.PrescriptionNote;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Prescription Not Repository.
 *
 * @author Kousik Manik
 */
@Repository
public interface PrescriptionNoteRepo extends JpaRepository<PrescriptionNote, Long> {
}
