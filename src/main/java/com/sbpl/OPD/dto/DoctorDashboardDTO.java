package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for doctor dashboard statistics response
 */
@Getter
@Setter
public class DoctorDashboardDTO {
    private DoctorBasicInfoDTO doctorInfo;
    private Map<String, Object> todaysAppointments;
    private Map<String, Object> weeklyPerformance;
    private Map<String, Long> appointmentStatusDistribution;
    private Map<String, Object> patientStats;
    private Object upcomingAppointments; // List of AppointmentDTO
    private Object recentActivities; // List of activity maps
    private Map<String, Object> performanceMetrics;
}