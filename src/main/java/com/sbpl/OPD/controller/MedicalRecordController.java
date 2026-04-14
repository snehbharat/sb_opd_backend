package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.medicalrecord.MedicalRecordRequestDto;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.model.MedicalRecord;
import com.sbpl.OPD.service.MedicalRecordService;
import com.sbpl.OPD.utils.RbacUtil;
import com.sbpl.OPD.utils.S3BucketStorageUtility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Base64;

/**
 * REST controller for medical record management.
 *
 * Manages APIs for uploading, retrieving,
 * and viewing patient medical documents.
 *
 * Ensures medical records are securely linked
 * to patients, doctors, and appointments.
 *
 * @author Rahul Kumar
 */


@RestController
@RequestMapping("/api/v1/medical-records")
public class MedicalRecordController {

    @Autowired
    private MedicalRecordService medicalRecordService;

    @Autowired
    private S3BucketStorageUtility s3BucketStorageUtility;
    
    @Autowired
    private RbacUtil rbacUtil;

    @PostMapping(value = "/upload", consumes = "multipart/form-data")
    public ResponseEntity<?> uploadMedicalRecord(
            @ModelAttribute MedicalRecordRequestDto requestDto) {
        return medicalRecordService.uploadMedicalRecord(requestDto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getMedicalRecordById(@PathVariable Long id) {
        return medicalRecordService.getMedicalRecordById(id);
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getMedicalRecordsByPatientId(@PathVariable Long patientId,
                                                          @RequestParam(required = false) Integer pageNo,
                                                          @RequestParam(required = false) Integer pageSize) {
        return medicalRecordService.getMedicalRecordsByPatientId(patientId,pageNo,pageSize);
    }

    @GetMapping("/doctor/{doctorId}")
    public ResponseEntity<?> getMedicalRecordsByDoctorId(@PathVariable Long doctorId,
                                                         @RequestParam(required = false) Integer pageNo,
                                                         @RequestParam(required = false) Integer pageSize) {
        return medicalRecordService.getMedicalRecordsByDoctorId(doctorId,pageNo,pageSize);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getMedicalRecordsByAppointmentId(@PathVariable Long appointmentId,
                                                              @RequestParam(required = false) Integer pageNo,
                                                              @RequestParam(required = false) Integer pageSize) {

        return medicalRecordService.getMedicalRecordsByAppointmentId(appointmentId, pageNo, pageSize);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteMedicalRecord(@PathVariable Long id) {
        // RECEPTIONIST, BILLING_STAFF, PATIENT, and doctors or higher roles can delete medical records
        if (!rbacUtil.canAccessMedicalRecords()) {
            throw new AccessDeniedException("Access denied: Insufficient role to delete medical record");
        }
        return medicalRecordService.deleteMedicalRecord(id);
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<ByteArrayResource> downloadMedicalRecord(@PathVariable Long id) {

        MedicalRecord record = medicalRecordService.getMedicalRecordEntityById(id);

        if (record == null || record.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        try {
            String s3Key = extractKeyFromS3Url(record.getFilePath());

            String base64Data = s3BucketStorageUtility.getFileAsBase64(s3Key);
            if (base64Data == null) {
                return ResponseEntity.notFound().build();
            }

            byte[] fileData = Base64.getDecoder().decode(base64Data);
            ByteArrayResource resource = new ByteArrayResource(fileData);

            return ResponseEntity.ok()
                    .header(HttpHeaders.CONTENT_DISPOSITION, "attachment;filename=" + record.getFileName())
                    .contentType(MediaType.APPLICATION_OCTET_STREAM)
                    .contentLength(fileData.length)
                    .body(resource);

        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Extract full S3 key from the S3 URL
     */
    private String extractKeyFromS3Url(String s3Url) {
        if (s3Url == null) return null;

        int index = s3Url.indexOf(".com/");
        if (index != -1 && index + 5 < s3Url.length()) {
            // everything after ".com/" is the S3 key
            return s3Url.substring(index + 5);
        }

        return s3Url;
    }


}