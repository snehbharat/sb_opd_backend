package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for SAAS admin analytics and company-wide monitoring.
 * Provides comprehensive reporting and KPIs for company-level oversight.
 */
public interface SaasAdminAnalyticsService {

    /**
     * Get comprehensive dashboard statistics for SAAS admin
     * @return dashboard statistics including company performance, branch metrics, and financial overview
     */
    ResponseEntity<?> getCompanyDashboardStatistics();

    /**
     * Get company-wide appointment statistics for a specific period
     * @param days number of days to look back (default: 30)
     * @return appointment statistics including counts by status and branch
     */
    ResponseEntity<?> getCompanyAppointmentStats(Integer days);

    /**
     * Get company staff performance metrics across all branches
     * @return performance KPIs for all staff members in the company
     */
    ResponseEntity<?> getCompanyStaffPerformance();

    /**
     * Get company financial overview
     * @param days number of days to analyze (default: 30)
     * @return financial metrics including revenue, collections, and billing statistics
     */
    ResponseEntity<?> getCompanyFinancialOverview(Integer days);

    /**
     * Get company patient statistics and demographics
     * @return patient-related metrics including new patients, active patients, and trends
     */
    ResponseEntity<?> getCompanyPatientStats();

    /**
     * Get branch-wise performance analytics
     * @return performance metrics broken down by branch
     */
    ResponseEntity<?> getBranchAnalytics();

    /**
     * Get company resource utilization statistics
     * @param days number of days to analyze (default: 7)
     * @return resource utilization metrics including capacity planning
     */
    ResponseEntity<?> getCompanyResourceUtilization(Integer days);

    /**
     * Get company trending data for reporting
     * @param days number of days for trend analysis (default: 90)
     * @return trend data for appointments, patients, and performance
     */
    ResponseEntity<?> getCompanyTrends(Integer days);

    /**
     * Get comparative analytics for multiple companies (for super admins)
     * @param companyIds list of company IDs to compare
     * @return comparative performance metrics
     */
    ResponseEntity<?> getComparativeCompanyAnalytics(Long[] companyIds);

    /**
     * Get system health and operational metrics
     * @return system performance, user activity, and operational health indicators
     */
    ResponseEntity<?> getSystemHealthMetrics();
}