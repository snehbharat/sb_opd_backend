package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for doctor performance metrics
 */
@Getter
@Setter
public class DoctorPerformanceMetricsDTO {
    private Double completionRate;
    private Double cancellationRate;
    private Double avgDailyAppointments;
    private Double patientRetentionRate;
    private Long totalAppointments;
    private Long completedAppointments;
    private Long cancelledAppointments;
    private Map<String, Object> additionalMetrics;
}