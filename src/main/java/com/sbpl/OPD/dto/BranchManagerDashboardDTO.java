package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

/**
 * DTO for branch manager dashboard statistics response
 */
@Getter
@Setter
public class BranchManagerDashboardDTO {
    private Map<String, Object> branchInfo;
    private Map<String, Object> todaysOverview;
    private Map<String, Object> weeklyPerformance;
    private Map<String, Long> appointmentStatusDistribution;
    private Map<String, Object> departmentPerformance;
    private Map<String, Object> staffMetrics;
    private Map<String, Object> patientStats;
    private Object financialOverview; // Placeholder for future financial integration
    private Object recentActivities; // List of activity maps
    private List<Map<String, Object>> staffPerformanceRanking;
    private List<Map<String, Object>> employees;
    private List<Map<String, Object>> doctors;
    private List<Map<String, Object>> latestPatients;
    private Map<String, Object> revenue;
    private Map<String, Object> billPaymentTypeStats;
    private List<Map<String, Object>> latestAppointments;
}