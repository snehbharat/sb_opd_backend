package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.BranchManagerDashboardService;
import com.sbpl.OPD.utils.RbacUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for branch manager dashboard endpoints.
 * Provides comprehensive reporting and KPIs for branch-level monitoring.
 */
@RestController
@RequestMapping("/api/v1/branch-manager/dashboard")
public class BranchManagerDashboardController {

    @Autowired
    private BranchManagerDashboardService branchManagerDashboardService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Get comprehensive dashboard statistics for branch manager
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getBranchDashboard() {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access branch manager dashboard");
        }
        return branchManagerDashboardService.getBranchDashboardStatistics();
    }

    /**
     * Get branch appointment statistics for a specific period
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> getBranchAppointmentStats(
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access branch appointment stats");
        }
        return branchManagerDashboardService.getBranchAppointmentStats(days);
    }

    /**
     * Get branch staff performance metrics
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/staff-performance")
    public ResponseEntity<?> getBranchStaffPerformance() {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access staff performance metrics");
        }
        return branchManagerDashboardService.getBranchStaffPerformance();
    }

    /**
     * Get branch financial overview
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/financial-overview")
    public ResponseEntity<?> getBranchFinancialOverview(
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access financial overview");
        }
        return branchManagerDashboardService.getBranchFinancialOverview(days);
    }

    /**
     * Get branch patient statistics and demographics
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/patients")
    public ResponseEntity<?> getBranchPatientStats() {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access patient statistics");
        }
        return branchManagerDashboardService.getBranchPatientStats();
    }

    /**
     * Get department-wise performance analytics
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/departments")
    public ResponseEntity<?> getDepartmentAnalytics() {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access department analytics");
        }
        return branchManagerDashboardService.getDepartmentAnalytics();
    }

    /**
     * Get branch resource utilization statistics
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/resource-utilization")
    public ResponseEntity<?> getBranchResourceUtilization(
            @RequestParam(required = false, defaultValue = "7") Integer days) {

        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access resource utilization");
        }
        return branchManagerDashboardService.getBranchResourceUtilization(days);
    }

    /**
     * Get today's quick statistics
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodaysStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access today's statistics");
        }
        return branchManagerDashboardService.getBranchDashboardStatistics();
    }

    /**
     * Get weekly overview statistics
     * Access: BRANCH_MANAGER and higher roles
     */
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyOverview() {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access weekly overview");
        }
        return branchManagerDashboardService.getBranchDashboardStatistics();
    }
}