package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.VitalSignsDTO;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for vital signs operations.
 * Handles vital sign recording, retrieval, and management.
 */
public interface VitalSignsService {
    ResponseEntity<?> createVitalSigns(VitalSignsDTO vitalSignsDTO);
    ResponseEntity<?> getVitalSignsById(Long id);
    ResponseEntity<?> getVitalSignsByAppointment(Long appointmentId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> getVitalSignsByPatient(Long patientId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> updateVitalSigns(Long id, VitalSignsDTO vitalSignsDTO);
    ResponseEntity<?> deleteVitalSigns(Long id);
    
    // Employee self-service methods
    ResponseEntity<?> createMyVitalSigns(VitalSignsDTO vitalSignsDTO);
    ResponseEntity<?> getMyVitalSignsByAppointment(Long appointmentId);
    ResponseEntity<?> getMyVitalSigns(Integer pageNo, Integer pageSize);
}