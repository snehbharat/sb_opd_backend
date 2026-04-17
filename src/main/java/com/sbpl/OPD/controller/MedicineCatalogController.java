package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.catelog.request.MedicineCatalogRequestDTO;
import com.sbpl.OPD.dto.catelog.request.MedicineCatalogUpdateDTO;
import com.sbpl.OPD.service.catelog.MedicineCatalogService;
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
 * REST controller for managing medicine catalog operations.
 * Provides endpoints for CRUD operations, search, and pagination.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/medicine-catalog")
public class MedicineCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(MedicineCatalogController.class);

    @Autowired
    private MedicineCatalogService medicineCatalogService;

    /**
     * Creates a new medicine in the catalog.
     *
     * @param dto the medicine catalog request DTO
     * @return ResponseEntity with created medicine details
     */
    @PostMapping("/create")
    public ResponseEntity<?> createMedicine(@Valid @RequestBody MedicineCatalogRequestDTO dto) {
        logger.info("REST API - Creating new medicine [name={}, brand={}, strength={}]",
            dto.getName(), dto.getBrandName(), dto.getStrength());
        return medicineCatalogService.createMedicine(dto);
    }

    /**
     * Updates an existing medicine in the catalog.
     *
     * @param id  the ID of the medicine to update
     * @param dto the medicine catalog update DTO
     * @return ResponseEntity with updated medicine details
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateMedicine(@PathVariable Long id,
                                                      @Valid @RequestBody MedicineCatalogUpdateDTO dto) {
        logger.info("REST API - Updating medicine with ID: {}", id);
         return medicineCatalogService.updateMedicine(id, dto);
    }

    /**
     * Retrieves a medicine by its ID.
     *
     * @param id the ID of the medicine
     * @return ResponseEntity with medicine details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicineById(@PathVariable Long id) {
        logger.info("REST API - Fetching medicine with ID: {}", id);
        return medicineCatalogService.getById(id);
    }


    /**
     * Retrieves all active medicines using integer-based pagination (alternative endpoint).
     *
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated active medicines
     */
    @GetMapping("/active")
    public ResponseEntity<?> getAllActiveMedicinesWithIntegerPagination(
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Fetching all active medicines with integer pagination [pageNo={}, pageSize={}]",
            pageNo, pageSize);

        return medicineCatalogService.getAllActive(pageNo, pageSize);
    }

    /**
     * Searches medicines by name using integer-based pagination (alternative endpoint).
     *
     * @param searchKey   the search term to match against medicine names
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchMedicinesWithIntegerPagination(
            @RequestParam String searchKey,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Searching medicines with integer pagination [search={}, pageNo={}, pageSize={}]",
                searchKey, pageNo, pageSize);

        return medicineCatalogService.searchMedicines(searchKey, pageNo, pageSize);
    }

    /**
     * Deactivates a medicine by setting its active status to false.
     *
     * @param id the ID of the medicine to deactivate
     * @return ResponseEntity with success or error message
     */
    @DeleteMapping("/deactivate/{id}")
    public ResponseEntity<?> deactivateMedicine(@PathVariable Long id) {
        logger.info("REST API - Deactivating medicine with ID: {}", id);
        return medicineCatalogService.deactivateMedicine(id);
    }
}
