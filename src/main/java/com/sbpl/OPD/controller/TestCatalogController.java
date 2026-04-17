package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.catelog.request.TestCatalogRequestDTO;
import com.sbpl.OPD.dto.catelog.request.TestCatalogUpdateDTO;
import com.sbpl.OPD.service.catelog.TestCatalogService;
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

import java.util.List;

/**
 * REST controller for managing test catalog operations.
 * Provides endpoints for CRUD operations, search, pagination, and status toggling.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/tests")
public class TestCatalogController {

    private static final Logger logger = LoggerFactory.getLogger(TestCatalogController.class);

    @Autowired
    private TestCatalogService testCatalogService;

    /**
     * Creates a new test in the catalog.
     *
     * @param dtoList the test catalog request DTO
     * @return ResponseEntity with created test details
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTest(@Valid @RequestBody List<TestCatalogRequestDTO> dtoList) {
        logger.info("REST API - Creating {} tests", dtoList.size());
        return testCatalogService.createTest(dtoList);
    }

    /**
     * Updates an existing test in the catalog.
     *
     * @param id  the ID of the test to update
     * @param dto the test update DTO
     * @return ResponseEntity with updated test details
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTest(@PathVariable Long id,
                                        @Valid @RequestBody TestCatalogUpdateDTO dto) {
        logger.info("REST API - Updating test with ID: {}", id);
        return testCatalogService.updateTest(id, dto);
    }

    /**
     * Retrieves a test by its ID.
     *
     * @param id the ID of the test
     * @return ResponseEntity with test details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTestById(@PathVariable Long id) {
        logger.info("REST API - Fetching test with ID: {}", id);
        return testCatalogService.getById(id);
    }

    /**
     * Retrieves all tests with pagination support.
     *
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated tests
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllTests(
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Fetching all tests [pageNo={}, pageSize={}]", pageNo, pageSize);
        return testCatalogService.getAll(pageNo, pageSize);
    }

    /**
     * Searches tests by name with pagination support.
     *
     * @param searchKey   the search term to match against test names
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchTests(
            @RequestParam String searchKey,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Searching tests [search={}, pageNo={}, pageSize={}]",
                searchKey, pageNo, pageSize);
        return testCatalogService.searchTests(searchKey, pageNo, pageSize);
    }

    /**
     * Retrieves tests by category with pagination support.
     * Use this to filter tests by specific categories like "Radiology", "Pathology", etc.
     *
     * @param category the category to filter by (case-insensitive)
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated tests filtered by category
     */
    @GetMapping("/category/{category}")
    public ResponseEntity<?> getTestsByCategory(
            @PathVariable String category,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Fetching tests by category [category={}, pageNo={}, pageSize={}]",
                category, pageNo, pageSize);
        return testCatalogService.getTestsByCategory(category, pageNo, pageSize);
    }

    /**
     * Retrieves only radiology tests with pagination support.
     * This is a specialized endpoint to fetch tests where category contains "Radiology".
     *
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated radiology tests
     */
    @GetMapping("/radiology")
    public ResponseEntity<?> getRadiologyTests(
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Fetching radiology tests [pageNo={}, pageSize={}]", pageNo, pageSize);
        return testCatalogService.getRadiologyTests(pageNo, pageSize);
    }

    /**
     * Searches radiology tests by name with pagination support.
     * This endpoint searches only within tests that have "Radiology" in their category.
     *
     * @param searchKey   the search term to match against test names
     * @param pageNo   the page number (0-based, default: 0)
     * @param pageSize the page size (default: 10)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/radiology/search")
    public ResponseEntity<?> searchRadiologyTests(
            @RequestParam String searchKey,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {
        
        logger.info("REST API - Searching radiology tests [search={}, pageNo={}, pageSize={}]",
                searchKey, pageNo, pageSize);
        return testCatalogService.searchRadiologyTests(searchKey, pageNo, pageSize);
    }

    /**
     * Deletes a test by its ID.
     *
     * @param id the ID of the test to delete
     * @return ResponseEntity with deletion status
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteTest(@PathVariable Long id) {
        logger.info("REST API - Deleting test with ID: {}", id);
        return testCatalogService.deleteTest(id);
    }

    /**
     * Toggles the active status of a test (activate/deactivate).
     *
     * @param id the ID of the test to toggle
     * @return ResponseEntity with updated test status
     */
    @PutMapping("/toggle-status/{id}")
    public ResponseEntity<?> toggleTestStatus(@PathVariable Long id) {
        logger.info("REST API - Toggling active status for test with ID: {}", id);
        return testCatalogService.toggleActiveStatus(id);
    }
}
