package com.sbpl.OPD.dto.Doctor.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Minimal doctor data transfer object for appointment-related operations.
 * Contains essential information needed for displaying doctors in appointment context.
 */
@Getter
@Setter
public class DoctorMinimalDTO {

    private Long doctorId;
    private String prefix;
    private String doctorName;
    private String doctorEmail;
    private String phoneNumber;
    private String specialization;
    private String department;
    private Boolean onlineConsultationAvailable;
    private Boolean active;
    private String consultationRoom;
    private String registrationNumber;
    private Boolean isActive;

    private Long companyId;
    private Long branchId;
    
    private Long coreExpertiseId;
    private String coreExpertiseName;
}