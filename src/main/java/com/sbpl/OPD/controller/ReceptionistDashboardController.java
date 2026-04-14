package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.ReceptionistDashboardService;
import com.sbpl.OPD.utils.RbacUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for receptionist dashboard endpoints.
 * Provides comprehensive reporting and KPIs for receptionist workflow.
 */
@RestController
@RequestMapping("/api/v1/receptionist/dashboard")
public class ReceptionistDashboardController {

    @Autowired
    private ReceptionistDashboardService receptionistDashboardService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Get comprehensive dashboard statistics for receptionist
     * Access: RECEPTIONIST, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getDashboardStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER,
                UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access receptionist dashboard");
        }
        return receptionistDashboardService.getDashboardStatistics();
    }

    /**
     * Get today's quick statistics
     * Access: RECEPTIONIST, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/today")
    public ResponseEntity<?> getTodaysStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER,
                UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access today's statistics");
        }
        return receptionistDashboardService.getTodaysStatistics();
    }

    /**
     * Get weekly overview statistics
     * Access: RECEPTIONIST, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/weekly")
    public ResponseEntity<?> getWeeklyOverview() {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER,
                UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access weekly overview");
        }
        return receptionistDashboardService.getWeeklyOverview();
    }

    /**
     * Get appointment statistics for the branch
     * Access: RECEPTIONIST, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> getAppointmentsStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER,
                UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access appointment statistics");
        }
        return receptionistDashboardService.getAppointmentsStatistics();
    }

    /**
     * Get patient statistics for the branch
     * Access: RECEPTIONIST, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/patients")
    public ResponseEntity<?> getPatientStatistics() {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER,
                UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access patient statistics");
        }
        return receptionistDashboardService.getPatientStatistics();
    }

    /**
     * Get recent activities for the branch
     * Access: RECEPTIONIST, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/activities")
    public ResponseEntity<?> getRecentActivities() {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BRANCH_MANAGER,
                UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to access recent activities");
        }
        return receptionistDashboardService.getRecentActivities();
    }
}