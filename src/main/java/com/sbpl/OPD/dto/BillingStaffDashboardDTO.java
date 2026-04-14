package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.util.Map;

/**
 * DTO for Billing Staff dashboard statistics response
 */
@Getter
@Setter
public class BillingStaffDashboardDTO {
    private Map<String, Object> staffInfo;
    private Map<String, Object> todaysOverview;
    private Map<String, Object> weeklyPerformance;
    private Map<String, Object> monthlyPerformance;
    private Map<String, Long> paymentStatusDistribution;
    private Map<String, Object> recentTransactions;
    private Map<String, Object> billingMetrics;
    private Object recentActivities; // List of activity maps
}