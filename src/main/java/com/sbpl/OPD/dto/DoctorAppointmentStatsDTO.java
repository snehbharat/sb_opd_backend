package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for doctor appointment statistics
 */
@Getter
@Setter
public class DoctorAppointmentStatsDTO {
    private Map<String, Long> statusCounts;
    private Long totalAppointments;
    private Double completionRate;
    private Double cancellationRate;
    private Integer periodDays;
}