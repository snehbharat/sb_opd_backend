package com.sbpl.OPD.service.catelog;

import com.sbpl.OPD.dto.catelog.request.MedicineCatalogRequestDTO;
import com.sbpl.OPD.dto.catelog.request.MedicineCatalogUpdateDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.data.domain.Pageable;

/**
 * Service interface for managing medicine catalog operations.
 *
 * @author Rahul Kumar
 */
public interface MedicineCatalogService {

    ResponseEntity<?> createMedicine(MedicineCatalogRequestDTO dto);

    ResponseEntity<?> updateMedicine(Long id, MedicineCatalogUpdateDTO dto);

    ResponseEntity<?> getById(Long id);

    ResponseEntity<?> getAllActive(Integer pageNo, Integer pageSize);

    ResponseEntity<?> searchMedicines(String search, Integer pageNo, Integer pageSize);

    ResponseEntity<?> deactivateMedicine(Long id);
}