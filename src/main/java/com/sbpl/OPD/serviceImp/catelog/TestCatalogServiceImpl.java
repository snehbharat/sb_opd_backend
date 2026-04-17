package com.sbpl.OPD.serviceImp.catelog;

import com.sbpl.OPD.dto.catelog.request.TestCatalogRequestDTO;
import com.sbpl.OPD.dto.catelog.request.TestCatalogUpdateDTO;
import com.sbpl.OPD.dto.catelog.response.TestCatalogResponseDTO;
import com.sbpl.OPD.model.catelog.TestCatalog;
import com.sbpl.OPD.repository.catelog.TestCatalogRepo;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.catelog.TestCatalogService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/**
 * Service implementation for managing test catalog operations.
 *
 * @author Rahul Kumar
 */
@Service
public class TestCatalogServiceImpl implements TestCatalogService {

    @Autowired
    private TestCatalogRepo testCatalogRepo;

    @Autowired
    private BaseResponse baseResponse;

    public static final Logger logger = LoggerFactory.getLogger(TestCatalogServiceImpl.class);

    /**
     * Creates a new test in the catalog.
     *
     * @param dtoList the test catalog request DTO containing test details
     * @return ResponseEntity with BaseResponse containing created test details
     */
    @Transactional
    @Override
    public ResponseEntity<?> createTest(List<TestCatalogRequestDTO> dtoList) {
        logger.info("Creating {} test records", dtoList.size());

        try {
            Long currentUserId = DbUtill.getLoggedInUserId();

            List<TestCatalog> testCatalogList = new ArrayList<>();

            for(TestCatalogRequestDTO dto : dtoList){

                if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                    logger.warn("Test name is required");
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Test name is required");
                }

                if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
                    logger.warn("Test category is required");
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Test category is required");
                }

                String testName = dto.getName().trim();
                if (testCatalogRepo.existsByName(testName)) {
                    logger.warn("Test '{}' already exists, skipping", testName);
                    continue;
                }

                TestCatalog testCatalog = new TestCatalog();
                testCatalog.setName(testName);
                testCatalog.setCategory(dto.getCategory().trim());
                testCatalog.setActive(true);
                testCatalog.setCreatedBy(currentUserId);

                testCatalogList.add(testCatalog);
            }


            testCatalogRepo.saveAll(testCatalogList);
            logger.info("Successfully created {} tests", testCatalogList.size());

