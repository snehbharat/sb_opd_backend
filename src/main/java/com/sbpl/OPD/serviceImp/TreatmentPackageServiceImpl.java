package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.appointment.PackageInputDTO;
import com.sbpl.OPD.dto.appointment.TreatmentPackageBulkRequest;
import com.sbpl.OPD.dto.treatment.pkg.TreatmentPackageCreateDTO;
import com.sbpl.OPD.dto.treatment.pkg.TreatmentPackageResponseDTO;
import com.sbpl.OPD.model.Treatment;
import com.sbpl.OPD.model.TreatmentPackage;
import com.sbpl.OPD.repository.TreatmentPackageRepository;
import com.sbpl.OPD.repository.TreatmentRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.TreatmentPackageService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class TreatmentPackageServiceImpl implements TreatmentPackageService {

    @Autowired
    private TreatmentPackageRepository treatmentPackageRepository;

    @Autowired
    private TreatmentRepository treatmentRepository;

    @Autowired
    private BaseResponse baseResponse;

    private static final Logger logger = LoggerFactory.getLogger(TreatmentPackageServiceImpl.class);

    @Override
    public ResponseEntity<?> getAllPackages(Long branchId) {
        UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
        Long companyId = DbUtill.getLoggedInCompanyId();
        logger.info("Fetching all treatment packages [currentUser={}, companyId={}, branchId={}]",
                currentUser, companyId, branchId);
        try {

            List<TreatmentPackage> packages;

            if (currentUser.name().equalsIgnoreCase("SUPER_ADMIN") ||
                    currentUser.name().equalsIgnoreCase("SUPER_ADMIN_MANAGER")) {
                packages = treatmentPackageRepository.findByActiveTrue();
                logger.info("SUPER_ADMIN fetching all active packages system-wide");
            } else if (currentUser.name().equalsIgnoreCase("SAAS_ADMIN") ||
                    currentUser.name().equalsIgnoreCase("SAAS_ADMIN_MANAGER")) {
                if (branchId != null) {
                    packages = treatmentPackageRepository.findByCompanyIdAndBranchIdAndActiveTrue(companyId, branchId);
                    logger.info("SAAS_ADMIN fetching packages for company ID: {}, branch ID: {}", companyId, branchId);
                } else {
                    packages = treatmentPackageRepository.findByCompanyIdAndActiveTrue(companyId);
                    logger.info("SAAS_ADMIN fetching packages for company ID: {} (company-wide)", companyId);
                }
            } else {
                if (branchId != null) {
                    packages = treatmentPackageRepository.findByBranchIdAndActiveTrue(branchId);
                    logger.info("{} fetching packages for branch ID: {}", currentUser, branchId);
                } else {
                    packages = treatmentPackageRepository.findByCompanyIdAndActiveTrue(companyId);
                    logger.info("{} fetching packages for company ID: {}", currentUser, companyId);
                }
            }

            List<TreatmentPackageResponseDTO> responseList = packages.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            logger.info("Successfully fetched {} treatment package(s)", responseList.size());

            return baseResponse.successResponse("Treatment packages fetched successfully", responseList);

        } catch (Exception e) {
            logger.error("Error fetching treatment packages", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch treatment packages");
        }
    }

    @Override
    public ResponseEntity<?> getPackagesByTreatmentId(Long treatmentId, Long branchId) {
        UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
        Long companyId = DbUtill.getLoggedInCompanyId();

        logger.info("Fetching packages for treatment ID: {} [currentUser={}, companyId={}, branchId={}]",
                treatmentId, currentUser, companyId, branchId);
        try {

            Long companyFilter;
            Long branchFilter;

            Long branchIdForLoggedUser = null;
            try {
                branchIdForLoggedUser = DbUtill.getLoggedInBranchId();
            } catch (IllegalStateException e) {
                logger.warn("User not associated with any branch: {}", currentUser);
            }

            if (currentUser == UserRole.SUPER_ADMIN || currentUser == UserRole.SUPER_ADMIN_MANAGER) {
                companyFilter = null;
                branchFilter = null;
                logger.info("SUPER_ADMIN accessing packages without filters");
            } else if (currentUser == UserRole.SAAS_ADMIN || currentUser == UserRole.SAAS_ADMIN_MANAGER) {
                companyFilter = companyId;
                branchFilter = branchId;
                logger.info("SAAS_ADMIN filtering by company ID: {}, branch ID: {}", companyId, branchId);
            } else {
                companyFilter = companyId;
                branchFilter = null;
                logger.info("{} filtering by company ID: {}, branch ID: {}", currentUser, companyId, branchIdForLoggedUser);
            }

//            LinkedHashMap<String, TreatmentPackageResponseDTO> list =
//                    treatmentPackageRepository
//                            .findByTreatmentIdWithFilters(treatmentId, companyFilter, branchFilter)
//                            .stream()
//                            .map(this::mapToDTO)
//                            .sorted(Comparator.comparing(TreatmentPackageResponseDTO::getSessions))
//                            .collect(Collectors.toMap(
//                                    dto -> dto.getSessions() == 1
//                                            ? "SINGLE SESSION"
//                                            : dto.getSessions() + " SESSIONS",
//                                    Function.identity(),
//                                    (a, b) -> a,
//                                    LinkedHashMap::new
//                            ));

            Map<String, Object> finalResponse = new LinkedHashMap<>();

            Map<String, TreatmentPackageResponseDTO> packagesMap = new LinkedHashMap<>();

            treatmentPackageRepository
                    .findByTreatmentIdWithFilters(treatmentId, companyFilter, branchFilter)
                    .stream()
                    .map(this::mapToDTO)
                    .sorted(Comparator.comparing(TreatmentPackageResponseDTO::getSessions))
                    .forEach(dto -> {

                        if (dto.getSessions() == 1) {
                            finalResponse.put("SINGLE_SESSION", dto);
                        } else {
                            String key = dto.getSessions() + "_SESSIONS";
                            packagesMap.put(key, dto);
                        }
                    });
            finalResponse.put("packages", packagesMap);

            int totalPackages = finalResponse.size();
            logger.info("Found {} package(s) for treatment ID: {}, grouped into {} categories",
                    totalPackages, treatmentId, finalResponse.size());

            return baseResponse.successResponse("Packages fetched successfully for treatment", finalResponse);

        } catch (Exception e) {
            logger.error("Error fetching packages by treatment ID: {}", treatmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch packages");
        }
    }

    @Override
    public ResponseEntity<?> searchPackages(String keyword, Long branchId) {
        UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
        Long companyId = DbUtill.getLoggedInCompanyId();
        Long branchIdForLoggedUser = DbUtill.getLoggedInBranchId();

        logger.info("Searching treatment packages with keyword: '{}' [currentUser={}, companyId={}, branchId={}]",
                keyword, currentUser, companyId, branchId);
        try {

            Long companyFilter;
            Long branchFilter;

            if (currentUser == UserRole.SUPER_ADMIN || currentUser == UserRole.SUPER_ADMIN_MANAGER) {
                companyFilter = null;
                branchFilter = null;
                logger.info("SUPER_ADMIN searching packages without filters");
            } else if (currentUser == UserRole.SAAS_ADMIN || currentUser == UserRole.SAAS_ADMIN_MANAGER) {
                companyFilter = companyId;
                branchFilter = branchId;
                logger.info("SAAS_ADMIN searching with company filter: {}, branch filter: {}", companyId, branchId);
            } else {
                companyFilter = companyId;
                branchFilter = branchIdForLoggedUser;
                logger.info("{} searching with company filter: {}, branch filter: {}", currentUser, companyId, branchIdForLoggedUser);
            }

            List<TreatmentPackageResponseDTO> list = treatmentPackageRepository
                    .searchTreatmentPackages(keyword, companyFilter, branchFilter)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            if (list.isEmpty()) {
                logger.warn("No packages found matching keyword: '{}'", keyword);
            } else {
                logger.info("Search returned {} package(s) for keyword: '{}'", list.size(), keyword);
            }

            return baseResponse.successResponse("Search results fetched successfully", list);

        } catch (Exception e) {
            logger.error("Error searching treatment packages with keyword", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to search packages");
        }
    }

    @Override
    public ResponseEntity<?> getRecommendedPackages(Long branchId) {
        UserRole currentUser = DbUtill.getLoggedInUserOriginalRole();
        Long companyId = DbUtill.getLoggedInCompanyId();
        Long branchIdForLoggedUser = DbUtill.getLoggedInBranchId();

        logger.info("Fetching recommended packages [currentUser={}, companyId={}, branchId={}]",
                currentUser, companyId, branchId);
        try {

            Long companyFilter;
            Long branchFilter;

            if (currentUser == UserRole.SUPER_ADMIN || currentUser == UserRole.SUPER_ADMIN_MANAGER) {
                companyFilter = null;
                branchFilter = null;
                logger.info("SUPER_ADMIN fetching recommended packages system-wide");
            } else if (currentUser == UserRole.SAAS_ADMIN || currentUser == UserRole.SAAS_ADMIN_MANAGER) {
                companyFilter = companyId;
                branchFilter = branchId;
                logger.info("SAAS_ADMIN fetching recommended packages for company ID: {}, branch ID: {}", companyId, branchId);
            } else {
                companyFilter = companyId;
                branchFilter = branchIdForLoggedUser;
                logger.info("{} fetching recommended packages for company ID: {}, branch ID: {}",
                        currentUser, companyId, branchIdForLoggedUser);
            }

            List<TreatmentPackageResponseDTO> list = treatmentPackageRepository
                    .findRecommendedPackages(companyFilter, branchFilter)
                    .stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            if (list.isEmpty()) {
                logger.warn("No recommended packages found");
            } else {
                logger.info("Found {} recommended package(s)", list.size());
            }

            return baseResponse.successResponse("Recommended packages fetched successfully", list);

        } catch (Exception e) {
            logger.error("Error fetching recommended packages", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch recommended packages");
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> createPackage(List<TreatmentPackageCreateDTO> dtoList, Long treatmentId) {
        Long loggedUserCompanyId = DbUtill.getLoggedInCompanyId();

        logger.info("Creating treatment package(s) [treatmentId={}, packageCount={}, companyId={}]",
                treatmentId, dtoList != null ? dtoList.size() : 0, loggedUserCompanyId);
        try {

            if (treatmentId == null) {
                logger.warn("Treatment ID is required for creating package");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Treatment ID is required");
            }

            if (dtoList == null || dtoList.isEmpty()) {
                logger.warn("Package list is empty or null");
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Package list cannot be empty");
            }


            Treatment treatment = treatmentRepository.findById(treatmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Treatment not found with ID: " + treatmentId));

            logger.info("Found treatment: {} (ID: {})", treatment.getName(), treatmentId);

            List<TreatmentPackage> packagesToSave = new ArrayList<>();
            List<String> skippedPackages = new ArrayList<>();
            List<String> createdPackages = new ArrayList<>();

            for (int i = 0; i < dtoList.size(); i++) {
                TreatmentPackageCreateDTO dto = dtoList.get(i);

                if (dto.getName() == null || dto.getName().isBlank()) {
                    logger.error("Package name is missing or blank at index {}", i);
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Package name is required");
                }
                if (dto.getSessions() == null) {
                    logger.error("Number of sessions is missing at index {}", i);
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Number of sessions is required");
                }

                Optional<TreatmentPackage> existingPackageOpt = treatmentPackageRepository
                        .findExistingPackage(treatmentId, dto.getName(), dto.getSessions(), loggedUserCompanyId);

                if (existingPackageOpt.isPresent()) {
                    TreatmentPackage existingPackage = existingPackageOpt.get();
                    logger.warn("Skipping duplicate package #{}: Name='{}', Sessions={} (already exists with ID: {})", 
                            i + 1, dto.getName(), dto.getSessions(), existingPackage.getId());
                    skippedPackages.add(dto.getName() + " (" + dto.getSessions() + " sessions)");
                    continue;
                }

                TreatmentPackage pkg = new TreatmentPackage();
                pkg.setTreatment(treatment);
                pkg.setName(dto.getName());
                pkg.setSessions(dto.getSessions());
                pkg.setTotalPrice(dto.getTotalPrice());
                pkg.setPerSessionPrice(dto.getPerSessionPrice());
                pkg.setDiscountPercentage(dto.getDiscountPercentage());
                pkg.setRecommended(dto.getRecommended() != null ? dto.getRecommended() : false);
                pkg.setBranchId(treatment.getBranchId());
                pkg.setClinicId(loggedUserCompanyId);
                pkg.setActive(dto.getActive() != null ? dto.getActive() : true);

                packagesToSave.add(pkg);
                createdPackages.add(dto.getName() + " (" + dto.getSessions() + " sessions)");

                logger.debug("Prepared package #{}: Name='{}', Sessions={}, Price={}, Recommended={}, Active={}",
                        i + 1, pkg.getName(), pkg.getSessions(), pkg.getTotalPrice(),
                        pkg.getRecommended(), pkg.getActive());
            }

            if (packagesToSave.isEmpty()) {
                logger.warn("All packages were skipped due to duplicates");
                return baseResponse.successResponse("All packages already exist. No new packages created.",
                        Map.of("skipped", skippedPackages));
            }

            treatmentPackageRepository.saveAll(packagesToSave);

            logger.info("Successfully saved {} treatment package(s) to database. Skipped {} duplicate(s).", 
                    packagesToSave.size(), skippedPackages.size());

            List<TreatmentPackageResponseDTO> responseList = packagesToSave.stream()
                    .map(this::mapToDTO)
                    .toList();

            Map<String, Object> response = new HashMap<>();
            response.put("count", packagesToSave.size());
//            response.put("packages", responseList);
            
            if (!skippedPackages.isEmpty()) {
                response.put("skipped", skippedPackages);
                response.put("skippedCount", skippedPackages.size());
            }

            String successMessage = packagesToSave.size() + " package(s) created successfully.";
            if (!skippedPackages.isEmpty()) {
                successMessage += " " + skippedPackages.size() + " duplicate(s) skipped.";
            }

            return baseResponse.successResponse(successMessage, response);

        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for creating package: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating treatment packages", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to create packages");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> updatePackage(Long id, TreatmentPackageCreateDTO dto) {
        logger.info("Updating treatment package ID: {}", id);
        try {

            TreatmentPackage existingPackage = treatmentPackageRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Package not found with ID: " + id));

            logger.debug("Found existing package: {} (Active={}, Sessions={}, Price={})",
                    existingPackage.getName(), existingPackage.getActive(),
                    existingPackage.getSessions(), existingPackage.getTotalPrice());

            List<String> updatedFields = new ArrayList<>();

            if (dto.getTreatmentId() != null) {
                Treatment treatment = treatmentRepository.findById(dto.getTreatmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Treatment not found with ID: " + dto.getTreatmentId()));
                existingPackage.setTreatment(treatment);
                updatedFields.add("treatment");
                logger.debug("Updated treatment to ID: {}", dto.getTreatmentId());
            }

            if (dto.getName() != null && !dto.getName().isBlank()) {
                existingPackage.setName(dto.getName());
                updatedFields.add("name");
            }

            if (dto.getSessions() != null) {
                existingPackage.setSessions(dto.getSessions());
                updatedFields.add("sessions");
            }

            if (dto.getTotalPrice() != null) {
                existingPackage.setTotalPrice(dto.getTotalPrice());
                updatedFields.add("totalPrice");
            }

            if (dto.getPerSessionPrice() != null) {
                existingPackage.setPerSessionPrice(dto.getPerSessionPrice());
                updatedFields.add("perSessionPrice");
            }

            if (dto.getDiscountPercentage() != null) {
                existingPackage.setDiscountPercentage(dto.getDiscountPercentage());
                updatedFields.add("discountPercentage");
            }

            if (dto.getRecommended() != null) {
                existingPackage.setRecommended(dto.getRecommended());
                updatedFields.add("recommended");
            }

            if (dto.getActive() != null) {
                existingPackage.setActive(dto.getActive());
                updatedFields.add("active");
            }

            if (!updatedFields.isEmpty()) {
                logger.info("Updating fields for package ID {}: {}", id, String.join(", ", updatedFields));
            }

            TreatmentPackage updatedPackage = treatmentPackageRepository.save(existingPackage);

            logger.info("Successfully updated package ID: {}", id);

            return baseResponse.successResponse("Treatment package updated successfully", mapToDTO(updatedPackage));

        } catch (IllegalArgumentException e) {
            logger.warn("Package not found with ID: {}", id);
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating treatment package ID: {}", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to update package");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> deletePackage(Long id) {
        logger.info("Soft deleting treatment package ID: {}", id);
        try {

            TreatmentPackage existingPackage = treatmentPackageRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Package not found with ID: " + id));

            logger.debug("Found package to delete: {} (Currently Active={})",
                    existingPackage.getName(), existingPackage.getActive());

            existingPackage.setActive(false);
            treatmentPackageRepository.save(existingPackage);

            logger.info("Successfully soft-deleted package ID: {} (set active=false)", id);

            return baseResponse.successResponse("Treatment package deleted successfully", null);

        } catch (IllegalArgumentException e) {
            logger.warn("Package not found for deletion with ID: {}", id);
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error deleting treatment package ID: {}", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to delete package");
        }
    }

    @Override
    public ResponseEntity<?> getPackageById(Long id) {
        logger.info("Fetching treatment package by ID: {}", id);
        try {

            TreatmentPackage pkg = treatmentPackageRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Package not found with ID: " + id));

            logger.debug("Found package: {} (ID: {}, Active: {})", pkg.getName(), id, pkg.getActive());

            return baseResponse.successResponse("Treatment package fetched successfully", mapToDTO(pkg));

        } catch (IllegalArgumentException e) {
            logger.warn("Package not found with ID: {}", id);
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching treatment package by ID: {}", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch package");
        }
    }

    private TreatmentPackageResponseDTO mapToDTO(TreatmentPackage pkg) {
        logger.debug("Mapping TreatmentPackage to DTO for package ID: {}, name: {}", pkg.getId(), pkg.getName());

        TreatmentPackageResponseDTO dto = new TreatmentPackageResponseDTO();
        dto.setId(pkg.getId());
        dto.setTreatmentName(pkg.getTreatment().getName());
        dto.setPackageName(pkg.getName());
        dto.setSessions(pkg.getSessions());
        dto.setTotalPrice(pkg.getTotalPrice());
        dto.setDiscountPercentage(pkg.getDiscountPercentage());
        dto.setRecommended(pkg.getRecommended());
        dto.setBranchId(pkg.getBranchId());
        dto.setClinicId(pkg.getClinicId());
        dto.setActive(pkg.getActive());
        return dto;
    }

    @Transactional
    @Override
    public ResponseEntity<?> createPackagesBulk(TreatmentPackageBulkRequest request) {

        Long companyId = DbUtill.getLoggedInCompanyId();

        logger.info("Bulk creating packages for treatmentId={}, companyId={}",
                request.getTreatmentId(), companyId);

        try {

            if (request.getTreatmentId() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Treatment ID is required");
            }

            if (request.getPackages() == null || request.getPackages().isEmpty()) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Package list cannot be empty");
            }

            // ✅ Fetch Treatment
            Treatment treatment = treatmentRepository.findById(request.getTreatmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Treatment not found"));

            // ✅ Sort packages (important)
            List<PackageInputDTO> dtoList = request.getPackages();
            dtoList.sort(Comparator.comparing(PackageInputDTO::getSessions));

            // ✅ Get base price (1 session)
            PackageInputDTO basePackage = dtoList.stream()
                    .filter(p -> p.getSessions() == 1)
                    .findFirst()
                    .orElseThrow(() -> new IllegalArgumentException("Single session price is required"));

            double basePrice = basePackage.getTotalPrice();

            List<TreatmentPackage> packagesToSave = new ArrayList<>();
            List<String> skipped = new ArrayList<>();

            for (PackageInputDTO dto : dtoList) {

                // ✅ Basic validation
                if (dto.getSessions() == null || dto.getSessions() <= 0) {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid sessions");
                }

                if (dto.getTotalPrice() == null || dto.getTotalPrice() <= 0) {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid price");
                }

                int sessions = dto.getSessions();
                double totalPrice = dto.getTotalPrice();

                // ✅ Auto calculations
                double perSession = totalPrice / sessions;

                double originalTotal = basePrice * sessions;
                double discount = ((originalTotal - totalPrice) / originalTotal) * 100;

                String name = getPackageName(sessions);

                // ✅ Duplicate check
                Optional<TreatmentPackage> existing = treatmentPackageRepository
                        .findExistingPackage(request.getTreatmentId(), name, sessions, companyId);

                if (existing.isPresent()) {
                    skipped.add(name + " (" + sessions + ")");
                    continue;
                }

                // ✅ Create entity
                TreatmentPackage pkg = new TreatmentPackage();
                pkg.setTreatment(treatment);
                pkg.setName(name);
                pkg.setSessions(sessions);
                pkg.setTotalPrice(BigDecimal.valueOf(totalPrice));
                pkg.setPerSessionPrice(BigDecimal.valueOf(Math.round(perSession)));
                pkg.setDiscountPercentage((int) Math.round(discount));
                pkg.setRecommended(sessions == 6);
                pkg.setBranchId(treatment.getBranchId());
                pkg.setClinicId(companyId);
                pkg.setActive(true);

                packagesToSave.add(pkg);
            }

            if (packagesToSave.isEmpty()) {
                return baseResponse.successResponse("All packages already exist",
                        Map.of("skipped", skipped));
            }

            // ✅ Save All
            treatmentPackageRepository.saveAll(packagesToSave);

            Map<String, Object> response = new HashMap<>();
            response.put("createdCount", packagesToSave.size());

            if (!skipped.isEmpty()) {
                response.put("skipped", skipped);
            }

            return baseResponse.successResponse("Packages created successfully", response);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating packages", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
        }
    }

    private String getPackageName(int sessions) {
        return sessions == 1
                ? "MRP SINGLE SESSION"
                : "MRP FOR " + sessions + " SESSION";
    }

    @Override
    public ResponseEntity<?> createPackagesForMultipleTreatments(
            List<TreatmentPackageBulkRequest> requests) {

        logger.info("Bulk creating packages for multiple treatments, count={}", requests.size());

        try {

            if (requests == null || requests.isEmpty()) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Request list cannot be empty");
            }

            int successCount = 0;
            Map<Long, Object> responseMap = new HashMap<>();

            for (TreatmentPackageBulkRequest request : requests) {

                try {
                    ResponseEntity<?> response = createPackagesBulk(request);

                    responseMap.put(request.getTreatmentId(), response.getBody());
                    successCount++;

                } catch (Exception e) {
                    logger.error("Error for treatmentId={}", request.getTreatmentId(), e);
                    responseMap.put(request.getTreatmentId(), "Failed: " + e.getMessage());
                }
            }

            Map<String, Object> finalResponse = new HashMap<>();
            finalResponse.put("totalRequested", requests.size());
            finalResponse.put("successCount", successCount);
            finalResponse.put("details", responseMap);

            return baseResponse.successResponse("Bulk processing completed", finalResponse);

        } catch (Exception e) {
            logger.error("Error in bulk multi-treatment", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Something went wrong");
        }
    }
}