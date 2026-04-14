package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO for receptionist dashboard statistics response
 */
@Getter
@Setter
public class ReceptionistDashboardDTO {
    private Map<String, Object> branchInfo;
    private Map<String, Object> todaysAppointments;
    private Map<String, Object> weeklyAppointments;
    private Map<String, Long> appointmentStatusDistribution;
    private Map<String, Object> patientStats;
//    private Object upcomingAppointments; // List of AppointmentDTO
//    private Object recentActivities; // List of activity maps

    private List<Map<String, Object>> staffPerformanceRanking;

    private List<Map<String, Object>> employees;

    private List<Map<String, Object>> doctors;

    private List<Map<String, Object>> latestPatients;

    private Map<String, Object> revenueSummary;

    private List<Map<String, Object>> latestAppointments;

    private Map<String, Object> billPaymentTypeStats;

    private Map<String, Object> staffMetrics;

    private Map<String, Object> branchPerformance;

    private Map<String, Object> weeklyPerformance;

}