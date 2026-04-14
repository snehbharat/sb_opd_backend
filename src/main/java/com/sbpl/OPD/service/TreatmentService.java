package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.treatment.TreatmentCreateDTO;
import org.springframework.http.ResponseEntity;

import java.util.List;

public interface TreatmentService {

  ResponseEntity<?> getFullPriceList(Long branchId);

  ResponseEntity<?> getByCategory(Long categoryId, Long branchId);

  ResponseEntity<?> search(String keyword, Long branchId);

  ResponseEntity<?> createTreatment(List<TreatmentCreateDTO> dto, Long branchId);

  ResponseEntity<?> getTreatmentById(Long id);
}