            return baseResponse.successResponse(
                    testCatalogList.size() + " tests created successfully"
            );
        } catch (Exception e) {
            logger.error("Error in bulk test creation: {}", e.getMessage(), e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create tests: " + e.getMessage()
            );
        }
    }

    /**
     * Updates an existing test in the catalog.
     *
     * @param id  the ID of the test to update
     * @param dto the test update DTO containing updated details
     * @return ResponseEntity with BaseResponse containing updated test details
     */
    @Override
    @Transactional
    public ResponseEntity<?> updateTest(Long id, TestCatalogUpdateDTO dto) {
        logger.info("Updating test with ID: {}", id);

        try {

            Long currentUserId = DbUtill.getLoggedInUserId();
            TestCatalog existingTest = testCatalogRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));

            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                logger.warn("Test name is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Test name is required");
            }

            if (dto.getCategory() == null || dto.getCategory().trim().isEmpty()) {
                logger.warn("Test category is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Test category is required");
            }

            String testName = dto.getName().trim();
            if (testCatalogRepo.existsByName(testName)) {
                TestCatalog existingByName = testCatalogRepo.findByName(testName).orElse(null);
                if (existingByName != null && !existingByName.getId().equals(id)) {
                    logger.warn("Another test with name '{}' already exists", testName);
                    return baseResponse.errorResponse(HttpStatus.CONFLICT,
                            "Another test with name '" + testName + "' already exists");
                }
            }

            existingTest.setName(testName);
            existingTest.setCategory(dto.getCategory().trim());

            existingTest.setUpdatedBy(currentUserId);

            TestCatalog updatedTest = testCatalogRepo.save(existingTest);
            logger.info("Test updated successfully with ID: {}", updatedTest.getId());

            return baseResponse.successResponse("Test updated successfully");

        } catch (RuntimeException e) {
            logger.warn("Test not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while updating test with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update test: " + e.getMessage());
        }
    }

    /**
     * Retrieves a test by its ID.
     *
     * @param id the ID of the test
     * @return ResponseEntity with BaseResponse containing test details
     */
    @Override
    public ResponseEntity<?> getById(Long id) {
        logger.info("Fetching test with ID: {}", id);

        try {
            TestCatalog test = testCatalogRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));

            TestCatalogResponseDTO responseDTO = convertToResponseDTO(test);
            return baseResponse.successResponse("Test found successfully", responseDTO);

        } catch (RuntimeException e) {
            logger.warn("Test not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while fetching test with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch test: " + e.getMessage());
        }
    }

    /**
     * Retrieves all tests with pagination support.
     *
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated tests
     */
    @Override
    public ResponseEntity<?> getAll(Integer pageNo, Integer pageSize) {
        logger.info("Fetching all tests [page={}, size={}]", pageNo, pageSize);

        try {

            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name"));
            Page<TestCatalog> testPage = testCatalogRepo.findAll(pageRequest);

            Page<TestCatalogResponseDTO> responsePage = testPage.map(this::convertToResponseDTO);

            logger.info("Successfully fetched {} tests out of total {}",
                    responsePage.getContent().size(), responsePage.getTotalElements());

            return baseResponse.successResponse("Tests fetched successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while fetching all tests: {}", e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch tests: " + e.getMessage());
        }
    }

    /**
     * Searches tests by name with pagination support.
     *
     * @param search   the search term
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated search results
     */
    @Override
    public ResponseEntity<?> searchTests(String search, Integer pageNo, Integer pageSize) {
        logger.info("Searching tests [search={}, page={}, size={}]", search, pageNo, pageSize);

        try {
            // Validate search term
            if (search == null || search.trim().isEmpty()) {
                logger.warn("Search term is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Search term is required");
            }

            // Default values
            if (pageNo == null) pageNo = 0;
            if (pageSize == null) pageSize = 10;

            // Validate page size
            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name"));
            Page<TestCatalog> testPage = testCatalogRepo.findByNameContainingIgnoreCase(search.trim(), pageRequest);

            Page<TestCatalogResponseDTO> responsePage = testPage.map(this::convertToResponseDTO);

            logger.info("Successfully found {} tests matching '{}'",
                    responsePage.getContent().size(), search);

            return baseResponse.successResponse("Tests search completed successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while searching tests [search={}]: {}",
                    search, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search tests: " + e.getMessage());
        }
    }

    /**
     * Retrieves tests by category with pagination support.
     *
     * @param category the category to filter by (e.g., "Radiology", "Pathology")
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated tests by category
     */
    @Override
    public ResponseEntity<?> getTestsByCategory(String category, Integer pageNo, Integer pageSize) {
        logger.info("Fetching tests by category [category={}, page={}, size={}]", category, pageNo, pageSize);

        try {
            // Validate category
            if (category == null || category.trim().isEmpty()) {
                logger.warn("Category is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Category is required");
            }

            // Default values
            if (pageNo == null) pageNo = 0;
            if (pageSize == null) pageSize = 10;

            // Validate page size
            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name"));
            Page<TestCatalog> testPage = testCatalogRepo.findByIsActiveTrueAndCategoryIgnoreCase(
                    category.trim(), pageRequest);

            List<TestCatalogResponseDTO> responsePage = testPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully found {} tests in category '{}'",
                    responsePage.size(), category);

            return baseResponse.successResponse("Tests filtered by category successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while fetching tests by category [category={}]: {}",
                    category, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch tests by category: " + e.getMessage());
        }
    }

    /**
     * Retrieves only radiology tests with pagination support.
     * This is a specialized method to fetch tests where category contains "Radiology"
     * (matches both "Radiology" and "Radiology & Imaging").
     *
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated radiology tests
     */
    @Override
    public ResponseEntity<?> getRadiologyTests(Integer pageNo, Integer pageSize) {
        logger.info("Fetching radiology tests (any Radiology category) [page={}, size={}]", pageNo, pageSize);

        try {
            
            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name"));
            
            Page<TestCatalog> testPage = testCatalogRepo.findByIsActiveTrueAndCategoryContainingIgnoreCase(
                    "Radiology", pageRequest);

            List<TestCatalogResponseDTO> responsePage = testPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully found {} radiology tests from all Radiology-related categories", responsePage.size());

            return baseResponse.successResponse("Radiology tests retrieved successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while fetching radiology tests: {}", e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch radiology tests: " + e.getMessage());
        }
    }

    /**
     * Searches radiology tests by name with pagination support.
     * This method searches only within tests that have "Radiology" in their category.
     *
     * @param search   the search term to match against test names
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated search results
     */
    @Override
    public ResponseEntity<?> searchRadiologyTests(String search, Integer pageNo, Integer pageSize) {
        logger.info("Searching radiology tests [search={}, page={}, size={}]", search, pageNo, pageSize);

        try {

            // Validate page size
            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name"));
            
            // Search for tests with name containing search term AND category containing "Radiology"
            Page<TestCatalog> testPage = testCatalogRepo.findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(
                    search.trim(), "Radiology", pageRequest);

            List<TestCatalogResponseDTO> responsePage = testPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully found {} radiology tests matching '{}'",
                    responsePage.size(), search);

            return baseResponse.successResponse("Radiology tests search completed successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while searching radiology tests [search={}]: {}",
                    search, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search radiology tests: " + e.getMessage());
        }
    }

    /**
     * Deletes a test by its ID.
     *
     * @param id the ID of the test to delete
     * @return ResponseEntity with BaseResponse containing deletion status
     */
    @Override
    @Transactional
    public ResponseEntity<?> deleteTest(Long id) {
        logger.info("Deleting test with ID: {}", id);

        try {
            TestCatalog existingTest = testCatalogRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));

            testCatalogRepo.delete(existingTest);

            logger.info("Test deleted successfully with ID: {}", id);

            return baseResponse.successResponse("Test deleted successfully");

        } catch (RuntimeException e) {
            logger.warn("Test not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while deleting test with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete test: " + e.getMessage());
        }
    }

    /**
     * Toggles the active status of a test.
     *
     * @param id the ID of the test to toggle
     * @return ResponseEntity with BaseResponse containing updated test status
     */
    @Override
    @Transactional
    public ResponseEntity<?> toggleActiveStatus(Long id) {
        logger.info("Toggling active status for test with ID: {}", id);

        try {
            TestCatalog existingTest = testCatalogRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test not found with ID: " + id));

            existingTest.setActive(!existingTest.isActive());
            TestCatalog updatedTest = testCatalogRepo.save(existingTest);

            logger.info("Test active status toggled to: {} for ID: {}", updatedTest.isActive(), id);

            TestCatalogResponseDTO responseDTO = convertToResponseDTO(updatedTest);
            return baseResponse.successResponse("Test status updated successfully", responseDTO);

        } catch (RuntimeException e) {
            logger.warn("Test not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while toggling test status with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update test status: " + e.getMessage());
        }
    }

    /**
     * Converts TestCatalog entity to TestCatalogResponseDTO.
     *
     * @param test the test catalog entity
     * @return the response DTO
     */
    private TestCatalogResponseDTO convertToResponseDTO(TestCatalog test) {
        TestCatalogResponseDTO dto = new TestCatalogResponseDTO();
        dto.setId(test.getId());
        dto.setName(test.getName());
        dto.setCategory(test.getCategory());
        dto.setActive(test.isActive());
        return dto;
    }
}
