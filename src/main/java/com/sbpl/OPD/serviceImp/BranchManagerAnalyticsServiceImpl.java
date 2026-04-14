package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.BranchManagerAnalyticsService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of branch manager analytics service.
 * Provides comprehensive performance metrics and reporting for branch-level monitoring.
 */
@Service
public class BranchManagerAnalyticsServiceImpl implements BranchManagerAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(BranchManagerAnalyticsServiceImpl.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBranchDashboardStatistics() {
        try {
            User currentUser = getCurrentUserWithBranch();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            Map<String, Object> dashboardData = new HashMap<>();

            // Basic branch info
            dashboardData.put("branchInfo", getBranchBasicInfo(currentBranch));

            // Today's overview
            dashboardData.put("todaysOverview", getTodaysBranchStats(currentBranch.getId()));

            // Weekly performance
            dashboardData.put("weeklyPerformance", getWeeklyBranchStats(currentBranch.getId()));

            // Appointment status distribution
            dashboardData.put("appointmentStatusDistribution", getBranchAppointmentStatusDistribution(currentBranch.getId()));

            // Department performance
            dashboardData.put("departmentPerformance", getDepartmentPerformance(currentBranch.getId()));

            // Staff metrics
            dashboardData.put("staffMetrics", getBranchStaffMetrics(currentBranch.getId()));

            // Patient statistics
            dashboardData.put("patientStats", getBranchPatientStatistics(currentBranch.getId()));

            // Financial overview
            dashboardData.put("financialOverview", getBranchFinancialOverview(30).getBody());

            // Recent activities
            dashboardData.put("recentActivities", getBranchRecentActivities(currentBranch.getId()));

            return baseResponse.successResponse("Branch dashboard statistics fetched successfully", dashboardData);

        } catch (Exception e) {
            logger.error("Error fetching branch dashboard statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch dashboard statistics");
        }
    }

    @Override
    public ResponseEntity<?> getBranchAppointmentStats(Integer days) {
        try {
            User currentUser = getCurrentUserWithBranch();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            int daysBack = days != null ? days : 30;
            LocalDate startDate = LocalDate.now().minusDays(daysBack);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = LocalDateTime.now();

            Map<String, Object> stats = new HashMap<>();

            // Appointment counts by status
            Map<String, Long> statusCounts = new HashMap<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
                    currentBranch.getId(), status, startDateTime, endDateTime);
                statusCounts.put(status.name(), count);
            }
            stats.put("statusCounts", statusCounts);

            // Total appointments
            long totalAppointments = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
                currentBranch.getId(), startDateTime, endDateTime);
            stats.put("totalAppointments", totalAppointments);

            // Completion rate
            long completed = statusCounts.getOrDefault("COMPLETED", 0L);
            double completionRate = totalAppointments > 0 ?
                (double) completed / totalAppointments * 100 : 0.0;
            stats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

            // Cancellation rate
            long cancelled = statusCounts.getOrDefault("CANCELLED", 0L);
            double cancellationRate = totalAppointments > 0 ?
                (double) cancelled / totalAppointments * 100 : 0.0;
            stats.put("cancellationRate", Math.round(cancellationRate * 100.0) / 100.0);

            return baseResponse.successResponse("Branch appointment stats fetched successfully", stats);

        } catch (Exception e) {
            logger.error("Error fetching branch appointment stats", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch appointment stats");
        }
    }

    @Override
    public ResponseEntity<?> getBranchStaffPerformance() {
        try {
            User currentUser = getCurrentUserWithBranch();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            // Get all doctors in branch
            List<Doctor> doctors = doctorRepository.findByBranchId(currentBranch.getId());

            List<Map<String, Object>> staffPerformance = new ArrayList<>();

            for (Doctor doctor : doctors) {
                Map<String, Object> doctorPerformance = new HashMap<>();
                doctorPerformance.put("doctorId", doctor.getId());
                doctorPerformance.put("doctorName", doctor.getDoctorName());
                doctorPerformance.put("specialization", doctor.getSpecialization());
                doctorPerformance.put("department", doctor.getDepartment());

                // Get doctor's performance metrics
                doctorPerformance.put("performanceMetrics", getDoctorPerformanceMetrics(doctor.getId()));

                staffPerformance.add(doctorPerformance);
            }

            return baseResponse.successResponse("Branch staff performance fetched successfully", staffPerformance);

        } catch (Exception e) {
            logger.error("Error fetching branch staff performance", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch staff performance");
        }
    }

    @Override
    public ResponseEntity<?> getBranchFinancialOverview(Integer days) {
        // Placeholder for financial integration
        return baseResponse.successResponse("Branch financial overview - feature coming soon",
            Map.of("message", "Financial analytics will be available in future updates"));
    }

    @Override
    public ResponseEntity<?> getBranchPatientStats() {
        try {
            User currentUser = getCurrentUserWithBranch();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            return baseResponse.successResponse("Branch patient stats fetched successfully",
                getBranchPatientStatistics(currentBranch.getId()));

        } catch (Exception e) {
            logger.error("Error fetching branch patient stats", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch patient stats");
        }
    }

    @Override
    public ResponseEntity<?> getDepartmentAnalytics() {
        try {
            User currentUser = getCurrentUserWithBranch();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            Map<String, Object> deptAnalytics = new HashMap<>();

            // Get all departments in branch
            List<String> departments = doctorRepository.findDistinctDepartmentsByBranchId(currentBranch.getId());

            Map<String, Object> departmentStats = new HashMap<>();
            for (String department : departments) {
                Map<String, Object> deptData = new HashMap<>();

                // Get doctors in department
                List<Doctor> deptDoctors = doctorRepository.findByBranchIdAndDepartment(
                    currentBranch.getId(), department, PageRequest.of(0, 100)).getContent();
                deptData.put("doctorCount", deptDoctors.size());

                // Get department appointments
                long deptAppointments = appointmentRepository.countByBranchIdAndDepartmentAndAppointmentDateBetween(
                    currentBranch.getId(), department,
                    LocalDate.now().minusDays(30).atStartOfDay(), LocalDateTime.now());
                deptData.put("appointmentsLast30Days", deptAppointments);

                departmentStats.put(department, deptData);
            }

            deptAnalytics.put("departments", departmentStats);
            deptAnalytics.put("totalDepartments", departments.size());

            return baseResponse.successResponse("Department analytics fetched successfully", deptAnalytics);

        } catch (Exception e) {
            logger.error("Error fetching department analytics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch department analytics");
        }
    }

    @Override
    public ResponseEntity<?> getBranchResourceUtilization(Integer days) {
        // Placeholder for resource utilization integration
        return baseResponse.successResponse("Branch resource utilization - feature coming soon",
            Map.of("message", "Resource utilization analytics will be available in future updates"));
    }

    @Override
    public ResponseEntity<?> getBranchTrends(Integer days) {
        try {
            User currentUser = getCurrentUserWithBranch();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            int daysBack = days != null ? days : 90;
            LocalDate startDate = LocalDate.now().minusDays(daysBack);
            LocalDate endDate = LocalDate.now();

            Map<String, Object> trends = new HashMap<>();

            // Daily appointment counts
            Map<String, Long> dailyCounts = new HashMap<>();
            LocalDate currentDate = startDate;
            while (!currentDate.isAfter(endDate)) {
                LocalDateTime dayStart = currentDate.atStartOfDay();
                LocalDateTime dayEnd = currentDate.atTime(23, 59, 59);

                long count = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
                    currentBranch.getId(), dayStart, dayEnd);
                dailyCounts.put(currentDate.toString(), count);

                currentDate = currentDate.plusDays(1);
            }
            trends.put("dailyAppointmentCounts", dailyCounts);

            // Status breakdown for period
            Map<String, Long> statusBreakdown = new HashMap<>();
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
                    currentBranch.getId(), status, startDateTime, endDateTime);
                statusBreakdown.put(status.name(), count);
            }
            trends.put("statusBreakdown", statusBreakdown);

            return baseResponse.successResponse("Branch trends fetched successfully", trends);

        } catch (Exception e) {
            logger.error("Error fetching branch trends", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch trends");
        }
    }

    @Override
    public ResponseEntity<?> getComparativeBranchAnalytics(Long[] branchIds) {
        // This would be used by higher-level admins to compare branches
        return baseResponse.successResponse("Comparative branch analytics - feature coming soon",
            Map.of("message", "Comparative analytics will be available in future updates"));
    }

    /**
     * Gets the current user with the branch eagerly loaded to avoid LazyInitializationException
     */
    private User getCurrentUserWithBranch() {
        User currentUser = DbUtill.getCurrentUser();
        
        // Fetch the user again with the branch eagerly loaded
        Optional<User> userWithBranch = userRepository.findByIdWithBranch(currentUser.getId());
        if (userWithBranch.isPresent()) {
            return userWithBranch.get();
        } else {
            throw new IllegalStateException("Current user not found in database");
        }
    }

    // Private helper methods
    private Map<String, Object> getBranchBasicInfo(Branch branch) {
        Map<String, Object> info = new HashMap<>();
        info.put("branchId", branch.getId());
        info.put("branchName", branch.getBranchName());
        info.put("address", branch.getAddress());
        info.put("phoneNumber", branch.getPhoneNumber());
        info.put("email", branch.getEmail());
        info.put("establishedDate", branch.getEstablishedDate());
        return info;
    }

    private Map<String, Object> getTodaysBranchStats(Long branchId) {
        // Use business timezone to determine "today" properly
        LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
        LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();

        Map<String, Object> stats = new HashMap<>();

        // Count appointments created today (based on createdAt field)
        long totalToday = appointmentRepository.countByBranchIdAndCreatedAtBetween(
            branchId, startOfDay, endOfDay);
        stats.put("totalAppointments", totalToday);

        // Confirmed appointments created today
        long confirmed = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
        stats.put("confirmed", confirmed);

        // Completed appointments created today
        long completed = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
        stats.put("completed", completed);

        // Pending appointments created today
        long pending = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
        stats.put("pending", pending);

        // Cancelled appointments created today
        long cancelled = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.CANCELLED, startOfDay, endOfDay);
        stats.put("cancelled", cancelled);

        // Rescheduled appointments created today
        long rescheduled = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.RESCHEDULED, startOfDay, endOfDay);
        stats.put("rescheduled", rescheduled);

        return stats;
    }

    private Map<String, Object> getWeeklyBranchStats(Long branchId) {
        LocalDate today = LocalDate.now();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
        LocalDateTime weekEndDateTime = today.atTime(23, 59, 59);

        Map<String, Object> weeklyStats = new HashMap<>();

        long weeklyTotal = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
            branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalAppointments", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
            branchId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
            (double) weeklyCompleted / weeklyTotal * 100 : 0.0;
        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        return weeklyStats;
    }

    private Map<String, Long> getBranchAppointmentStatusDistribution(Long branchId) {
        Map<String, Long> statusDistribution = new HashMap<>();

        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByBranchIdAndStatus(branchId, status);
            statusDistribution.put(status.name(), count);
        }

        return statusDistribution;
    }

    private Map<String, Object> getDepartmentPerformance(Long branchId) {
        Map<String, Object> deptPerformance = new HashMap<>();

        List<String> departments = doctorRepository.findDistinctDepartmentsByBranchId(branchId);

        for (String department : departments) {
            Map<String, Object> deptStats = new HashMap<>();

            // Get appointments for this department
            long deptAppointments = appointmentRepository.countByBranchIdAndDepartment(
                branchId, department);
            deptStats.put("totalAppointments", deptAppointments);

            // Get completed appointments
            long completedAppointments = appointmentRepository.countByBranchIdAndDepartmentAndStatus(
                branchId, department, AppointmentStatus.COMPLETED);
            deptStats.put("completedAppointments", completedAppointments);

            // Completion rate
            double completionRate = deptAppointments > 0 ?
                (double) completedAppointments / deptAppointments * 100 : 0.0;
            deptStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

            deptPerformance.put(department, deptStats);
        }

        return deptPerformance;
    }

    private Map<String, Object> getBranchStaffMetrics(Long branchId) {
        Map<String, Object> staffMetrics = new HashMap<>();

        // Get total doctors
        long totalDoctors = doctorRepository.countByBranchId(branchId);
        staffMetrics.put("totalDoctors", totalDoctors);

        // Get active doctors
        long activeDoctors = doctorRepository.countByBranchIdAndIsActive(branchId, true);
        staffMetrics.put("activeDoctors", activeDoctors);

        // Define staff roles (excluding admin roles)
        List<UserRole> staffRoles = Arrays.asList(
            UserRole.DOCTOR,
            UserRole.RECEPTIONIST,
            UserRole.BILLING_STAFF,
            UserRole.BRANCH_MANAGER
        );

        // Get total staff (all users in branch with staff roles)
        long totalStaff = userRepository.countStaffByBranchIdAndRoles(branchId, staffRoles);
        staffMetrics.put("totalStaff", totalStaff);

        // Get active staff
        long activeStaff = userRepository.countActiveStaffByBranchIdAndRoles(branchId, staffRoles);
        staffMetrics.put("activeStaff", activeStaff);

        return staffMetrics;
    }

    private Map<String, Object> getBranchPatientStatistics(Long branchId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Total patients in branch
        long totalPatients = customerRepository.countByBranchId(branchId);
        patientStats.put("totalPatients", totalPatients);

        // New patients this month
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        long newPatients = customerRepository.countByBranchIdAndCreatedAtAfter(branchId, monthStartDateTime);
        patientStats.put("newPatientsThisMonth", newPatients);

        // Active patients (with recent appointments)
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        LocalDateTime threeMonthsAgoDateTime = threeMonthsAgo.atStartOfDay();
        long activePatients = customerRepository.countActivePatientsByBranch(branchId, threeMonthsAgoDateTime);
        patientStats.put("activePatients", activePatients);

        return patientStats;
    }

    private List<Map<String, Object>> getBranchRecentActivities(Long branchId) {
        LocalDateTime oneWeekAgo = LocalDate.now().minusDays(7).atStartOfDay();

        List<Appointment> recentAppointments = appointmentRepository
            .findByBranchIdAndUpdatedAtAfterOrderByUpdatedAtDesc(
                branchId, oneWeekAgo, PageRequest.of(0, 15))
            .getContent();

        return recentAppointments.stream()
            .map(appointment -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", appointment.getId());
                activity.put("type", "APPOINTMENT_" + appointment.getStatus());
                activity.put("patientName", appointment.getPatient().getFirstName() + " " +
                    appointment.getPatient().getLastName());
                activity.put("doctorName", appointment.getDoctor().getDoctorName());
                activity.put("appointmentDate", appointment.getAppointmentDate());
                activity.put("status", appointment.getStatus());
                activity.put("updatedAt", appointment.getUpdatedAt());
                return activity;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> getDoctorPerformanceMetrics(Long doctorId) {
        Map<String, Object> metrics = new HashMap<>();

        // Get last 30 days data
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDateTime thirtyDaysAgoDateTime = thirtyDaysAgo.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long totalAppointments = appointmentRepository.countByDoctorIdAndAppointmentDateBetween(
            doctorId, thirtyDaysAgoDateTime, now);

        long completedAppointments = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.COMPLETED, thirtyDaysAgoDateTime, now);

        // Completion rate
        double completionRate = totalAppointments > 0 ?
            (double) completedAppointments / totalAppointments * 100 : 0.0;
        metrics.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // Average daily appointments
        int daysInPeriod = 30;
        double avgDaily = (double) totalAppointments / daysInPeriod;
        metrics.put("avgDailyAppointments", Math.round(avgDaily * 100.0) / 100.0);

        metrics.put("totalAppointments", totalAppointments);
        metrics.put("completedAppointments", completedAppointments);

        return metrics;
    }
}