package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.treatment.pkg.PatientPackageUsageDTO;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.PatientPackageUsage;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.PatientPackageUsageRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.PatientPackageUsageService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PatientPackageUsageServiceImpl implements PatientPackageUsageService {

    @Autowired
    private PatientPackageUsageRepository patientPackageUsageRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BaseResponse baseResponse;

    private static final Logger logger = LoggerFactory.getLogger(PatientPackageUsageServiceImpl.class);

    @Override
    public ResponseEntity<?> getPatientPackageUsage(Long patientId) {
        logger.info("Fetching package usage for patient ID: {}", patientId);
        try {
            List<PatientPackageUsage> usages = patientPackageUsageRepository.findByPatientIdAndActiveTrue(patientId);

            if (usages.isEmpty()) {
                logger.warn("No active package usage found for patient ID: {}", patientId);
            } else {
                logger.info("Found {} active package usage(s) for patient ID: {}", usages.size(), patientId);
            }

            List<PatientPackageUsageDTO> dtoList = usages.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return baseResponse.successResponse("Patient package usage fetched successfully", dtoList);

        } catch (Exception e) {
            logger.error("Error fetching patient package usage for patient ID: {}", patientId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch package usage");
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateUsesSession(Long patientId, Long treatmentPackageId,
                                               Boolean followUp, String followUpDate) {
        logger.info("Updating session usage for patient ID: {} and treatment package ID: {}", 
                patientId, treatmentPackageId);
        
        try {
            if (followUp == null) {
                logger.warn("Follow-up parameter is null for patient ID: {}", patientId);
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Follow-up parameter cannot be null"
                );
            }

            if (followUp && (followUpDate == null || followUpDate.trim().isEmpty())) {
                logger.warn("Follow-up date is required when follow-up is true for patient ID: {}", patientId);
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Follow-up date is required when follow-up is enabled"
                );
            }

            Customer patient = customerRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found with ID: " + patientId));

            logger.debug("Found patient: {} {} (ID: {})", 
                    patient.getFirstName(), patient.getLastName(), patientId);

            Optional<PatientPackageUsage> packageUsageOpt = patientPackageUsageRepository
                    .findActiveUsageByPatientAndPackage(patientId, treatmentPackageId);

            PatientPackageUsage usage;

            if (packageUsageOpt.isPresent()) {
                usage = packageUsageOpt.get();

                logger.info("Found existing active package usage: sessionsUsed={}, sessionsRemaining={}, completed={}",
                        usage.getSessionsUsed(), usage.getSessionsRemaining(), usage.getCompleted());

                if (usage.getCompleted()) {
                    logger.warn("Package already completed for patient ID: {} and package ID: {}", 
                            patientId, treatmentPackageId);
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "This treatment package has been fully utilized. All sessions completed."
                    );
                }

                if (usage.getSessionsRemaining() <= 0) {
                    logger.warn("No remaining sessions in package for patient ID: {}", patientId);
                    usage.setCompleted(true);
                    usage.setActive(false);
                    patientPackageUsageRepository.save(usage);
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "No remaining sessions in this package. Package completed."
                    );
                }

                Integer oldSessionsUsed = usage.getSessionsUsed();
                Integer oldSessionsRemaining = usage.getSessionsRemaining();
                
                usage.setSessionsUsed(usage.getSessionsUsed() + 1);
                usage.setSessionsRemaining(usage.getSessionsRemaining() - 1);
                usage.setLastSessionDate(new Date());

                usage.setFollowUp(followUp);
                usage.setFollowUpDate(followUpDate);

                if (usage.getSessionsRemaining() <= 0) {
                    usage.setCompleted(true);
                    usage.setActive(false);
                    logger.info("Package completed for patient ID: {} - all sessions used", patientId);
                }

                patientPackageUsageRepository.save(usage);

                logger.info("Using existing package [patientId={}, packageId={}, sessionsUsed={}->{}, sessionsRemaining={}->{}]",
                        patientId, treatmentPackageId, oldSessionsUsed, usage.getSessionsUsed(),
                        oldSessionsRemaining, usage.getSessionsRemaining());

                String successMessage = usage.getCompleted() 
                        ? "Session updated successfully. Package completed!" 
                        : "Session updated successfully.";

                return baseResponse.successResponse(successMessage);

            } else {
                logger.warn("No active package usage found for patient ID: {} and package ID: {}", 
                        patientId, treatmentPackageId);
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "No active package usage found for this patient and treatment package"
                );
            }
            
        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for updating session usage: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating session usage for patient ID: {} and package ID: {}", 
                    patientId, treatmentPackageId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR, 
                    "Failed to update session usage"
            );
        }
    }

    @Override
    public ResponseEntity<?> findAllCompletedUsesPackage(Long patientId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching completed package usage for patient ID: {} [pageNo={}, pageSize={}]",
                patientId, pageNo, pageSize);
        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

            Page<PatientPackageUsage> packageUsagePage = patientPackageUsageRepository.findAllCompletedUsageByPatientAndPackage(patientId, pageRequest);

            if (packageUsagePage.isEmpty()) {
                logger.warn("No completed package usage found for patient ID: {}", patientId);
            } else {
                logger.info("Found {} completed package usage(s) for patient ID: {}",
                        packageUsagePage.getTotalElements(), patientId);
            }

            List<PatientPackageUsageDTO> packageUsageList = packageUsagePage.stream()
                    .map(this::mapToDTO).toList();

            return baseResponse.successResponse("Patient package usage fetched successfully", packageUsageList);

        } catch (Exception e) {
            logger.error("Error fetching completed package usage for patient ID: {}", patientId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch completed package usage");
        }
    }

    @Override
    public ResponseEntity<?> getActivePackageUsage(Long patientId) {
        logger.info("Fetching active package usage for patient ID: {}", patientId);
        try {
            List<PatientPackageUsage> usages = patientPackageUsageRepository.findActiveUsagesByPatientId(patientId);

            if (usages.isEmpty()) {
                logger.warn("No active package usage found for patient ID: {}", patientId);
            } else {
                logger.info("Found {} active package usage(s) for patient ID: {}", usages.size(), patientId);
                for (PatientPackageUsage usage : usages) {
                    logger.debug("Package: {}, Treatment: {}, Sessions Remaining: {}, Completed: {}",
                            usage.getTreatmentPackage().getName(),
                            usage.getTreatment().getName(),
                            usage.getSessionsRemaining(),
                            usage.getCompleted());
                }
            }

            List<PatientPackageUsageDTO> dtoList = usages.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return baseResponse.successResponse("Active package usage fetched successfully", dtoList);

        } catch (Exception e) {
            logger.error("Error fetching active package usage for patient ID: {}", patientId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch active package usage");
        }
    }

    @Override
    public ResponseEntity<?> checkAvailableSessions(Long patientId, Long treatmentId) {
        logger.info("Checking available sessions for patient ID: {} and treatment ID: {}", patientId, treatmentId);
        try {
            List<PatientPackageUsage> activeUsages = patientPackageUsageRepository.findActiveUsagesByPatientId(patientId);

            int totalAvailableSessions = 0;
            PatientPackageUsage specificUsage = null;

            for (PatientPackageUsage usage : activeUsages) {
                if (usage.getTreatment().getId().equals(treatmentId)) {
                    totalAvailableSessions += usage.getSessionsRemaining();
                    specificUsage = usage;
                    logger.debug("Found matching package usage - Total: {}, Used: {}, Remaining: {}, Completed: {}",
                            usage.getTotalSessions(), usage.getSessionsUsed(),
                            usage.getSessionsRemaining(), usage.getCompleted());
                }
            }

            if (specificUsage == null) {
                logger.warn("No active package found for patient ID: {} and treatment ID: {}", patientId, treatmentId);
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "No active package found for this treatment");
            }

            logger.info("Total available sessions for patient ID: {} and treatment ID: {} is {}",
                    patientId, treatmentId, totalAvailableSessions);

            Map<String, Object> response = Map.of(
                    "available", true,
                    "sessionsRemaining", totalAvailableSessions,
                    "totalSessions", specificUsage.getTotalSessions(),
                    "sessionsUsed", specificUsage.getSessionsUsed(),
                    "packageName", specificUsage.getTreatmentPackage().getName(),
                    "completed", specificUsage.getCompleted()
            );

            return baseResponse.successResponse("Session availability checked successfully", response);

        } catch (Exception e) {
            logger.error("Error checking available sessions for patient ID: {} and treatment ID: {}",
                    patientId, treatmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to check session availability");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> recordSessionUsage(Long patientId, Long treatmentId, Long billId) {
        logger.info("Recording session usage for patient ID: {}, treatment ID: {}, bill ID: {}",
                patientId, treatmentId, billId);
        try {

            Customer patient = customerRepository.findById(patientId)
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            List<PatientPackageUsage> activeUsages = patientPackageUsageRepository.findActiveUsagesByPatientId(patientId);

            PatientPackageUsage usageToUse = null;
            for (PatientPackageUsage usage : activeUsages) {
                if (usage.getTreatment().getId().equals(treatmentId) && !usage.getCompleted()) {
                    usageToUse = usage;
                    break;
                }
            }

            if (usageToUse == null) {
                logger.warn("No active package with available sessions found for patient ID: {} and treatment ID: {}",
                        patientId, treatmentId);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "No active package with available sessions found for this treatment");
            }

            if (usageToUse.getSessionsRemaining() <= 0) {
                logger.warn("Package has no remaining sessions for patient ID: {}. Marking as completed.", patientId);
                usageToUse.setCompleted(true);
                patientPackageUsageRepository.save(usageToUse);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "All sessions in this package have been completed");
            }

            Integer oldSessionsUsed = usageToUse.getSessionsUsed();
            Integer oldSessionsRemaining = usageToUse.getSessionsRemaining();

            usageToUse.setSessionsUsed(usageToUse.getSessionsUsed() + 1);
            usageToUse.setSessionsRemaining(usageToUse.getSessionsRemaining() - 1);
            usageToUse.setLastSessionDate(new Date());

            if (usageToUse.getSessionsRemaining() <= 0) {
                usageToUse.setCompleted(true);
                logger.info("Package completed for patient ID: {} - all sessions used", patientId);
            }

            patientPackageUsageRepository.save(usageToUse);

            logger.info("Session usage recorded for patient ID: {} - Sessions: {}->{} (Used), {}->{} (Remaining)",
                    patientId, oldSessionsUsed, usageToUse.getSessionsUsed(),
                    oldSessionsRemaining, usageToUse.getSessionsRemaining());

            Map<String, Object> response = Map.of(
                    "sessionsUsed", usageToUse.getSessionsUsed(),
                    "sessionsRemaining", usageToUse.getSessionsRemaining(),
                    "completed", usageToUse.getCompleted(),
                    "packageName", usageToUse.getTreatmentPackage().getName()
            );

            return baseResponse.successResponse("Session usage recorded successfully", response);

        } catch (IllegalArgumentException e) {
            logger.warn("Bad request for recording session usage: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error recording session usage for patient ID: {} and treatment ID: {}",
                    patientId, treatmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to record session usage");
        }
    }

    @Override
    public ResponseEntity<?> getPackageUsageHistory(Long patientId) {
        logger.info("Fetching package usage history for patient ID: {}", patientId);
        try {
            List<PatientPackageUsage> allUsages = patientPackageUsageRepository.findByPatientIdAndActiveTrue(patientId);

            if (allUsages.isEmpty()) {
                logger.warn("No package usage history found for patient ID: {}", patientId);
            } else {
                logger.info("Found {} package usage record(s) for patient ID: {}", allUsages.size(), patientId);
            }

            List<PatientPackageUsageDTO> dtoList = allUsages.stream()
                    .map(this::mapToDTO)
                    .collect(Collectors.toList());

            return baseResponse.successResponse("Package usage history fetched successfully", dtoList);

        } catch (Exception e) {
            logger.error("Error fetching package usage history for patient ID: {}", patientId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch package usage history");
        }
    }

    private PatientPackageUsageDTO mapToDTO(PatientPackageUsage usage) {
        logger.debug("Mapping PatientPackageUsage to DTO for usage ID: {}, patient: {}, package: {}",
                usage.getId(),
                usage.getPatient().getFirstName() + " " + usage.getPatient().getLastName(),
                usage.getTreatmentPackage().getName());

        PatientPackageUsageDTO dto = new PatientPackageUsageDTO();
        dto.setId(usage.getId());
        dto.setPatientId(usage.getPatient().getId());
        dto.setPatientName(usage.getPatient().getFirstName() + " " + usage.getPatient().getLastName());
        dto.setTreatmentPackageId(usage.getTreatmentPackage().getId());
        dto.setPackageName(usage.getTreatmentPackage().getName());
        dto.setTreatmentId(usage.getTreatment().getId());
        dto.setTreatmentName(usage.getTreatment().getName());
        dto.setTotalSessions(usage.getTotalSessions());
        dto.setSessionsUsed(usage.getSessionsUsed());
        dto.setFollowUpDate(usage.getFollowUpDate());
        dto.setFollowUp(usage.getFollowUp());
        dto.setSessionsRemaining(usage.getSessionsRemaining());
        dto.setPackagePricePaid(usage.getPackagePricePaid());
        dto.setPurchaseDate(usage.getPurchaseDate());
        dto.setLastSessionDate(usage.getLastSessionDate());
        dto.setCompleted(usage.getCompleted());
        dto.setActive(usage.getActive());
        return dto;
    }
}