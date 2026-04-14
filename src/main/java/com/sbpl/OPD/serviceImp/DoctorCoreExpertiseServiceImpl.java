package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.Doctor.DoctorCoreExpertiseResponseDTO;
import com.sbpl.OPD.model.DoctorCoreExpertise;
import com.sbpl.OPD.repository.DoctorCoreExpertiseRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.DoctorCoreExpertiseService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Implementation of doctor core expertise service.
 *
 * @author Rahul Kumar
 */
@Service
public class DoctorCoreExpertiseServiceImpl implements DoctorCoreExpertiseService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorCoreExpertiseServiceImpl.class);

    private final DoctorCoreExpertiseRepository expertiseRepository;
    private final BaseResponse baseResponse;

    public DoctorCoreExpertiseServiceImpl(DoctorCoreExpertiseRepository expertiseRepository, BaseResponse baseResponse) {
        this.expertiseRepository = expertiseRepository;
        this.baseResponse = baseResponse;
    }

    @Transactional
    @Override
    public ResponseEntity<?> createCoreExpertise(String expertiseName, String description, String category) {
        logger.info("Creating doctor core expertise [name={}]", expertiseName);

        try {
            // Check if expertise already exists
            if (expertiseRepository.existsByExpertiseNameIgnoreCase(expertiseName)) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Expertise '" + expertiseName + "' already exists");
            }

            DoctorCoreExpertise expertise = new DoctorCoreExpertise();
            expertise.setExpertiseName(expertiseName.trim());
            expertise.setDescription(description);
            expertise.setCategory(category);
            expertise.setIsActive(true);
            expertise.setCreatedBy(DbUtill.getLoggedInUserId());

            DoctorCoreExpertise saved = expertiseRepository.save(expertise);

            logger.info("Doctor core expertise created successfully [id={}]", saved.getId());
            return baseResponse.successResponse("Core expertise created successfully", convertToResponseDTO(saved));

        } catch (DataIntegrityViolationException e) {
            logger.warn("Failed to create core expertise - data integrity violation", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(HttpStatus.CONFLICT, "Expertise with this name already exists");
        } catch (Exception e) {
            logger.error("Error creating core expertise", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to create core expertise");
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateCoreExpertise(Long expertiseId, String expertiseName, String description, String category, Boolean isActive) {
        logger.info("Updating doctor core expertise [id={}]", expertiseId);

        try {
            DoctorCoreExpertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new IllegalArgumentException("Core expertise not found with ID: " + expertiseId));

            // Update fields if provided
            if (expertiseName != null && !expertiseName.trim().isEmpty()) {
                // Check if new name conflicts with existing
                if (!expertise.getExpertiseName().equalsIgnoreCase(expertiseName.trim()) 
                        && expertiseRepository.existsByExpertiseNameIgnoreCase(expertiseName.trim())) {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Expertise '" + expertiseName + "' already exists");
                }
                expertise.setExpertiseName(expertiseName.trim());
            }

            if (description != null) {
                expertise.setDescription(description);
            }

            if (category != null) {
                expertise.setCategory(category);
            }

            if (isActive != null) {
                expertise.setIsActive(isActive);
            }

            expertise.setUpdatedBy(DbUtill.getLoggedInUserId());
            DoctorCoreExpertise saved = expertiseRepository.save(expertise);

            logger.info("Doctor core expertise updated successfully [id={}]", saved.getId());
            return baseResponse.successResponse("Core expertise updated successfully", convertToResponseDTO(saved));

        } catch (IllegalArgumentException e) {
            logger.warn("Core expertise update failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (DataIntegrityViolationException e) {
            logger.warn("Failed to update core expertise - data integrity violation", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(HttpStatus.CONFLICT, "Expertise with this name already exists");
        } catch (Exception e) {
            logger.error("Error updating core expertise", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to update core expertise");
        }
    }

    @Override
    public ResponseEntity<?> getCoreExpertiseById(Long expertiseId) {
        logger.info("Fetching doctor core expertise [id={}]", expertiseId);

        try {
            DoctorCoreExpertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new IllegalArgumentException("Core expertise not found with ID: " + expertiseId));

            DoctorCoreExpertiseResponseDTO responseDTO = convertToResponseDTO(expertise);
            return baseResponse.successResponse("Core expertise fetched successfully", responseDTO);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllCoreExpertise(Integer pageNo, Integer pageSize, Boolean activeOnly) {
        PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

        Page<DoctorCoreExpertise> page;
        
        if (Boolean.TRUE.equals(activeOnly)) {
            page = expertiseRepository.findByIsActiveTrue(pageRequest);
        } else {
            page = expertiseRepository.findAll(pageRequest);
        }

        List<DoctorCoreExpertiseResponseDTO> responseDTOs = page.getContent().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        String message = Boolean.TRUE.equals(activeOnly) 
                ? "Active core expertise fetched successfully" 
                : "All core expertise fetched successfully";

        return baseResponse.successResponse(
                message,
                DbUtill.buildPaginatedResponse(page, responseDTOs)
        );
    }

    @Override
    public ResponseEntity<?> searchCoreExpertiseByName(String name) {
        logger.info("Searching doctor core expertise [name={}]", name);

        List<DoctorCoreExpertise> expertiseList = expertiseRepository.findByExpertiseNameContainingIgnoreCase(name);
        
        List<DoctorCoreExpertiseResponseDTO> responseDTOs = expertiseList.stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        String message = responseDTOs.isEmpty() 
                ? "No core expertise found matching '" + name + "'" 
                : "Core expertise search completed successfully";

        return baseResponse.successResponse(message, responseDTOs);
    }

    @Transactional
    @Override
    public ResponseEntity<?> activateOrDeactivateExpertise(Long expertiseId, Boolean active) {
        logger.info("Activating/Deactivating doctor core expertise [id={}, active={}]", expertiseId, active);

        try {
            DoctorCoreExpertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new IllegalArgumentException("Core expertise not found with ID: " + expertiseId));

            expertise.setIsActive(active);
            expertise.setUpdatedBy(DbUtill.getLoggedInUserId());
            expertiseRepository.save(expertise);

            String message = Boolean.TRUE.equals(active) 
                    ? "Core expertise activated successfully" 
                    : "Core expertise deactivated successfully";

            return baseResponse.successResponse(message);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> deleteCoreExpertise(Long expertiseId) {
        logger.info("Deleting doctor core expertise [id={}]", expertiseId);

        try {
            // Check if any doctors are using this expertise
            Long doctorCount = expertiseRepository.countDoctorsByExpertiseId(expertiseId);
            if (doctorCount > 0) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                        "Cannot delete core expertise. " + doctorCount + " doctor(s) are currently associated with this expertise.");
            }

            if (!expertiseRepository.existsById(expertiseId)) {
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Core expertise not found");
            }

            expertiseRepository.deleteById(expertiseId);
            return baseResponse.successResponse("Core expertise deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting core expertise", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to delete core expertise");
        }
    }

    /**
     * Convert entity to response DTO
     */
    private DoctorCoreExpertiseResponseDTO convertToResponseDTO(DoctorCoreExpertise expertise) {
        DoctorCoreExpertiseResponseDTO dto = new DoctorCoreExpertiseResponseDTO();
        dto.setId(expertise.getId());
        dto.setExpertiseName(expertise.getExpertiseName());
        dto.setDescription(expertise.getDescription());
        dto.setCategory(expertise.getCategory());
        dto.setIsActive(expertise.getIsActive());
        
        // Get doctor count
        Long count = expertiseRepository.countDoctorsByExpertiseId(expertise.getId());
        dto.setDoctorCount(count);
        
        return dto;
    }
}
