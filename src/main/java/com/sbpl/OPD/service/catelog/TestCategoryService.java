package com.sbpl.OPD.service.catelog;

import com.sbpl.OPD.dto.catelog.request.TestCategoryRequestDTO;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for managing test category operations.
 *
 * @author Rahul Kumar
 */
public interface TestCategoryService {

    /**
     * Creates a new test category.
     *
     * @param requestDTO the test category request DTO containing category details
     * @return ResponseEntity with response containing created category details
     */
    ResponseEntity<?> createTestCategory(TestCategoryRequestDTO requestDTO);

    /**
     * Updates an existing test category.
     *
     * @param id the ID of the category to update
     * @param requestDTO the test category request DTO containing updated details
     * @return ResponseEntity with response containing updated category details
     */
    ResponseEntity<?> updateTestCategory(Long id, TestCategoryRequestDTO requestDTO);

    /**
     * Retrieves a test category by ID.
     *
     * @param id the ID of the category to retrieve
     * @return ResponseEntity with response containing category details
     */
    ResponseEntity<?> getTestCategoryById(Long id);

    /**
     * Retrieves all test categories with pagination.
     *
     * @param pageNo the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with response containing paginated categories
     */
    ResponseEntity<?> getAllTestCategories(Integer pageNo, Integer pageSize);

    /**
     * Searches test categories by name with pagination.
     *
     * @param search the search term to match against category names
     * @param pageNo the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with response containing paginated search results
     */
    ResponseEntity<?> searchTestCategories(String search, Integer pageNo, Integer pageSize);

    /**
     * Deletes a test category by ID (soft delete).
     *
     * @param id the ID of the category to delete
     * @return ResponseEntity with response
     */
    ResponseEntity<?> deleteTestCategory(Long id);
}
