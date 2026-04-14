package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.appointment.TreatmentPackageBulkRequest;
import com.sbpl.OPD.dto.treatment.TreatmentCreateDTO;
import com.sbpl.OPD.dto.treatment.pkg.TreatmentPackageCreateDTO;
import com.sbpl.OPD.service.PatientPackageUsageService;
import com.sbpl.OPD.service.TreatmentPackageService;
import com.sbpl.OPD.service.TreatmentService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/treatments")
public class TreatmentController {

    private static final Logger logger = LoggerFactory.getLogger(TreatmentController.class);

    @Autowired
    private TreatmentService treatmentService;

    @Autowired
    private TreatmentPackageService treatmentPackageService;

    @Autowired
    private PatientPackageUsageService patientPackageUsageService;

    @GetMapping("/price-list")
    public ResponseEntity<?> getFullPriceList(@RequestParam(required = false) Long branchId) {
        return treatmentService.getFullPriceList(branchId);
    }

    @GetMapping("/category/{categoryId}")
    public ResponseEntity<?> getByCategory(@PathVariable Long categoryId,
                                           @RequestParam(required = false) Long branchId) {
        return treatmentService.getByCategory(categoryId, branchId);
    }

    @GetMapping("/search")
    public ResponseEntity<?> search(@RequestParam String keyword,
                                    @RequestParam(required = false) Long branchId) {
        return treatmentService.search(keyword, branchId);
    }

    @PostMapping("/create")
    public ResponseEntity<?> create(@RequestBody List<TreatmentCreateDTO> dto,
                                    @RequestParam(required = false) Long branchId) {
        return treatmentService.createTreatment(dto, branchId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getById(@PathVariable Long id) {
        return treatmentService.getTreatmentById(id);
    }

    @GetMapping("/packages")
    public ResponseEntity<?> getAllPackages(@RequestParam(required = false) Long branchId) {
        return treatmentPackageService.getAllPackages(branchId);
    }

    @GetMapping("/packages/treatment/{treatmentId}")
    public ResponseEntity<?> getPackagesByTreatment(@PathVariable Long treatmentId,
                                                    @RequestParam(required = false) Long branchId) {
        return treatmentPackageService.getPackagesByTreatmentId(treatmentId, branchId);
    }

    @GetMapping("/packages/search")
    public ResponseEntity<?> searchPackages(@RequestParam String keyword,
                                            @RequestParam(required = false) Long branchId) {
        return treatmentPackageService.searchPackages(keyword, branchId);
    }

    @GetMapping("/packages/recommended")
    public ResponseEntity<?> getRecommendedPackages(@RequestParam(required = false) Long branchId) {
        return treatmentPackageService.getRecommendedPackages(branchId);
    }

    @GetMapping("/packages/{id}")
    public ResponseEntity<?> getPackageById(@PathVariable Long id) {
        return treatmentPackageService.getPackageById(id);
    }

    @PostMapping("/packages/create")
    public ResponseEntity<?> createPackage(@RequestBody List<TreatmentPackageCreateDTO> dto,
                                           @RequestParam Long treatmentId) {
        return treatmentPackageService.createPackage(dto, treatmentId);
    }

    @PutMapping("/packages/{id}")
    public ResponseEntity<?> updatePackage(@PathVariable Long id,
                                           @RequestBody TreatmentPackageCreateDTO dto) {
        return treatmentPackageService.updatePackage(id, dto);
    }

    @DeleteMapping("/packages/{id}")
    public ResponseEntity<?> deletePackage(@PathVariable Long id) {
        return treatmentPackageService.deletePackage(id);
    }

    /**
     * Get active package usage for a patient
     */
    @GetMapping("/patient-package-usage/patient/{patientId}")
    public ResponseEntity<?> getPatientPackageUsage(@PathVariable Long patientId) {
        logger.info("REST API - Getting patient package usage for patient ID: {}", patientId);
        return patientPackageUsageService.getPatientPackageUsage(patientId);
    }

    @PutMapping("/patient-package/update-session")
    public ResponseEntity<?> updateUsesSession(@RequestParam Long patientId,
                                               @RequestParam Long treatmentPackageId,
                                               @RequestParam(required = false) @Valid Boolean followUp,
                                               @RequestParam(required = false) @Valid String followUpDate) {
        logger.info("REST API - Updating session usage for patient ID: {} and package ID: {}",
                patientId, treatmentPackageId);

        if (followUp == null) {
            followUp = false;
        }

        return patientPackageUsageService.updateUsesSession(patientId, treatmentPackageId, followUp, followUpDate);
    }

    @GetMapping("/patient-package-usage/patient/{patientId}/completed")
    public ResponseEntity<?> findAllCompletedUsesPackage(@PathVariable Long patientId,
                                                         @RequestParam Integer pageNo,
                                                         @RequestParam Integer pageSize) {
        logger.info("REST API - Fetching completed package usage for patient ID: {} [pageNo={}, pageSize={}]",
                patientId, pageNo, pageSize);
        return patientPackageUsageService.findAllCompletedUsesPackage(patientId, pageNo, pageSize);
    }

    @GetMapping("/patient-package-usage/patient/{patientId}/active")
    public ResponseEntity<?> getActivePackageUsage(@PathVariable Long patientId) {
        logger.info("REST API - Fetching active package usage for patient ID: {}", patientId);
        return patientPackageUsageService.getActivePackageUsage(patientId);
    }

    @GetMapping("/patient-package-usage/patient/{patientId}/history")
    public ResponseEntity<?> getPackageUsageHistory(@PathVariable Long patientId) {
        logger.info("REST API - Fetching package usage history for patient ID: {}", patientId);
        return patientPackageUsageService.getPackageUsageHistory(patientId);
    }

    @PostMapping("/bulk-multiple")
    public ResponseEntity<?> createPackagesForMultipleTreatments(
            @RequestBody List<TreatmentPackageBulkRequest> requests) {

        return treatmentPackageService.createPackagesForMultipleTreatments(requests);
    }

}