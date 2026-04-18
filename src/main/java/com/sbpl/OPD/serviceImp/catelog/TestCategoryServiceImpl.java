package com.sbpl.OPD.serviceImp.catelog;

import com.sbpl.OPD.dto.catelog.request.TestCategoryRequestDTO;
import com.sbpl.OPD.dto.catelog.response.TestCategoryResponseDTO;
import com.sbpl.OPD.model.catelog.TestCategory;
import com.sbpl.OPD.repository.catelog.TestCategoryRepo;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.catelog.TestCategoryService;
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

import java.util.List;

/**
 * Service implementation for managing test category operations.
 * Provides CRUD operations, search, and pagination functionality.
 *
 * @author Rahul Kumar
 */
@Service
public class TestCategoryServiceImpl implements TestCategoryService {

    @Autowired
    private TestCategoryRepo testCategoryRepo;

    @Autowired
    private BaseResponse baseResponse;

    public static final Logger logger = LoggerFactory.getLogger(TestCategoryServiceImpl.class);

    /**
     * Creates a new test category.
     *
     * @param requestDTO the test category request DTO containing category details
     * @return ResponseEntity with response containing created category details
     */
    @Transactional
    @Override
    public ResponseEntity<?> createTestCategory(TestCategoryRequestDTO requestDTO) {
        logger.info("Creating new test category with name: {}", requestDTO.getCategoryName());

        try {
            if (requestDTO.getCategoryName() == null || requestDTO.getCategoryName().trim().isEmpty()) {
                logger.warn("Category name is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Category name is required");
            }

            String categoryName = requestDTO.getCategoryName().trim();

            boolean exists = testCategoryRepo.existsByCategoryNameIgnoreCase(categoryName);
            if (exists) {
                logger.warn("Test category '{}' already exists", categoryName);
                return baseResponse.errorResponse(HttpStatus.CONFLICT,
                        "Test category '" + categoryName + "' already exists");
            }

            TestCategory testCategory = new TestCategory();
            testCategory.setCategoryName(categoryName);
            testCategory.setCategoryDescription(
                    requestDTO.getCategoryDescription() != null ?
                    requestDTO.getCategoryDescription().trim() : null
            );
            testCategory.setCreatedBy(DbUtill.getLoggedInUserId());

            TestCategory savedCategory = testCategoryRepo.save(testCategory);
            logger.info("Test category created successfully with ID: {}", savedCategory.getId());

            return baseResponse.successResponse("Test category created successfully");

        } catch (Exception e) {
            logger.error("Error occurred while creating test category: {}", e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create test category: " + e.getMessage());
        }
    }

    /**
     * Updates an existing test category.
     *
     * @param id the ID of the category to update
     * @param requestDTO the test category request DTO containing updated details
     * @return ResponseEntity with response containing updated category details
     */
    @Transactional
    @Override
    public ResponseEntity<?> updateTestCategory(Long id, TestCategoryRequestDTO requestDTO) {
        logger.info("Updating test category with ID: {}", id);

        try {
            TestCategory existingCategory = testCategoryRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test category not found with ID: " + id));

            if (requestDTO.getCategoryName() != null && !requestDTO.getCategoryName().trim().isEmpty()) {
                String categoryName = requestDTO.getCategoryName().trim();
                
                boolean exists = testCategoryRepo.existsByCategoryNameIgnoreCaseAndIdNot(categoryName, id);
                if (exists) {
                    logger.warn("Another test category with name '{}' already exists", categoryName);
                    return baseResponse.errorResponse(HttpStatus.CONFLICT,
                            "Another test category with name '" + categoryName + "' already exists");
                }
                
                existingCategory.setCategoryName(categoryName);
            }
            
            if (requestDTO.getCategoryDescription() != null) {
                existingCategory.setCategoryDescription(requestDTO.getCategoryDescription().trim());
            }
            
            existingCategory.setUpdatedBy(DbUtill.getLoggedInUserId());
            
            TestCategory updatedCategory = testCategoryRepo.save(existingCategory);
            logger.info("Test category updated successfully with ID: {}", updatedCategory.getId());
            
            return baseResponse.successResponse("Test category updated successfully");

        } catch (RuntimeException e) {
            logger.warn("Test category not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while updating test category with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update test category: " + e.getMessage());
        }
    }

    /**
     * Retrieves a test category by ID.
     *
     * @param id the ID of the category to retrieve
     * @return ResponseEntity with response containing category details
     */
    @Override
    public ResponseEntity<?> getTestCategoryById(Long id) {
        logger.info("Fetching test category with ID: {}", id);

        try {
            TestCategory testCategory = testCategoryRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test category not found with ID: " + id));

            TestCategoryResponseDTO responseDTO = convertToResponseDTO(testCategory);
            logger.info("Successfully fetched test category with ID: {}", id);

            return baseResponse.successResponse("Test category fetched successfully", responseDTO);

        } catch (RuntimeException e) {
            logger.warn("Test category not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while fetching test category with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch test category: " + e.getMessage());
        }
    }

    /**
     * Retrieves all test categories with pagination.
     *
     * @param pageNo the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with response containing paginated categories
     */
    @Override
    public ResponseEntity<?> getAllTestCategories(Integer pageNo, Integer pageSize) {
        logger.info("Fetching all test categories with pageNo: {}, pageSize: {}", pageNo, pageSize);

        try {

            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize,
                    Sort.by(Sort.Direction.ASC, "categoryName"));
            Page<TestCategory> categoryPage = testCategoryRepo.findAll(pageRequest);

            List<TestCategoryResponseDTO> responsePage = categoryPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully fetched {} test categories", responsePage.size());

            return baseResponse.successResponse("Test categories fetched successfully",
                    DbUtill.buildPaginatedResponse(categoryPage,responsePage));

        } catch (Exception e) {
            logger.error("Error occurred while fetching all test categories: {}", e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch test categories: " + e.getMessage());
        }
    }

    /**
     * Searches test categories by name with pagination.
     *
     * @param search the search term to match against category names
     * @param pageNo the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with response containing paginated search results
     */
    @Override
    public ResponseEntity<?> searchTestCategories(String search, Integer pageNo, Integer pageSize) {
        logger.info("Searching test categories with search term: '{}', pageNo: {}, pageSize: {}",
                search, pageNo, pageSize);

        try {
            if (search == null || search.trim().isEmpty()) {
                logger.warn("Search term is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Search term is required");
            }

            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize,
                    Sort.by(Sort.Direction.ASC, "categoryName"));
            Page<TestCategory> categoryPage = testCategoryRepo
                    .findByCategoryNameContainingIgnoreCase(search.trim(), pageRequest);

            List<TestCategoryResponseDTO> responsePage = categoryPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully found {} test categories matching '{}",
                    responsePage.size(), search);

            return baseResponse.successResponse("Test category search completed successfully",
                    DbUtill.buildPaginatedResponse(categoryPage,responsePage));

        } catch (Exception e) {
            logger.error("Error occurred while searching test categories [search={}]: {}",
                    search, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search test categories: " + e.getMessage());
        }
    }

