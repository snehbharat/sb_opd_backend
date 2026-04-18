package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.catelog.request.TestCategoryRequestDTO;
import com.sbpl.OPD.service.catelog.TestCategoryService;
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
 * REST controller for managing test category operations.
 * Provides endpoints for CRUD operations, search, and pagination.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/test-categories")
public class TestCategoryController {

    private static final Logger logger = LoggerFactory.getLogger(TestCategoryController.class);

    @Autowired
    private TestCategoryService testCategoryService;

    /**
     * Creates a new test category.
     *
     * @param requestDTO the test category request DTO containing category details
     * @return ResponseEntity with created category details
     */
    @PostMapping("/create")
    public ResponseEntity<?> createTestCategory(@Valid @RequestBody TestCategoryRequestDTO requestDTO) {
        logger.info("REST API - Creating test category with name: {}", requestDTO.getCategoryName());
        return testCategoryService.createTestCategory(requestDTO);
    }

    /**
     * Updates an existing test category.
     *
     * @param id         the ID of the category to update
     * @param requestDTO the test category request DTO containing updated details
     * @return ResponseEntity with updated category details
     */
    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateTestCategory(@PathVariable Long id,
                                                @Valid @RequestBody TestCategoryRequestDTO requestDTO) {
        logger.info("REST API - Updating test category with ID: {}", id);
        return testCategoryService.updateTestCategory(id, requestDTO);
    }

    /**
     * Retrieves a test category by its ID.
     *
     * @param id the ID of the category
     * @return ResponseEntity with category details
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getTestCategoryById(@PathVariable Long id) {
        logger.info("REST API - Fetching test category with ID: {}", id);
        return testCategoryService.getTestCategoryById(id);
    }

    /**
     * Retrieves all test categories with pagination support.
     *
     * @param pageNo   the page number (0-based)
     * @param pageSize the page size (1-100)
     * @return ResponseEntity with paginated categories
     */
    @GetMapping("/all")
    public ResponseEntity<?> getAllTestCategories(
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {

        logger.info("REST API - Fetching all test categories [pageNo={}, pageSize={}]", pageNo, pageSize);
        return testCategoryService.getAllTestCategories(pageNo, pageSize);
    }

    /**
     * Searches test categories by name with pagination support.
     *
     * @param searchKey the search term to match against category names
     * @param pageNo    the page number (0-based)
     * @param pageSize  the page size (1-100)
     * @return ResponseEntity with paginated search results
     */
    @GetMapping("/search")
    public ResponseEntity<?> searchTestCategories(
            @RequestParam String searchKey,
            @RequestParam Integer pageNo,
            @RequestParam Integer pageSize) {

        logger.info("REST API - Searching test categories [searchKey={}, pageNo={}, pageSize={}]",
                searchKey, pageNo, pageSize);
        return testCategoryService.searchTestCategories(searchKey, pageNo, pageSize);
    }
}
