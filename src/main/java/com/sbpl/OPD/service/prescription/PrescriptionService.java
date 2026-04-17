package com.sbpl.OPD.service.prescription;

import com.sbpl.OPD.dto.prescription.CreateMedicineCatalogRequestDto;
import com.sbpl.OPD.dto.prescription.CreateTestCatalogRequestDto;
import com.sbpl.OPD.dto.prescription.CreateTestTypeRequestDto;
import com.sbpl.OPD.dto.prescription.PrescriptionCreateRequestDto;
import org.springframework.http.ResponseEntity;

/**
 * This Is An Prescription Service interface class.
 *
 * @author Kousik Manik
 */
public interface PrescriptionService {
  public ResponseEntity<?> createPrescription(PrescriptionCreateRequestDto request);

  public ResponseEntity<?> createMedicineCatalog(CreateMedicineCatalogRequestDto dto);

  public ResponseEntity<?> getAllMedicine(Integer pageNo, Integer pageSize);

  public ResponseEntity<?> getMedicineByName(String name, Integer pageNo, Integer pageSize);

  public ResponseEntity<?> createTestCatalog(CreateTestCatalogRequestDto dto);

  public ResponseEntity<?> getAllTest(Long typeId, Integer pageNo, Integer pageSize);

  public ResponseEntity<?> getTestByName(Long typeId, String name, Integer pageNo, Integer pageSize);

  public ResponseEntity<?> createTestType(CreateTestTypeRequestDto dto);

  public ResponseEntity<?> getAllTestType(Integer pageNo, Integer pageSize);

}
