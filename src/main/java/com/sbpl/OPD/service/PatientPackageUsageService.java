package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;


public interface PatientPackageUsageService {

    ResponseEntity<?> getPatientPackageUsage(Long patientId);

    ResponseEntity<?> updateUsesSession(Long patientId, Long treatmentPackageId,
                                        Boolean followUp, String followUpDate);

    ResponseEntity<?> findAllCompletedUsesPackage(Long patientId, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getActivePackageUsage(Long patientId);

    ResponseEntity<?> checkAvailableSessions(Long patientId, Long treatmentId);

    ResponseEntity<?> recordSessionUsage(Long patientId, Long treatmentId, Long billId);

    ResponseEntity<?> getPackageUsageHistory(Long patientId);
}