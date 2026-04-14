package com.sbpl.OPD.service.prescription;

import com.sbpl.OPD.dto.prescription.PrescriptionCreateRequestDto;
import org.springframework.http.ResponseEntity;

/**
 * This Is An Prescription Service interface class.
 *
 * @author Kousik Manik
 */
public interface PrescriptionService {
  public ResponseEntity<?> createPrescription(PrescriptionCreateRequestDto request);
}
