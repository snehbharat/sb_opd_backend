package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

/**
 * DTO for basic doctor information in analytics
 */
@Getter
@Setter
public class DoctorBasicInfoDTO {
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private String department;
    private Double consultationFee;
    private Boolean isActive;
    private Integer experienceYears;
}