package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO for comparative doctor analytics
 */
@Getter
@Setter
public class ComparativeDoctorAnalyticsDTO {
    private List<DoctorComparisonDataDTO> doctors;
    private Map<String, Object> summaryStatistics;
    private String comparisonPeriod;
}

@Getter
@Setter
class DoctorComparisonDataDTO {
    private Long doctorId;
    private String doctorName;
    private String specialization;
    private Map<String, Object> performanceMetrics;
    private Map<String, Object> appointmentStats;
    private Integer ranking;
}