package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.catelog.request.SymptomRequestDTO;
import com.sbpl.OPD.dto.catelog.request.SymptomUpdateDTO;
import com.sbpl.OPD.service.catelog.SymptomService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing symptom catalog operations.
 * Provides endpoints for CRUD operations, search, and pagination.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/symptoms")
public class SymptomController {

    private static final Logger logger = LoggerFactory.getLogger(SymptomController.class);

    @Autowired
    private SymptomService symptomService;

    /**
     * Creates a new symptom in the catalog.
     *
     * @param dto the symptom request DTO
     * @return ResponseEntity with created symptom details
     */
    @PostMapping("/create")
    public ResponseEntity<?> createSymptom(@Valid @RequestBody SymptomRequestDTO dto) {
        logger.info("REST API - Creating new symptom [name={}]", dto.getName());
        return symptomService.createSymptom(dto);
    }

    /**
     * Updates an existing symptom in the catalog.
     *
     * @param id  the ID of the symptom to update
     * @param dto the symptom update DTO
     * @return ResponseEntity with updated symptom details
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateSymptom(@PathVariable Long id,
                                           @Valid @RequestBody SymptomUpdateDTO dto) {
        logger.info("REST API - Updating symptom with ID: {}", id);
        return symptomService.updateSymptom(id, dto);
    }

    /**
     * Retrieves a symptom by its ID.
     *
     * @param id the ID of the symptom
     * @return ResponseEntity with symptom details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getSymptomById(@PathVariable Long id) {
        logger.info("REST API - Fetching symptom with ID: {}", id);
        return symptomService.getById(id);
    }

    /**
     * Retrieves all symptoms with pagination support.
     *
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated symptoms
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllSymptoms(
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Fetching all symptoms [pageNo={}, pageSize={}]", pageNo, pageSize);
        return symptomService.getAll(pageNo, pageSize);
    }

    /**
     * Searches symptoms by name with pagination support.
     *
     * @param searchKey   the search term to match against symptom names
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchSymptoms(
            @RequestParam String searchKey,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Searching symptoms [search={}, pageNo={}, pageSize={}]",
                searchKey, pageNo, pageSize);
        return symptomService.searchSymptoms(searchKey, pageNo, pageSize);
    }

}
