package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.service.DoctorDashboardService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * REST controller for doctor dashboard endpoints.
 * Provides comprehensive reporting and KPIs for doctor performance monitoring.
 */
@RestController
@RequestMapping("/api/v1/doctors/dashboard")
public class DoctorDashboardController {

    @Autowired
    private DoctorDashboardService doctorDashboardService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Get comprehensive dashboard statistics for a specific doctor
     * Access: DOCTOR, BRANCH_MANAGER, and higher roles
     * Doctors can only access their own dashboard
     */
    @GetMapping("/{doctorId}")
    public ResponseEntity<?> getDoctorDashboard(
            @PathVariable @NotNull Long doctorId,
            @RequestParam(required = false) Long requestedDoctorId) {

        // Validate access - doctors can only see their own data
        try {
            if (rbacUtil.hasRole(UserRole.DOCTOR)) {
                Long currentDoctorId = getCurrentDoctorId();
                if (!doctorId.equals(currentDoctorId)) {
                    return ResponseEntity.status(403).body(Map.of("error", "Doctors can only access their own dashboard"));
                }
            } else if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                    UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to access doctor dashboard"));
            }
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Authentication system not properly configured: " + e.getMessage()));
        }

        return doctorDashboardService.getDoctorDashboardStatistics(doctorId);
    }

    /**
     * Get doctor's appointment statistics for a specific period
     * Access: DOCTOR, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/{doctorId}/appointments")
    public ResponseEntity<?> getDoctorAppointmentStats(
            @PathVariable @NotNull Long doctorId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        ResponseEntity<?> validationError = validateDoctorAccess(doctorId);
        if (validationError != null) {
            return validationError;
        }
        return doctorDashboardService.getDoctorAppointmentStats(doctorId, days);
    }

    /**
     * Get doctor's performance metrics
     * Access: DOCTOR, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/{doctorId}/performance")
    public ResponseEntity<?> getDoctorPerformanceMetrics(
            @PathVariable @NotNull Long doctorId) {

        ResponseEntity<?> validationError = validateDoctorAccess(doctorId);
        if (validationError != null) {
            return validationError;
        }
        return doctorDashboardService.getDoctorPerformanceMetrics(doctorId);
    }

    /**
     * Get doctor's patient statistics
     * Access: DOCTOR, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/{doctorId}/patients")
    public ResponseEntity<?> getDoctorPatientStats(
            @PathVariable @NotNull Long doctorId) {

        ResponseEntity<?> validationError = validateDoctorAccess(doctorId);
        if (validationError != null) {
            return validationError;
        }
        return doctorDashboardService.getDoctorPatientStats(doctorId);
    }

    /**
     * Get doctor's revenue/billing statistics
     * Access: DOCTOR, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/{doctorId}/revenue")
    public ResponseEntity<?> getDoctorRevenueStats(
            @PathVariable @NotNull Long doctorId,
            @RequestParam(required = false, defaultValue = "30") Integer days) {

        ResponseEntity<?> validationError = validateDoctorAccess(doctorId);
        if (validationError != null) {
            return validationError;
        }
        return doctorDashboardService.getDoctorRevenueStats(doctorId, days);
    }

    /**
     * Get doctor's schedule utilization statistics
     * Access: DOCTOR, BRANCH_MANAGER, and higher roles
     */
    @GetMapping("/{doctorId}/schedule-utilization")
    public ResponseEntity<?> getDoctorScheduleUtilization(
            @PathVariable @NotNull Long doctorId,
            @RequestParam(required = false, defaultValue = "7") Integer days) {

        ResponseEntity<?> validationError = validateDoctorAccess(doctorId);
        if (validationError != null) {
            return validationError;
        }
        return doctorDashboardService.getDoctorScheduleUtilization(doctorId, days);
    }

    /**
     * Validate doctor access based on user role
     */
    private ResponseEntity<?> validateDoctorAccess(Long doctorId) {
        try {
            if (rbacUtil.hasRole(UserRole.DOCTOR)) {
                Long currentDoctorId = getCurrentDoctorId();
                if (!doctorId.equals(currentDoctorId)) {
                    return ResponseEntity.status(403).body(Map.of("error", "Doctors can only access their own dashboard"));
                }
            } else if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN,
                    UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
                return ResponseEntity.status(403).body(Map.of("error", "Insufficient permissions to access doctor dashboard"));
            }
            return null; // No error
        } catch (IllegalStateException e) {
            return ResponseEntity.status(500).body(Map.of("error", "Authentication system not properly configured: " + e.getMessage()));
        }
    }

    /**
     * Get current doctor ID from authenticated user
     */
    private Long getCurrentDoctorId() {
        // This retrieves the doctor ID associated with the current authenticated user
        // Implementation depends on how doctor-user relationship is stored in your system
        
        // Placeholder implementation - replace with your actual auth system integration
        // For now, we'll extract from JWT token if available
        try {
            // This would typically extract doctor ID from JWT claims
            // Example: return jwtUtil.extractDoctorId();
            
            // Temporary workaround - return a default value for testing
            // In production, this should be properly implemented with your auth system
            return 1L; // Replace with actual implementation
            
        } catch (Exception e) {
            throw new IllegalStateException("Unable to retrieve doctor ID from authentication context", e);
        }
    }
}