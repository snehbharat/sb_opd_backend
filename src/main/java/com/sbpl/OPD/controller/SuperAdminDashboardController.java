package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.SuperAdminDashboardService;
import com.sbpl.OPD.utils.RbacUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for super admin dashboard endpoints.
 * Provides comprehensive system-wide reporting and KPIs.
 */
@RestController
@RequestMapping("/api/v1/super-admin/dashboard")
public class SuperAdminDashboardController {

    @Autowired
    private SuperAdminDashboardService superAdminDashboardService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Get comprehensive system dashboard statistics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getSystemDashboard() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access super admin dashboard");
        }
        return superAdminDashboardService.getSystemDashboardStatistics();
    }

    /**
     * Get system-wide appointment statistics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> getSystemAppointmentStats() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system appointment stats");
        }
        return superAdminDashboardService.getSystemAppointmentStats();
    }

    /**
     * Get system-wide staff performance metrics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/staff-performance")
    public ResponseEntity<?> getSystemStaffPerformance() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access staff performance metrics");
        }
        return superAdminDashboardService.getSystemStaffPerformance();
    }

    /**
     * Get system-wide financial overview
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/financial-overview")
    public ResponseEntity<?> getSystemFinancialOverview() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system financial overview");
        }
        return superAdminDashboardService.getSystemFinancialOverview();
    }

    /**
     * Get system-wide patient statistics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/patients")
    public ResponseEntity<?> getSystemPatientStats() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system patient stats");
        }
        return superAdminDashboardService.getSystemPatientStats();
    }

    /**
     * Get company analytics and performance metrics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/companies")
    public ResponseEntity<?> getCompanyAnalytics() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access company analytics");
        }
        return superAdminDashboardService.getCompanyAnalytics();
    }

    /**
     * Get system resource utilization
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/resource-utilization")
    public ResponseEntity<?> getSystemResourceUtilization() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system resource utilization");
        }
        return superAdminDashboardService.getSystemResourceUtilization();
    }

    /**
     * Get system trends and growth analytics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/trends")
    public ResponseEntity<?> getSystemTrends() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system trends");
        }
        return superAdminDashboardService.getSystemTrends();
    }

    /**
     * Get system security and compliance metrics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/security-metrics")
    public ResponseEntity<?> getSystemSecurityMetrics() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system security metrics");
        }
        return superAdminDashboardService.getSystemSecurityMetrics();
    }

    /**
     * Get system maintenance and health status
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/health-status")
    public ResponseEntity<?> getSystemHealthStatus() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access system health status");
        }
        return superAdminDashboardService.getSystemHealthStatus();
    }

    /**
     * Get today's quick statistics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodaysStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access today's statistics");
        }
        return superAdminDashboardService.getSystemDashboardStatistics();
    }

    /**
     * Get weekly overview statistics
     * Access: SUPER_ADMIN and higher roles only
     */
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyOverview() {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access weekly overview");
        }
        return superAdminDashboardService.getSystemDashboardStatistics();
    }
}