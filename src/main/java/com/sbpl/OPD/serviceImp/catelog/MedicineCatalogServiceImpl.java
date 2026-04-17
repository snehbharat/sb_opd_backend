package com.sbpl.OPD.serviceImp.catelog;

import com.sbpl.OPD.dto.catelog.request.MedicineCatalogRequestDTO;
import com.sbpl.OPD.dto.catelog.request.MedicineCatalogUpdateDTO;
import com.sbpl.OPD.dto.catelog.response.MedicineCatalogResponseDTO;
import com.sbpl.OPD.model.prescription.MedicineCatalog;
import com.sbpl.OPD.repository.prescription.MedicineCatalogRepo;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.response.ResponseDto;
import com.sbpl.OPD.service.catelog.MedicineCatalogService;
import com.sbpl.OPD.utils.DbUtill;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import java.util.List;

/**
 * Implementation of MedicineCatalogService for managing medicine catalog operations.
 * Provides CRUD operations with logging, error handling, and pagination support.
 *
 * @author Rahul Kumar
 */
@Service
public class MedicineCatalogServiceImpl implements MedicineCatalogService {

    @Autowired
    private MedicineCatalogRepo medicineCatalogRepo;

    @Autowired
    private BaseResponse baseResponse;

    public static final Logger logger = LoggerFactory.getLogger(MedicineCatalogServiceImpl.class);

