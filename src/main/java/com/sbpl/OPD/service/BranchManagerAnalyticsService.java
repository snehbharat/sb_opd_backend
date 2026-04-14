package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for branch manager analytics and performance metrics.
 * Provides comprehensive reporting and KPIs for branch-level monitoring.
 */
public interface BranchManagerAnalyticsService {

    /**
     * Get comprehensive dashboard statistics for branch manager
     * @return dashboard statistics including branch performance, staff metrics, and financial overview
     */
    ResponseEntity<?> getBranchDashboardStatistics();

    /**
     * Get branch appointment statistics for a specific period
     * @param days number of days to look back (default: 30)
     * @return appointment statistics including counts by status and department
     */
    ResponseEntity<?> getBranchAppointmentStats(Integer days);

    /**
     * Get branch staff performance metrics
     * @return performance KPIs for all staff members in the branch
     */
    ResponseEntity<?> getBranchStaffPerformance();

    /**
     * Get branch financial overview
     * @param days number of days to analyze (default: 30)
     * @return financial metrics including revenue, collections, and billing statistics
     */
    ResponseEntity<?> getBranchFinancialOverview(Integer days);

    /**
     * Get branch patient statistics and demographics
     * @return patient-related metrics including new patients, active patients, and trends
     */
    ResponseEntity<?> getBranchPatientStats();

    /**
     * Get department-wise performance analytics
     * @return performance metrics broken down by department
     */
    ResponseEntity<?> getDepartmentAnalytics();

    /**
     * Get branch resource utilization statistics
     * @param days number of days to analyze (default: 7)
     * @return resource utilization metrics including room usage, equipment, etc.
     */
    ResponseEntity<?> getBranchResourceUtilization(Integer days);

    /**
     * Get branch trending data for reporting
     * @param days number of days for trend analysis (default: 90)
     * @return trend data for appointments, patients, and performance
     */
    ResponseEntity<?> getBranchTrends(Integer days);

    /**
     * Get comparative analytics for multiple branches (for higher-level admins)
     * @param branchIds list of branch IDs to compare
     * @return comparative performance metrics
     */
    ResponseEntity<?> getComparativeBranchAnalytics(Long[] branchIds);
}