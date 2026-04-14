package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.appointment.TreatmentPackageBulkRequest;
import com.sbpl.OPD.dto.treatment.pkg.TreatmentPackageCreateDTO;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

public interface TreatmentPackageService {

    ResponseEntity<?> getAllPackages(@RequestParam(required = false) Long branchId);

    ResponseEntity<?> getPackagesByTreatmentId(Long treatmentId, Long branchId);

    ResponseEntity<?> searchPackages(String keyword, Long branchId);

    ResponseEntity<?> getRecommendedPackages(Long branchId);

//    ResponseEntity<?> createPackage(List<TreatmentPackageCreateDTO> dtoList, Long branchId);


    ResponseEntity<?> createPackage(List<TreatmentPackageCreateDTO> dtoList,Long treatmentId);

    ResponseEntity<?> updatePackage(Long id, TreatmentPackageCreateDTO dto);

    ResponseEntity<?> deletePackage(Long id);

    ResponseEntity<?> getPackageById(Long id);

    ResponseEntity<?> createPackagesBulk(TreatmentPackageBulkRequest request);

    ResponseEntity<?> createPackagesForMultipleTreatments(
            List<TreatmentPackageBulkRequest> requests);
}