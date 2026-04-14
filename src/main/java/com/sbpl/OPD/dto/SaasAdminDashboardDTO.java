package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for SAAS admin dashboard statistics response
 */
@Getter
@Setter
public class SaasAdminDashboardDTO {
    private Map<String, Object> companyInfo;
    private Map<String, Object> branchInfo; // Optional: populated when viewing branch-specific dashboard
    private Map<String, Object> todaysOverview;
    private Map<String, Object> weeklyPerformance;
    private Map<String, Long> appointmentStatusDistribution;
    private Map<String, Object> branchPerformance;
    private Map<String, Object> staffMetrics;
    private Map<String, Object> patientStats;
    private Object recentActivities; // List of activity maps

    private Object staffPerformanceRanking;   // ✅ ADD THIS
    private Object employees;
    private Object doctors;
    private Object latestPatients;
    private Map<String, Object> revenueSummary;

    private Map<String, Object> billPaymentTypeStats;

    private Object latestAppointments;
}