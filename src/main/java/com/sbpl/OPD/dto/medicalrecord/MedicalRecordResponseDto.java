package com.sbpl.OPD.dto.medicalrecord;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for medical record response data.
 *
 * @author HMS Team
 */
@Data
public class MedicalRecordResponseDto {

    private Long id;
    private String requestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Patient information
    private Long patientId;
    private String patientName;
    private String patientEmail;
    
    // Doctor information
    private Long doctorId;
    private String doctorName;
    private String doctorEmail;
    
    // Appointment information
    private Long appointmentId;
    
    // Record details
    private String recordType;
    private String fileName;
    private String filePath;
    private String mimeType;
    private Long fileSize;
    private String description;
    private Boolean isActive;
    private Long uploadedBy;
    private String accessLevel;
    private String tags;
    private Boolean isConfidential;
    
    // Access control information
    private Boolean canBeAccessedByCurrentUser;
    private String currentUserAccessLevel;
}