package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.Doctor.DoctorCoreExpertiseResponseDTO;
import com.sbpl.OPD.dto.Doctor.DepartmentExpertiseGroupDTO;
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
import java.util.Map;
import java.util.stream.Collectors;
import java.util.LinkedHashMap;
import java.util.ArrayList;

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
            expertise.setDepartmentName(category); // Using category as departmentName if needed
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

            if (category != null) {
                expertise.setDepartmentName(category); // Using category as departmentName
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

        // Since isActive field is removed, we fetch all expertise regardless of activeOnly parameter
        Page<DoctorCoreExpertise> page = expertiseRepository.findAll(pageRequest);

        List<DoctorCoreExpertiseResponseDTO> responseDTOs = page.getContent().stream()
                .map(this::convertToResponseDTO)
                .collect(Collectors.toList());

        String message = "All core expertise fetched successfully";

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
        logger.info("Activate/Deactivate request for doctor core expertise [id={}, active={}] - Note: isActive field removed", expertiseId, active);

        try {
            DoctorCoreExpertise expertise = expertiseRepository.findById(expertiseId)
                    .orElseThrow(() -> new IllegalArgumentException("Core expertise not found with ID: " + expertiseId));

            // Note: Since isActive field is removed from DoctorCoreExpertise model,
            // this method now just logs the request without actually changing any field.
            // Consider removing this endpoint if not needed, or add alternative logic.
            expertise.setUpdatedBy(DbUtill.getLoggedInUserId());
            expertiseRepository.save(expertise);

            String message = "Expertise status update request received. Note: isActive field is not available in current model.";

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
        dto.setDepartmentName(expertise.getDepartmentName());
        
        // Get doctor count
        Long count = expertiseRepository.countDoctorsByExpertiseId(expertise.getId());
        dto.setDoctorCount(count);
        
        return dto;
    }

    @Override
    public ResponseEntity<?> getExpertiseByDepartment(String departmentName) {
        logger.info("Fetching core expertise by department [department={}]", departmentName);

        try {
            List<DoctorCoreExpertise> expertiseList = expertiseRepository.findByDepartmentNameIgnoreCase(departmentName);
            
            List<DoctorCoreExpertiseResponseDTO> responseDTOs = expertiseList.stream()
                    .map(this::convertToResponseDTO)
                    .collect(Collectors.toList());

            String message = responseDTOs.isEmpty()
                    ? "No core expertise found for department '" + departmentName + "'"
                    : "Core expertise fetched successfully for department '" + departmentName + "'";

            return baseResponse.successResponse(message, responseDTOs);

        } catch (Exception e) {
            logger.error("Error fetching core expertise by department", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch expertise by department");
        }
    }

    @Override
    public ResponseEntity<?> getAllExpertiseGroupedByDepartment() {
        logger.info("Fetching all core expertise grouped by department");

        try {
            // Get all expertise
            List<DoctorCoreExpertise> allExpertise = expertiseRepository.findAll();

            // Group by department
            Map<String, List<DoctorCoreExpertise>> groupedByDepartment = allExpertise.stream()
                    .filter(exp -> exp.getDepartmentName() != null && !exp.getDepartmentName().trim().isEmpty())
                    .collect(Collectors.groupingBy(
                            DoctorCoreExpertise::getDepartmentName,
                            LinkedHashMap::new,
                            Collectors.toList()
                    ));

            // Convert to response DTO
            List<DepartmentExpertiseGroupDTO> responseList = new ArrayList<>();
            long totalDoctorCountAll = 0;

            for (Map.Entry<String, List<DoctorCoreExpertise>> entry : groupedByDepartment.entrySet()) {
                String department = entry.getKey();
                List<DoctorCoreExpertise> expertiseInDept = entry.getValue();

                List<DoctorCoreExpertiseResponseDTO> expertiseDTOs = expertiseInDept.stream()
                        .map(this::convertToResponseDTO)
                        .collect(Collectors.toList());

                long totalDoctorCount = expertiseDTOs.stream()
                        .mapToLong(dto -> dto.getDoctorCount() != null ? dto.getDoctorCount() : 0L)
                        .sum();

                totalDoctorCountAll += totalDoctorCount;

                DepartmentExpertiseGroupDTO groupDTO = new DepartmentExpertiseGroupDTO();
                groupDTO.setDepartmentName(department);
                groupDTO.setExpertiseCount((long) expertiseDTOs.size());
                groupDTO.setTotalDoctorCount(totalDoctorCount);
                groupDTO.setExpertiseList(expertiseDTOs);

                responseList.add(groupDTO);
            }

            // Add expertise without department to a separate group
            List<DoctorCoreExpertise> ungrouped = allExpertise.stream()
                    .filter(exp -> exp.getDepartmentName() == null || exp.getDepartmentName().trim().isEmpty())
                    .collect(Collectors.toList());

            if (!ungrouped.isEmpty()) {
                List<DoctorCoreExpertiseResponseDTO> ungroupedDTOs = ungrouped.stream()
                        .map(this::convertToResponseDTO)
                        .collect(Collectors.toList());

                DepartmentExpertiseGroupDTO ungroupedDTO = new DepartmentExpertiseGroupDTO();
                ungroupedDTO.setDepartmentName("Other/Unclassified");
                ungroupedDTO.setExpertiseCount((long) ungroupedDTOs.size());
                ungroupedDTO.setTotalDoctorCount(ungroupedDTOs.stream()
                        .mapToLong(dto -> dto.getDoctorCount() != null ? dto.getDoctorCount() : 0L)
                        .sum());
                ungroupedDTO.setExpertiseList(ungroupedDTOs);

                responseList.add(ungroupedDTO);
            }

            logger.info("Successfully grouped {} expertise entries into {} departments",
                    allExpertise.size(), responseList.size());

            return baseResponse.successResponse(
                    "Core expertise grouped by department fetched successfully",
                    responseList
            );

        } catch (Exception e) {
            logger.error("Error grouping expertise by department", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to group expertise by department"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllDepartments() {
        logger.info("Fetching all distinct department names");

        try {
            List<String> departments = expertiseRepository.findAllDistinctDepartmentNames();
            
            // Filter out nulls and empty strings
            List<String> validDepartments = departments.stream()
                    .filter(dept -> dept != null && !dept.trim().isEmpty())
                    .sorted()
                    .collect(Collectors.toList());

            return baseResponse.successResponse(
                    "Departments fetched successfully",
                    validDepartments
            );

        } catch (Exception e) {
            logger.error("Error fetching departments", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch departments"
            );
        }
    }
}
