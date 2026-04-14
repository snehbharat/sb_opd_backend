package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Setter
@Getter
public class MedicalRecordDTO {
    private Long id;
    private Long patientId;
    private Long doctorId;
    private Long appointmentId;
    private String recordType;
    private String fileName;
    private String filePath;
    private String mimeType;
    private Long fileSize;
    private String description;
    private LocalDateTime createdAt;
    private String patientName;
    private String doctorName;
    private String base64;
}