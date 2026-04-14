package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.medicalrecord.MedicalRecordRequestDto;
import com.sbpl.OPD.model.MedicalRecord;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for medical record operations.
 *
 * Handles medical record creation, retrieval, and management.
 *
 * @author Rahul Kumar
 */
public interface MedicalRecordService {
    ResponseEntity<?> uploadMedicalRecord(MedicalRecordRequestDto requestDto);
    ResponseEntity<?> getMedicalRecordById(Long id);
    MedicalRecord getMedicalRecordEntityById(Long id);

    ResponseEntity<?> getMedicalRecordsByPatientId(Long patientId, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getMedicalRecordsByDoctorId(Long doctorId, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getMedicalRecordsByAppointmentId(Long appointmentId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> deleteMedicalRecord(Long id);
    byte[] downloadMedicalRecord(Long id);
}