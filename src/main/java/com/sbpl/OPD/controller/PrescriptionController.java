package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.prescription.CreateMedicineCatalogRequestDto;
import com.sbpl.OPD.dto.prescription.CreateTestCatalogRequestDto;
import com.sbpl.OPD.dto.prescription.CreateTestTypeRequestDto;
import com.sbpl.OPD.dto.prescription.PrescriptionCreateRequestDto;
import com.sbpl.OPD.service.prescription.PrescriptionService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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

  @PostMapping("/medicine/create")
  public ResponseEntity<?> createMedicineCatalog(@Valid @RequestBody CreateMedicineCatalogRequestDto dto) {
    return prescriptionService.createMedicineCatalog(dto);
  }

  @GetMapping("/get/all/medicine")
  public ResponseEntity<?> getAllMedicine(@RequestParam Integer pageNo,
                                          @RequestParam Integer pageSize) {
    return prescriptionService.getAllMedicine(pageNo, pageSize);
  }

  @GetMapping("/get/medicine/by-name")
  public ResponseEntity<?> getMedicineByName(@RequestParam String name,
                                             @RequestParam Integer pageNo,
                                             @RequestParam Integer pageSize) {
    return prescriptionService.getMedicineByName(name, pageNo, pageSize);
  }

  @PostMapping("/test/create")
  public ResponseEntity<?> createTestCatalog(@Valid @RequestBody CreateTestCatalogRequestDto dto) {
    return prescriptionService.createTestCatalog(dto);
  }

  @GetMapping("/get/all/test")
  public ResponseEntity<?> getAllTest(@RequestParam(required = false) Long typeId,
                                      @RequestParam Integer pageNo,
                                      @RequestParam Integer pageSize) {
    return prescriptionService.getAllTest(typeId, pageNo, pageSize);
  }

  @GetMapping("/get/test/by-name")
  public ResponseEntity<?> getTestByName(@RequestParam(required = false) Long typeId,
                                         @RequestParam String name,
                                         @RequestParam Integer pageNo,
                                         @RequestParam Integer pageSize) {
    return prescriptionService.getTestByName(typeId, name, pageNo, pageSize);
  }

  @PostMapping("/testType/create")
  public ResponseEntity<?> createTestType(@Valid @RequestBody CreateTestTypeRequestDto dto) {
    return prescriptionService.createTestType(dto);
  }

  @GetMapping("/get/all/testType")
  public ResponseEntity<?> getAllTestType(@RequestParam Integer pageNo,
                                          @RequestParam Integer pageSize) {
    return prescriptionService.getAllTestType(pageNo, pageSize);
  }

}
