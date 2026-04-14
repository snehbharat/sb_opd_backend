package com.sbpl.OPD.dto.medicalrecord;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

/**
 * DTO for medical record creation requests.
 *
 * @author HMS Team
 */
@Data
public class MedicalRecordRequestDto {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Appointment ID is required")
    private Long appointmentId;

    @NotBlank(message = "Record type is required")
    private String recordType;

    @NotBlank(message = "Description is required")
    private String description;

    private String uhid;

    private String accessLevel = "PATIENT_ONLY";
    private String tags;
    private Boolean isConfidential = false;

    private MultipartFile record;
}