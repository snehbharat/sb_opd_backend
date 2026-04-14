package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for doctor analytics and performance metrics.
 * Provides comprehensive reporting and KPIs for doctor performance monitoring.
 */
public interface DoctorAnalyticsService {

    /**
     * Get comprehensive dashboard statistics for a specific doctor
     * @param doctorId the doctor ID
     * @return dashboard statistics including appointments, performance metrics, and trends
     */
    ResponseEntity<?> getDoctorDashboardStatistics(Long doctorId);

    /**
     * Get doctor's appointment statistics for a specific period
     * @param doctorId the doctor ID
     * @param days number of days to look back (default: 30)
     * @return appointment statistics including counts by status and completion rates
     */
    ResponseEntity<?> getDoctorAppointmentStats(Long doctorId, Integer days);

    /**
     * Get doctor's performance metrics
     * @param doctorId the doctor ID
     * @return performance KPIs including completion rate, average consultation time, etc.
     */
    ResponseEntity<?> getDoctorPerformanceMetrics(Long doctorId);

    /**
     * Get doctor's patient statistics
     * @param doctorId the doctor ID
     * @return patient-related metrics including new patients, repeat patients, etc.
     */
    ResponseEntity<?> getDoctorPatientStats(Long doctorId);

    /**
     * Get doctor's revenue/billing statistics
     * @param doctorId the doctor ID
     * @param days number of days to look back (default: 30)
     * @return billing and revenue metrics
     */
    ResponseEntity<?> getDoctorRevenueStats(Long doctorId, Integer days);

    /**
     * Get doctor's schedule utilization statistics
     * @param doctorId the doctor ID
     * @param days number of days to analyze (default: 7)
     * @return schedule utilization metrics
     */
    ResponseEntity<?> getDoctorScheduleUtilization(Long doctorId, Integer days);

    /**
     * Get doctor's specialization-specific analytics
     * @param doctorId the doctor ID
     * @return specialization-based performance metrics
     */
    ResponseEntity<?> getDoctorSpecializationAnalytics(Long doctorId);

    /**
     * Get comparative analytics for multiple doctors
     * @param doctorIds list of doctor IDs to compare
     * @return comparative performance metrics
     */
    ResponseEntity<?> getComparativeDoctorAnalytics(Long[] doctorIds);

    /**
     * Get doctor's trending data for reporting
     * @param doctorId the doctor ID
     * @param days number of days for trend analysis (default: 90)
     * @return trend data for appointments, patients, and performance
     */
    ResponseEntity<?> getDoctorTrends(Long doctorId, Integer days);
}