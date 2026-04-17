package com.sbpl.OPD.service.catelog;

import com.sbpl.OPD.dto.catelog.request.TestCatalogRequestDTO;
import com.sbpl.OPD.dto.catelog.request.TestCatalogUpdateDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Service interface for managing test catalog operations.
 *
 * @author Rahul Kumar
 */
public interface TestCatalogService {

    ResponseEntity<?> createTest(List<TestCatalogRequestDTO> dtoList);

    ResponseEntity<?> updateTest(Long id, TestCatalogUpdateDTO dto);

    ResponseEntity<?> getById(Long id);

    ResponseEntity<?> getAll(Integer pageNo, Integer pageSize);

    ResponseEntity<?> searchTests(String search, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getTestsByCategory(String category, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getRadiologyTests(Integer pageNo, Integer pageSize);

    ResponseEntity<?> searchRadiologyTests(String search, Integer pageNo, Integer pageSize);

    ResponseEntity<?> deleteTest(Long id);

    ResponseEntity<?> toggleActiveStatus(Long id);
}
