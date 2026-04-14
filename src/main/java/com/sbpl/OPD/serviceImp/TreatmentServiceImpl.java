package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.treatment.TreatmentCreateDTO;
import com.sbpl.OPD.dto.treatment.TreatmentResponseDTO;
import com.sbpl.OPD.model.Treatment;
import com.sbpl.OPD.model.TreatmentCategory;
import com.sbpl.OPD.repository.TreatmentCategoryRepository;
import com.sbpl.OPD.repository.TreatmentRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.TreatmentService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class TreatmentServiceImpl implements TreatmentService {

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private TreatmentCategoryRepository categoryRepository;

    @Autowired
    private BaseResponse baseResponse;

    private static final Logger logger =
            LoggerFactory.getLogger(TreatmentServiceImpl.class);

    // ---------------- GET FULL PRICE LIST ----------------
    @Override
    public ResponseEntity<?> getFullPriceList(Long branchId) {
        try {
            UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
            Long companyId = DbUtill.getLoggedInCompanyId();

            List<Treatment> treatments;

            if (currentUser.name().equalsIgnoreCase("SUPER_ADMIN")|| currentUser.name().equalsIgnoreCase("SUPER_ADMIN_MANAGER")) {

                treatments = treatmentRepository.findByActiveTrue();
            }

            else if (currentUser.name().equalsIgnoreCase("SAAS_ADMIN") || currentUser.name().equalsIgnoreCase("SAAS_ADMIN_MANAGER")) {

                if (branchId != null) {
                    treatments = treatmentRepository
                            .findByCompanyIdAndBranchIdAndActiveTrue(companyId, branchId);
                } else {
                    treatments = treatmentRepository
                            .findByCompanyIdAndActiveTrue(companyId);
                }

            }

            else {

                if (branchId != null) {
                    treatments = treatmentRepository
                            .findByBranchIdAndActiveTrue(branchId);
                } else {
                    treatments = treatmentRepository
                            .findByCompanyIdAndActiveTrue(companyId);
                }

            }

            Map<String, List<TreatmentResponseDTO>> result =
                    treatments.stream()
                            .map(this::mapToDTO)
                            .collect(Collectors.groupingBy(
                                    TreatmentResponseDTO::getCategoryName
                            ));

            return baseResponse.successResponse(
                    "Price list fetched successfully",
                    result
            );

        } catch (Exception e) {
            logger.error("Error fetching price list", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch price list"
            );
        }
    }

    // ---------------- GET BY CATEGORY ----------------
    @Override
    public ResponseEntity<?> getByCategory(Long categoryId, Long branchId) {
        try {
            UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
            Long companyId = DbUtill.getLoggedInCompanyId();
            Long branchIdForLoggerUser = DbUtill.getLoggedInBranchId();

            Long companyFilter;
            Long branchFilter;

            if (currentUser == UserRole.SUPER_ADMIN
                    || currentUser == UserRole.SUPER_ADMIN_MANAGER) {
                companyFilter = null;
                branchFilter = null;
            }else if (currentUser == UserRole.SAAS_ADMIN
                    || currentUser == UserRole.SAAS_ADMIN_MANAGER) {
                companyFilter = companyId;
                branchFilter = branchId;
            }else {

                companyFilter = companyId;
                branchFilter = branchIdForLoggerUser;
            }

            List<TreatmentResponseDTO> list =
                    treatmentRepository
                            .findByCategoryWithFilters(categoryId, companyFilter, branchFilter)
                            .stream()
                            .map(this::mapToDTO)
                            .toList();

            return baseResponse.successResponse(
                    "Treatments fetched successfully",
                    list
            );

        } catch (Exception e) {
            logger.error("Error fetching treatments by category", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch treatments"
            );
        }
    }

    // ---------------- SEARCH ----------------
//    @Override
//    public ResponseEntity<?> search(String keyword) {
//        List<TreatmentResponseDTO> list =
//                treatmentRepository.searchByName(keyword)
//                        .stream()
//                        .map(this::mapToDTO)
//                        .toList();
//
//        return baseResponse.successResponse(
//                "Search results fetched successfully",
//                list
//        );
//    }

    @Override
    public ResponseEntity<?> search(String keyword, Long branchId){

        try {
            UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
            Long companyId = DbUtill.getLoggedInCompanyId();
            Long branchIdForLoggerUser = DbUtill.getLoggedInBranchId();

            Long companyFilter;
            Long branchFilter;

            if (currentUser == UserRole.SUPER_ADMIN
                    || currentUser == UserRole.SUPER_ADMIN_MANAGER) {
                companyFilter = null;
                branchFilter = null;
            }else if (currentUser == UserRole.SAAS_ADMIN
                    || currentUser == UserRole.SAAS_ADMIN_MANAGER) {
                companyFilter = companyId;
                branchFilter = branchId;
            }else {

                companyFilter = companyId;
                branchFilter = branchIdForLoggerUser;
            }

            List<TreatmentResponseDTO> list =
                    treatmentRepository
                            .searchTreatments(keyword, companyFilter, branchFilter)
                            .stream()
                            .map(this::mapToDTO)
                            .toList();

            return baseResponse.successResponse(
                    "Search results fetched successfully",
                    list
            );

        } catch (Exception e) {
            logger.error("Error searching treatments", e);

            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to search treatments"
            );        }
    }

    // ---------------- CREATE TREATMENTS (BULK) ----------------
    @Override
    public ResponseEntity<?> createTreatment(List<TreatmentCreateDTO> dtoList, Long branchId) {

        Long loggedUserCompanyId = DbUtill.getLoggedInCompanyId();

        try {
            if (branchId == null) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "BranchId is required"
                );
            }
            if (dtoList == null || dtoList.isEmpty()) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Treatment list cannot be empty"
                );
            }

            List<Treatment> treatmentsToSave = new ArrayList<>();

            for (TreatmentCreateDTO dto : dtoList) {

                // -------- VALIDATION --------
                if (dto.getCategoryName() == null || dto.getCategoryName().isBlank()) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Category name is required for all treatments"
                    );
                }

                if (dto.getName() == null || dto.getName().isBlank()) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Treatment name is required"
                    );
                }

                // -------- CATEGORY AUTO CREATE / FETCH --------
                TreatmentCategory category = categoryRepository
                        .findByNameIgnoreCase(dto.getCategoryName().trim())
                        .orElseGet(() -> {
                            TreatmentCategory newCategory = new TreatmentCategory();
                            newCategory.setName(dto.getCategoryName().trim().toUpperCase());
                            newCategory.setActive(true);
                            return categoryRepository.save(newCategory);
                        });

                // -------- CREATE TREATMENT --------
                Treatment treatment = new Treatment();
                treatment.setCategory(category);
                treatment.setName(dto.getName());
                treatment.setClinicId(loggedUserCompanyId);
                treatment.setBranchId(branchId);
                treatment.setSingleSessionPrice(dto.getSingleSessionPrice());
                treatment.setMinimumPrice(dto.getMinimumPrice());
                treatment.setMinimumSessions(dto.getMinimumSessions());
                treatment.setPackagePrice(dto.getPackagePrice());
                treatment.setUnitLabel(dto.getUnitLabel());
                treatment.setUnitPrice(dto.getUnitPrice());
                treatment.setActive(dto.getActive() != null ? dto.getActive() : true);

                treatmentsToSave.add(treatment);
            }

            // -------- SAVE ALL AT ONCE --------
            treatmentRepository.saveAll(treatmentsToSave);

            return baseResponse.successResponse(
                    "Treatments created successfully",
                    Map.of("count", treatmentsToSave.size())
            );

        } catch (Exception e) {
            logger.error("Error creating treatments", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create treatments"
            );
        }
    }

    // ---------------- GET BY ID ----------------
    @Override
    public ResponseEntity<?> getTreatmentById(Long id) {
        try {
            Treatment treatment = treatmentRepository.findById(id)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Treatment not found"));

            return baseResponse.successResponse(
                    "Treatment fetched successfully",
                    mapToDTO(treatment)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );
        }
    }

    // ---------------- MAPPER ----------------
    private TreatmentResponseDTO mapToDTO(Treatment treatment) {
        TreatmentResponseDTO dto = new TreatmentResponseDTO();
        dto.setId(treatment.getId());
        dto.setCategoryName(treatment.getCategory().getName());
        dto.setTreatmentName(treatment.getName());
        dto.setSingleSessionPrice(treatment.getSingleSessionPrice());
        dto.setMinimumPrice(treatment.getMinimumPrice());
        dto.setMinimumSessions(treatment.getMinimumSessions());
        dto.setPackagePrice(treatment.getPackagePrice());
        dto.setUnitLabel(treatment.getUnitLabel());
        dto.setUnitPrice(treatment.getUnitPrice());
        return dto;
    }
}
