package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.BillingStaffDashboardService;
import com.sbpl.OPD.utils.RbacUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for billing staff dashboard endpoints.
 * Provides comprehensive reporting and KPIs for billing staff workflow.
 */
@RestController
@RequestMapping("/api/v1/billing-staff/dashboard")
public class BillingStaffDashboardController {

    @Autowired
    private BillingStaffDashboardService billingStaffDashboardService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Get comprehensive dashboard statistics for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getDashboardStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access billing staff dashboard");
        }
        return billingStaffDashboardService.getDashboardStatistics();
    }

    /**
     * Get today's quick statistics for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodaysStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access today's statistics");
        }
        return billingStaffDashboardService.getTodaysStatistics();
    }

    /**
     * Get weekly overview statistics for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyOverview() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access weekly overview");
        }
        return billingStaffDashboardService.getWeeklyOverview();
    }

    /**
     * Get monthly performance statistics for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/monthly")
    public ResponseEntity<?> getMonthlyPerformance() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access monthly performance");
        }
        return billingStaffDashboardService.getMonthlyPerformance();
    }

    /**
     * Get payment status distribution for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/payment-status")
    public ResponseEntity<?> getPaymentStatusDistribution() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access payment status distribution");
        }
        return billingStaffDashboardService.getPaymentStatusDistribution();
    }

    /**
     * Get recent billing transactions for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/recent-transactions")
    public ResponseEntity<?> getRecentTransactions(
            @RequestParam(required = false) Integer limit) {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access recent transactions");
        }
        return billingStaffDashboardService.getRecentTransactions(limit);
    }

    /**
     * Get billing metrics and KPIs for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/metrics")
    public ResponseEntity<?> getBillingMetrics() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access billing metrics");
        }
        return billingStaffDashboardService.getBillingMetrics();
    }

    /**
     * Get recent activities for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/activities")
    public ResponseEntity<?> getRecentActivities(
            @RequestParam(required = false) Integer limit) {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access recent activities");
        }
        return billingStaffDashboardService.getRecentActivities(limit);
    }

    /**
     * Get collection rate statistics for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/collection-rate")
    public ResponseEntity<?> getCollectionRateStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access collection rate statistics");
        }
        return billingStaffDashboardService.getCollectionRateStatistics();
    }

    /**
     * Get pending bills overview for billing staff
     * Access: BILLING_STAFF, RECEPTIONIST, STAFF, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/pending-bills")
    public ResponseEntity<?> getPendingBillsOverview() {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF, UserRole.RECEPTIONIST, UserRole.STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access pending bills overview");
        }
        return billingStaffDashboardService.getPendingBillsOverview();
    }
}