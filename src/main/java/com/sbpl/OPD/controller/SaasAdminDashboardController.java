package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.SaasAdminDashboardService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for SAAS admin dashboard endpoints.
 * Provides comprehensive reporting and KPIs for company-level oversight.
 */
@RestController
@RequestMapping("/api/v1/saas-admin/dashboard")
public class SaasAdminDashboardController {

    @Autowired
    private SaasAdminDashboardService saasAdminDashboardService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Get comprehensive dashboard statistics for SAAS admin
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getCompanyDashboard(
            @RequestParam(required = false) Long branchId) {
        return saasAdminDashboardService.getCompanyDashboardStatistics(branchId);
    }

    /**
     * Get company-wide appointment statistics for a specific period
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> getCompanyAppointmentStats(
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access company appointment stats");
        }
        return saasAdminDashboardService.getCompanyAppointmentStats(days);
    }

    /**
     * Get company staff performance metrics across all branches
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/staff-performance")
    public ResponseEntity<?> getCompanyStaffPerformance() {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access staff performance metrics");
        }
        return saasAdminDashboardService.getCompanyStaffPerformance();
    }

    /**
     * Get company financial overview
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/financial-overview")
    public ResponseEntity<?> getCompanyFinancialOverview(
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access financial overview");
        }
        return saasAdminDashboardService.getCompanyFinancialOverview(days);
    }

    /**
     * Get company patient statistics and demographics
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/patients")
    public ResponseEntity<?> getCompanyPatientStats() {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access patient statistics");
        }
        return saasAdminDashboardService.getCompanyPatientStats();
    }

    /**
     * Get branch-wise performance analytics
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/branches")
    public ResponseEntity<?> getBranchAnalytics() {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access branch analytics");
        }
        return saasAdminDashboardService.getBranchAnalytics();
    }

    /**
     * Get company resource utilization statistics
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/resource-utilization")
    public ResponseEntity<?> getCompanyResourceUtilization(
            @RequestParam(required = false, defaultValue = "7") Integer days) {

        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access resource utilization");
        }
        return saasAdminDashboardService.getCompanyResourceUtilization(days);
    }

    /**
     * Get today's quick statistics
     * Access: SAAS_ADMIN and higher roles
     */
//    @GetMapping("/today")
//    public ResponseEntity<?> getTodaysStatistics() {
//        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
//                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
//            throw new AccessDeniedException("Insufficient permissions to access today's statistics");
//        }
//        return saasAdminDashboardService.getCompanyDashboardStatistics();
//    }

    /**
     * Get weekly overview statistics
     * Access: SAAS_ADMIN and higher roles
     */
//    @GetMapping("/weekly")
//    public ResponseEntity<?> getWeeklyOverview() {
//        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
//                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
//            throw new AccessDeniedException("Insufficient permissions to access weekly overview");
//        }
//        return saasAdminDashboardService.getCompanyDashboardStatistics();
//    }

    /**
     * Get comparative analytics for multiple companies (for super admins)
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/comparative")
    public ResponseEntity<?> getComparativeCompanyAnalytics(
            @RequestParam @NotNull Long[] companyIds) {

        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions for comparative company analytics");
        }
        return saasAdminDashboardService.getComparativeCompanyAnalytics(companyIds);
    }

    /**
     * Get system health and operational metrics
     * Access: SAAS_ADMIN and higher roles
     */
    @GetMapping("/health-metrics")
    public ResponseEntity<?> getSystemHealthMetrics() {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, 
                UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system health metrics");
        }
        return saasAdminDashboardService.getSystemHealthMetrics();
    }
}