    /**
     * Deletes a test category by ID (soft delete).
     *
     * @param id the ID of the category to delete
     * @return ResponseEntity with response
     */
    @Transactional
    @Override
    public ResponseEntity<?> deleteTestCategory(Long id) {
        logger.info("Deleting test category with ID: {}", id);

        try {
            // Check if category exists
            TestCategory testCategory = testCategoryRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Test category not found with ID: " + id));

            // Delete the category
            testCategoryRepo.delete(testCategory);
            logger.info("Test category deleted successfully with ID: {}", id);

            return baseResponse.successResponse("Test category deleted successfully");

        } catch (RuntimeException e) {
            logger.warn("Test category not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while deleting test category with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete test category: " + e.getMessage());
        }
    }

    /**
     * Converts TestCategory entity to TestCategoryResponseDTO.
     *
     * @param testCategory the test category entity
     * @return the response DTO
     */
    private TestCategoryResponseDTO convertToResponseDTO(TestCategory testCategory) {
        TestCategoryResponseDTO dto = new TestCategoryResponseDTO();
        dto.setId(testCategory.getId());
        dto.setCategoryName(testCategory.getCategoryName());
        dto.setCategoryDescription(testCategory.getCategoryDescription());
        dto.setIsActive(true);
        return dto;
    }
}
