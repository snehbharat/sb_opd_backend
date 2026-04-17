package com.sbpl.OPD.serviceImp.catelog;

import com.sbpl.OPD.dto.catelog.request.SymptomRequestDTO;
import com.sbpl.OPD.dto.catelog.request.SymptomUpdateDTO;
import com.sbpl.OPD.dto.catelog.response.SymptomResponseDTO;
import com.sbpl.OPD.model.catelog.Symptom;
import com.sbpl.OPD.repository.catelog.SymptomRepo;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.catelog.SymptomService;
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
 * Implementation of SymptomService for managing symptom catalog operations.
 * Provides CRUD operations with logging, error handling, and pagination support.
 *
 * @author Rahul Kumar
 */
@Service
public class SymptomServiceImpl implements SymptomService {

    @Autowired
    private SymptomRepo symptomRepo;

    @Autowired
    private BaseResponse baseResponse;

    public static final Logger logger = LoggerFactory.getLogger(SymptomServiceImpl.class);

    /**
     * Creates a new symptom in the catalog.
     *
     * @param dto the symptom request DTO containing symptom details
     * @return ResponseEntity with BaseResponse containing created symptom details
     */
    @Override
    @Transactional
    public ResponseEntity<?> createSymptom(SymptomRequestDTO dto) {
        logger.info("Creating new symptom [name={}]", dto.getName());

        try {
            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                logger.warn("Symptom name is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Symptom name is required");
            }

            String symptomName = dto.getName().trim();
            if (symptomRepo.existsByName(symptomName)) {
                logger.warn("Symptom with name '{}' already exists", symptomName);
                return baseResponse.errorResponse(HttpStatus.CONFLICT, 
                        "Symptom with name '" + symptomName + "' already exists");
            }

            Symptom symptom = new Symptom();
            symptom.setName(symptomName);

            Symptom savedSymptom = symptomRepo.save(symptom);
            logger.info("Symptom created successfully with ID: {}", savedSymptom.getId());

            return baseResponse.successResponse("Symptom created successfully");

        } catch (Exception e) {
            logger.error("Error occurred while creating symptom [name={}]: {}",
                    dto.getName(), e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create symptom: " + e.getMessage());
        }
    }

    /**
     * Updates an existing symptom in the catalog.
     *
     * @param id  the ID of the symptom to update
     * @param dto the symptom update DTO containing updated details
     * @return ResponseEntity with BaseResponse containing updated symptom details
     */
    @Override
    @Transactional
    public ResponseEntity<?> updateSymptom(Long id, SymptomUpdateDTO dto) {
        logger.info("Updating symptom with ID: {}", id);

        try {
            Symptom existingSymptom = symptomRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Symptom not found with ID: " + id));

            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                logger.warn("Symptom name is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Symptom name is required");
            }

            String symptomName = dto.getName().trim();
            if (symptomRepo.existsByName(symptomName)) {
                Symptom existingByName = symptomRepo.findByName(symptomName).orElse(null);
                if (existingByName != null && !existingByName.getId().equals(id)) {
                    logger.warn("Another symptom with name '{}' already exists", symptomName);
                    return baseResponse.errorResponse(HttpStatus.CONFLICT,
                            "Another symptom with name '" + symptomName + "' already exists");
                }
            }

            existingSymptom.setName(symptomName);

            Symptom updatedSymptom = symptomRepo.save(existingSymptom);
            logger.info("Symptom updated successfully with ID: {}", updatedSymptom.getId());

            SymptomResponseDTO responseDTO = convertToResponseDTO(updatedSymptom);
            return baseResponse.successResponse("Symptom updated successfully", responseDTO);

        } catch (RuntimeException e) {
            logger.warn("Symptom not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while updating symptom with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update symptom: " + e.getMessage());
        }
    }

    /**
     * Retrieves a symptom by its ID.
     *
     * @param id the ID of the symptom
     * @return ResponseEntity with BaseResponse containing symptom details
     */
    @Override
    public ResponseEntity<?> getById(Long id) {
        logger.info("Fetching symptom with ID: {}", id);

        try {
            Symptom symptom = symptomRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Symptom not found with ID: " + id));

            SymptomResponseDTO responseDTO = convertToResponseDTO(symptom);
            return baseResponse.successResponse("Symptom found successfully", responseDTO);

        } catch (RuntimeException e) {
            logger.warn("Symptom not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while fetching symptom with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch symptom: " + e.getMessage());
        }
    }

    /**
     * Retrieves all symptoms with pagination support.
     *
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated symptoms
     */
    @Override
    public ResponseEntity<?> getAll(Integer pageNo, Integer pageSize) {
        logger.info("Fetching all symptoms [page={}, size={}]", pageNo, pageSize);

        try {
            if (pageNo == null) pageNo = 0;
            if (pageSize == null) pageSize = 10;

            if (pageSize <= 0 || pageSize > 100) {
                logger.warn("Invalid page size: {}. Must be between 1 and 100", pageSize);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Page size must be between 1 and 100");
            }

            PageRequest pageRequest = PageRequest.of(pageNo, pageSize, Sort.by(Sort.Direction.ASC, "name"));
            Page<Symptom> symptomPage = symptomRepo.findAll(pageRequest);

            List<SymptomResponseDTO> responsePage = symptomPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully fetched {} symptoms out of total {}",
                    responsePage.size(), symptomPage.getTotalElements());

            return baseResponse.successResponse("Symptoms fetched successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while fetching all symptoms: {}", e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch symptoms: " + e.getMessage());
        }
    }

    /**
     * Searches symptoms by name with pagination support.
     *
     * @param search   the search term
     * @param pageNo   the page number (0-based)
     * @param pageSize the number of items per page
     * @return ResponseEntity with BaseResponse containing paginated search results
     */
    @Override
    public ResponseEntity<?> searchSymptoms(String search, Integer pageNo, Integer pageSize) {
        logger.info("Searching symptoms [search={}, page={}, size={}]", search, pageNo, pageSize);

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
            Page<Symptom> symptomPage = symptomRepo.findByNameContainingIgnoreCase(search.trim(), pageRequest);

            List<SymptomResponseDTO> responsePage = symptomPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully found {} symptoms matching '{}'",
                    responsePage.size(), search);

            return baseResponse.successResponse("Symptoms search completed successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while searching symptoms [search={}]: {}",
                    search, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search symptoms: " + e.getMessage());
        }
    }

    /**
     * Deletes a symptom by its ID.
     *
     * @param id the ID of the symptom to delete
     * @return ResponseEntity with BaseResponse containing deletion status
     */
    @Override
    @Transactional
    public ResponseEntity<?> deleteSymptom(Long id) {
        logger.info("Deleting symptom with ID: {}", id);

        try {
            Symptom existingSymptom = symptomRepo.findById(id)
                    .orElseThrow(() -> new RuntimeException("Symptom not found with ID: " + id));

            symptomRepo.delete(existingSymptom);

            logger.info("Symptom deleted successfully with ID: {}", id);

            return baseResponse.successResponse("Symptom deleted successfully");

        } catch (RuntimeException e) {
            logger.warn("Symptom not found with ID {}: {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error occurred while deleting symptom with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete symptom: " + e.getMessage());
        }
    }

    /**
     * Converts Symptom entity to SymptomResponseDTO.
     *
     * @param symptom the symptom entity
     * @return the response DTO
     */
    private SymptomResponseDTO convertToResponseDTO(Symptom symptom) {
        SymptomResponseDTO dto = new SymptomResponseDTO();
        dto.setId(symptom.getId());
        dto.setName(symptom.getName());
        return dto;
    }
}
