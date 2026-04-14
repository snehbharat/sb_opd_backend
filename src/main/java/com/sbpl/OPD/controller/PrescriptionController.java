package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.prescription.PrescriptionCreateRequestDto;
import com.sbpl.OPD.service.prescription.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * This Is A Prescription Controller.
 *
 * @author Kousik Manik
 */
@RestController
@RequestMapping("/api/v1/prescription")
public class PrescriptionController {

  @Autowired
  private PrescriptionService prescriptionService;

  @PostMapping("/create")
  public ResponseEntity<?> createPrescription(@Valid @RequestBody PrescriptionCreateRequestDto request) {
    return prescriptionService.createPrescription(request);
  }

}
