package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for receptionist dashboard and analytics.
 * Provides comprehensive reporting and KPIs for receptionist workflow.
 */
public interface ReceptionistDashboardService {

    /**
     * Get comprehensive dashboard statistics for receptionist
     * @return ReceptionistDashboardDTO containing all dashboard metrics
     */
    ResponseEntity<?> getDashboardStatistics();

    /**
     * Get today's quick statistics
     * @return Today's metrics for quick overview
     */
    ResponseEntity<?> getTodaysStatistics();

    /**
     * Get weekly overview statistics
     * @return Weekly metrics for trend analysis
     */
    ResponseEntity<?> getWeeklyOverview();

    /**
     * Get appointment statistics for the branch
     * @return Appointment-related metrics and trends
     */
    ResponseEntity<?> getAppointmentsStatistics();

    /**
     * Get patient statistics for the branch
     * @return Patient-related metrics and trends
     */
    ResponseEntity<?> getPatientStatistics();

    /**
     * Get recent activities for the branch
     * @return Recent activity logs and updates
     */
    ResponseEntity<?> getRecentActivities();
}