    /**
     * Creates a new medicine in the catalog.
     *
     * @param dto the medicine catalog request DTO containing medicine details
     * @return ResponseEntity with BaseResponse containing created medicine details
     */
    @Override
    @Transactional
    public ResponseEntity<ResponseDto> createMedicine(MedicineCatalogRequestDTO dto) {
        logger.info("Creating new medicine [name={}, brand={}, strength={}]",
                dto.getName(), dto.getBrandName(), dto.getStrength());

        try {
            Long currentUserId = DbUtill.getLoggedInUserId();

            if (dto.getName() == null || dto.getName().trim().isEmpty()) {
                logger.warn("Medicine name is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Medicine name is required");
            }

            if (dto.getBrandName() == null || dto.getBrandName().trim().isEmpty()) {
                logger.warn("Brand name is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Brand name is required");
            }

            if (dto.getStrength() == null || dto.getStrength().trim().isEmpty()) {
                logger.warn("Medicine strength is required");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Medicine strength is required");
            }

            MedicineCatalog medicineCatalog = new MedicineCatalog();
            medicineCatalog.setName(dto.getName().trim());
            medicineCatalog.setForm(dto.getForm());
            medicineCatalog.setBrandName(dto.getBrandName().trim());
            medicineCatalog.setStrength(dto.getStrength().trim());
            medicineCatalog.setActive(true);
            medicineCatalog.setCreatedBy(currentUserId);

            MedicineCatalog savedMedicine = medicineCatalogRepo.save(medicineCatalog);
            logger.info("Medicine created successfully with ID: {}", savedMedicine.getId());
            return baseResponse.successResponse("Medicine created successfully");

        } catch (Exception e) {
            logger.error("Error occurred while creating medicine [name={}, brand={}]: {}",
                    dto.getName(), dto.getBrandName(), e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create medicine: " + e.getMessage());
        }
    }

    /**
     * Updates an existing medicine in the catalog.
     *
     * @param id  the ID of the medicine to update
     * @param dto the medicine catalog update DTO containing updated details
     * @return ResponseEntity with BaseResponse containing updated medicine details
     */
    @Override
    @Transactional
    public ResponseEntity<ResponseDto> updateMedicine(Long id, MedicineCatalogUpdateDTO dto) {
        logger.info("Updating medicine with ID: {}", id);

        try {
            Long currentUserId = DbUtill.getLoggedInUserId();

            MedicineCatalog existingMedicine = medicineCatalogRepo.findById(id)
                    .orElse(null);

            if (existingMedicine == null) {
                logger.warn("Medicine not found with ID: {}", id);
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Medicine not found with ID: " + id);
            }

            if (dto.getName() != null && !dto.getName().trim().isEmpty()) {
                existingMedicine.setName(dto.getName().trim());
            }

            if (dto.getForm() != null) {
                existingMedicine.setForm(dto.getForm());
            }

            if (dto.getBrandName() != null && !dto.getBrandName().trim().isEmpty()) {
                existingMedicine.setBrandName(dto.getBrandName().trim());
            }

            if (dto.getStrength() != null && !dto.getStrength().trim().isEmpty()) {
                existingMedicine.setStrength(dto.getStrength().trim());
            }
            existingMedicine.setUpdatedBy(currentUserId);

            MedicineCatalog updatedMedicine = medicineCatalogRepo.save(existingMedicine);
            logger.info("Medicine updated successfully with ID: {}", updatedMedicine.getId());

            return baseResponse.successResponse("Medicine updated successfully");

        } catch (Exception e) {
            logger.error("Error occurred while updating medicine with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update medicine: " + e.getMessage());
        }
    }

    /**
     * Retrieves a medicine by its ID.
     *
     * @param id the ID of the medicine
     * @return ResponseEntity with BaseResponse containing medicine details
     */
    @Override
    public ResponseEntity<ResponseDto> getById(Long id) {
        logger.info("Fetching medicine with ID: {}", id);

        try {
            MedicineCatalog medicineCatalog = medicineCatalogRepo.findById(id)
                    .orElse(null);

            if (medicineCatalog == null) {
                logger.warn("Medicine not found with ID: {}", id);
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Medicine not found with ID: " + id);
            }

            MedicineCatalogResponseDTO responseDTO = convertToResponseDTO(medicineCatalog);
            logger.info("Medicine retrieved successfully with ID: {}", id);

            return baseResponse.successResponse("Medicine retrieved successfully", responseDTO);

        } catch (Exception e) {
            logger.error("Error occurred while fetching medicine with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch medicine: " + e.getMessage());
        }
    }

    /**
     * Retrieves all active medicines with pagination.
     *
     * @param pageNo   the pagination information
     * @param pageSize the pagination information
     * @return ResponseEntity with BaseResponse containing paginated active medicines
     */
    @Override
    public ResponseEntity<ResponseDto> getAllActive(Integer pageNo, Integer pageSize) {
        logger.info("Fetching all active medicines with pagination: page={}, size={}",
                pageNo, pageSize);

        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

            Page<MedicineCatalog> medicinesPage = medicineCatalogRepo.findByIsActiveTrue(pageRequest);

            List<MedicineCatalogResponseDTO> responsePage = medicinesPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully retrieved {} active medicines", medicinesPage.getTotalElements());

            return baseResponse.successResponse("Active medicines retrieved successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while fetching all active medicines: {}", e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch active medicines: " + e.getMessage());
        }
    }

    /**
     * Searches medicines by name with pagination.
     *
     * @param search   the search term to match against medicine names
     * @param pageNo   the pagination information
     * @param pageSize the pagination information
     * @return ResponseEntity with BaseResponse containing paginated search results
     */
    @Override
    public ResponseEntity<ResponseDto> searchMedicines(String search, Integer pageNo, Integer pageSize) {
        logger.info("Searching medicines with term: '{}', page={}, size={}",
                search, pageNo, pageSize);

        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

            Page<MedicineCatalog> medicinesPage = medicineCatalogRepo
                    .findByIsActiveTrueAndNameContainingIgnoreCase(search.trim(), pageRequest);

            List<MedicineCatalogResponseDTO> responsePage = medicinesPage.getContent().stream()
                    .map(this::convertToResponseDTO).toList();

            logger.info("Successfully found {} medicines matching search term: '{}'",
                    medicinesPage.getTotalElements(), search);

            return baseResponse.successResponse("Medicine search completed successfully", responsePage);

        } catch (Exception e) {
            logger.error("Error occurred while searching medicines with term '{}': {}",
                    search, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search medicines: " + e.getMessage());
        }
    }

    /**
     * Deactivates a medicine by setting its active status to false.
     *
     * @param id the ID of the medicine to deactivate
     * @return ResponseEntity with BaseResponse indicating success or failure
     */
    @Override
    @Transactional
    public ResponseEntity<ResponseDto> deactivateMedicine(Long id) {
        logger.info("Deactivating medicine with ID: {}", id);

        try {
            MedicineCatalog existingMedicine = medicineCatalogRepo.findById(id)
                    .orElse(null);

            if (existingMedicine == null) {
                logger.warn("Medicine not found with ID: {}", id);
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Medicine not found with ID: " + id);
            }

            if (!existingMedicine.isActive()) {
                logger.info("Medicine with ID: {} is already deactivated", id);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Medicine is already deactivated");
            }

            existingMedicine.setActive(false);
            medicineCatalogRepo.save(existingMedicine);

            logger.info("Medicine deactivated successfully with ID: {}", id);

            return baseResponse.successResponse("Medicine deactivated successfully");

        } catch (Exception e) {
            logger.error("Error occurred while deactivating medicine with ID {}: {}",
                    id, e.getMessage(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to deactivate medicine: " + e.getMessage());
        }
    }

    /**
     * Converts MedicineCatalog entity to MedicineCatalogResponseDTO.
     *
     * @param medicineCatalog the medicine catalog entity
     * @return the response DTO
     */
    private MedicineCatalogResponseDTO convertToResponseDTO(MedicineCatalog medicineCatalog) {
        MedicineCatalogResponseDTO dto = new MedicineCatalogResponseDTO();
        dto.setId(medicineCatalog.getId());
        dto.setName(medicineCatalog.getName());
        dto.setForm(medicineCatalog.getForm());
        dto.setBrandName(medicineCatalog.getBrandName());
        dto.setStrength(medicineCatalog.getStrength());
        dto.setActive(medicineCatalog.isActive());
        return dto;
    }
}
