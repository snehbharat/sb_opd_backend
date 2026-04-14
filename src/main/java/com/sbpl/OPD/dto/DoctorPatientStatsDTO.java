package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for doctor patient statistics
 */
@Getter
@Setter
public class DoctorPatientStatsDTO {
    private Long totalUniquePatients;
    private Long newPatientsThisMonth;
    private Long repeatPatients;
    private Double patientRetentionRate;
    private Map<String, Object> patientDemographics;
}