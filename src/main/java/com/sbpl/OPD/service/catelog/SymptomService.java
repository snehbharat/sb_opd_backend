package com.sbpl.OPD.service.catelog;

import com.sbpl.OPD.dto.catelog.request.SymptomRequestDTO;
import com.sbpl.OPD.dto.catelog.request.SymptomUpdateDTO;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for managing symptom catalog operations.
 *
 * @author Rahul Kumar
 */
public interface SymptomService {

    ResponseEntity<?> createSymptom(SymptomRequestDTO dto);

    ResponseEntity<?> updateSymptom(Long id, SymptomUpdateDTO dto);

    ResponseEntity<?> getById(Long id);

    ResponseEntity<?> getAll(Integer pageNo, Integer pageSize);

    ResponseEntity<?> searchSymptoms(String search, Integer pageNo, Integer pageSize);

    ResponseEntity<?> deleteSymptom(Long id);
}
