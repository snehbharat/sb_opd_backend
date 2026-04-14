package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for billing staff dashboard operations.
 * Provides comprehensive dashboard statistics and analytics for billing staff.
 */
public interface BillingStaffDashboardService {

    /**
     * Get comprehensive dashboard statistics for billing staff
     * @return ResponseEntity with dashboard statistics including staff info, today's overview,
     *         weekly performance, monthly performance, payment status distribution,
     *         recent transactions, billing metrics, and recent activities
     */
    ResponseEntity<?> getDashboardStatistics();

    /**
     * Get today's quick statistics for billing staff
     * @return ResponseEntity with today's billing statistics
     */
    ResponseEntity<?> getTodaysStatistics();

    /**
     * Get weekly overview statistics for billing staff
     * @return ResponseEntity with weekly billing performance data
     */
    ResponseEntity<?> getWeeklyOverview();

    /**
     * Get monthly performance statistics for billing staff
     * @return ResponseEntity with monthly billing metrics
     */
    ResponseEntity<?> getMonthlyPerformance();

    /**
     * Get payment status distribution for billing staff
     * @return ResponseEntity with distribution of bills by payment status
     */
    ResponseEntity<?> getPaymentStatusDistribution();

    /**
     * Get recent billing transactions for billing staff
     * @param limit Number of recent transactions to retrieve
     * @return ResponseEntity with recent transaction data
     */
    ResponseEntity<?> getRecentTransactions(Integer limit);

    /**
     * Get billing metrics and KPIs for billing staff
     * @return ResponseEntity with key billing performance indicators
     */
    ResponseEntity<?> getBillingMetrics();

    /**
     * Get recent activities for billing staff
     * @param limit Number of recent activities to retrieve
     * @return ResponseEntity with recent billing activities
     */
    ResponseEntity<?> getRecentActivities(Integer limit);

    /**
     * Get collection rate statistics for billing staff
     * @return ResponseEntity with collection rate metrics
     */
    ResponseEntity<?> getCollectionRateStatistics();

    /**
     * Get pending bills overview for billing staff
     * @return ResponseEntity with pending bills summary
     */
    ResponseEntity<?> getPendingBillsOverview();
}