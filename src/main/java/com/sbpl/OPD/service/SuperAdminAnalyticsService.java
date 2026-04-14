package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

public interface SuperAdminAnalyticsService {
    
    /**
     * Get comprehensive system dashboard statistics
     * @return System-wide metrics and KPIs
     */
    ResponseEntity<?> getSystemDashboardStatistics();
    
    /**
     * Get system-wide appointment statistics
     * @return Company and branch appointment metrics
     */
    ResponseEntity<?> getSystemAppointmentStats();
    
    /**
     * Get system-wide staff performance metrics
     * @return Performance analytics across all roles
     */
    ResponseEntity<?> getSystemStaffPerformance();
    
    /**
     * Get system-wide financial overview
     * @return Revenue and billing analytics across all companies
     */
    ResponseEntity<?> getSystemFinancialOverview();
    
    /**
     * Get system-wide patient statistics
     * @return Patient metrics across entire system
     */
    ResponseEntity<?> getSystemPatientStats();
    
    /**
     * Get company analytics and performance metrics
     * @return Performance comparison between companies
     */
    ResponseEntity<?> getCompanyAnalytics();
    
    /**
     * Get system resource utilization
     * @return System capacity and resource usage metrics
     */
    ResponseEntity<?> getSystemResourceUtilization();
    
    /**
     * Get system trends and growth analytics
     * @return Historical trends and growth metrics
     */
    ResponseEntity<?> getSystemTrends();
    
    /**
     * Get system security and compliance metrics
     * @return Security audit and compliance statistics
     */
    ResponseEntity<?> getSystemSecurityMetrics();
    
    /**
     * Get system maintenance and health status
     * @return System health and maintenance metrics
     */
    ResponseEntity<?> getSystemHealthStatus();
}