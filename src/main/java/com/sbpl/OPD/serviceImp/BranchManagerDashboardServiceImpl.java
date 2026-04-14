package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.BranchManagerDashboardDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.BranchManagerDashboardService;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of branch manager dashboard service.
 * Provides comprehensive performance metrics and reporting for branch-level monitoring.
 */
@Service
public class BranchManagerDashboardServiceImpl implements BranchManagerDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(BranchManagerDashboardServiceImpl.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private DashboardCommonService dashboardCommonService;

    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getBranchDashboardStatistics() {
        try {
            User currentUser = getCurrentUserWithBranch();

            Long companyId = DbUtill.getLoggedInCompanyId();

            if (currentUser.getBranch() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
            }

            Branch currentBranch = currentUser.getBranch();

            BranchManagerDashboardDTO dashboardDTO = new BranchManagerDashboardDTO();

            // Set branch info
            Map<String, Object> branchInfo = new HashMap<>();
            branchInfo.put("branchId", currentBranch.getId());
            branchInfo.put("branchName", currentBranch.getBranchName());
            branchInfo.put("address", currentBranch.getAddress());
            branchInfo.put("phoneNumber", currentBranch.getPhoneNumber());
            branchInfo.put("email", currentBranch.getEmail());
            branchInfo.put("establishedDate", currentBranch.getEstablishedDate());
            dashboardDTO.setBranchInfo(branchInfo);

            // Today's overview
            dashboardDTO.setTodaysOverview(getTodaysBranchStats(currentBranch.getId()));

            // Weekly performance
            dashboardDTO.setWeeklyPerformance(getWeeklyBranchStats(currentBranch.getId()));

            // Appointment status distribution
            dashboardDTO.setAppointmentStatusDistribution(getBranchAppointmentStatusDistribution(currentBranch.getId()));

            // Department performance
            dashboardDTO.setDepartmentPerformance(getDepartmentPerformance(currentBranch.getId()));

            // Staff metrics
            dashboardDTO.setStaffMetrics(getBranchStaffMetrics(currentBranch.getId()));

            // Patient statistics
            dashboardDTO.setPatientStats(getBranchPatientStatistics(currentBranch.getId()));


            // Recent activities
            dashboardDTO.setRecentActivities(getBranchRecentActivities(currentBranch.getId()));

            dashboardDTO.setStaffPerformanceRanking(
                    dashboardCommonService.getStaffPerformanceRanking(companyId, currentBranch.getId())
            );

            dashboardDTO.setEmployees(
                    dashboardCommonService.getEmployees(companyId, currentBranch.getId())
            );

            dashboardDTO.setDoctors(
                    dashboardCommonService.getDoctors(companyId, currentBranch.getId())
            );

            dashboardDTO.setLatestPatients(
                    dashboardCommonService.getLatestPatients(companyId)
            );

            dashboardDTO.setRevenue(
                    dashboardCommonService.getRevenue(companyId, currentBranch.getId())
            );

            dashboardDTO.setBillPaymentTypeStats(
                    dashboardCommonService.getBillPaymentTypeStats(companyId, currentBranch.getId())
            );

            dashboardDTO.setLatestAppointments(
                    dashboardCommonService.getLatestAppointments(companyId, currentBranch.getId())
            );

            return baseResponse.successResponse("Branch dashboard statistics fetched successfully", dashboardDTO);

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
            // Use business timezone (IST) instead of server timezone
            LocalDate startDate = DateUtils.getBusinessLocalDate().minusDays(daysBack);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = DateUtils.getEndOfBusinessDay();

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

            // No-show rate
            long noShow = statusCounts.getOrDefault("NO_SHOW", 0L);
            double noShowRate = totalAppointments > 0 ?
                (double) noShow / totalAppointments * 100 : 0.0;
            stats.put("noShowRate", Math.round(noShowRate * 100.0) / 100.0);

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

            Map<String, Object> staffPerformance = new HashMap<>();

            for (Doctor doctor : doctors) {
                Map<String, Object> doctorPerformance = new HashMap<>();
                doctorPerformance.put("doctorId", doctor.getId());
                doctorPerformance.put("doctorName", doctor.getDoctorName());
                doctorPerformance.put("specialization", doctor.getSpecialization());
                doctorPerformance.put("department", doctor.getDepartment());

                // Get doctor's performance metrics
                doctorPerformance.put("performanceMetrics", getDoctorPerformanceMetrics(doctor.getId()));

                staffPerformance.put(doctor.getDoctorName(), doctorPerformance);
            }

            return baseResponse.successResponse("Branch staff performance fetched successfully", staffPerformance);

        } catch (Exception e) {
            logger.error("Error fetching branch staff performance", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch staff performance");
        }
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
                // Use business timezone (IST) instead of server timezone
                long deptAppointments = appointmentRepository.countByBranchIdAndDepartmentAndAppointmentDateBetween(
                    currentBranch.getId(), department,
                    DateUtils.getBusinessLocalDate().minusDays(30).atStartOfDay(), LocalDateTime.now(DateUtils.getBusinessZone()));
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
    private Map<String, Object> getTodaysBranchStats(Long branchId) {
        // Use business timezone to determine "today" properly
        LocalDate today = DateUtils.getBusinessLocalDate();
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

        // No-show appointments created today
        long noShow = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.NO_SHOW, startOfDay, endOfDay);
        stats.put("noShow", noShow);

        return stats;
    }

    private Map<String, Object> getWeeklyBranchStats(Long branchId) {
        // Use business timezone to determine dates properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime weekStartDateTime = DateUtils.getStartOfBusinessDay(weekStart);
        LocalDateTime weekEndDateTime = DateUtils.getEndOfBusinessDay(today);

        Map<String, Object> weeklyStats = new HashMap<>();

        // Appointment date based statistics
        long weeklyTotal = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
            branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalAppointments", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
            branchId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
            (double) weeklyCompleted / weeklyTotal * 100 : 0.0;
        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // No-show appointments in week
        long weeklyNoShow = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
            branchId, AppointmentStatus.NO_SHOW, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("noShow", weeklyNoShow);

        double noShowRate = weeklyTotal > 0 ?
            (double) weeklyNoShow / weeklyTotal * 100 : 0.0;
        weeklyStats.put("noShowRate", Math.round(noShowRate * 100.0) / 100.0);

        // Creation date based statistics
        long weeklyCreatedTotal = appointmentRepository.countByBranchIdAndCreatedAtBetween(
            branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalCreated", weeklyCreatedTotal);

        long weeklyCreatedCompleted = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("createdCompleted", weeklyCreatedCompleted);

        double createdCompletionRate = weeklyCreatedTotal > 0 ?
            (double) weeklyCreatedCompleted / weeklyCreatedTotal * 100 : 0.0;
        weeklyStats.put("createdCompletionRate", Math.round(createdCompletionRate * 100.0) / 100.0);

        return weeklyStats;
    }

    private Map<String, Long> getBranchAppointmentStatusDistribution(Long branchId) {
        Map<String, Long> statusDistribution = new HashMap<>();

        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByBranchIdAndStatus(branchId, status);
            statusDistribution.put(status.name(), count);
        }
//
//        // Add no-show rate calculation
//        Long total = statusDistribution.values().stream().mapToLong(Long::longValue).sum();
//        Long noShowCount = statusDistribution.getOrDefault("NO_SHOW", 0L);
//        double noShowRate = total > 0 ? (double) noShowCount / total * 100 : 0.0;
//        statusDistribution.put("noShowRate", Math.round(noShowRate * 100.0) / 100.0);

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
        logger.info("Calculating staff metrics for branch ID: {}", branchId);
        Map<String, Object> staffMetrics = new HashMap<>();

        try {
            // Get total doctors
            long totalDoctors = doctorRepository.countByBranchId(branchId);
            staffMetrics.put("totalDoctors", totalDoctors);
            logger.info("Total doctors in branch {}: {}", branchId, totalDoctors);

            // Get active doctors
            long activeDoctors = doctorRepository.countByBranchIdAndIsActive(branchId, true);
            staffMetrics.put("activeDoctors", activeDoctors);
            logger.info("Active doctors in branch {}: {}", branchId, activeDoctors);

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
            logger.info("Total staff in branch {}: {}", branchId, totalStaff);

            // Get active staff
            long activeStaff = userRepository.countActiveStaffByBranchIdAndRoles(branchId, staffRoles);
            staffMetrics.put("activeStaff", activeStaff);
            logger.info("Active staff in branch {}: {}", branchId, activeStaff);

            logger.info("Staff metrics calculation completed successfully for branch {}", branchId);
            return staffMetrics;
            
        } catch (Exception e) {
            logger.error("Error calculating staff metrics for branch {}: {}", branchId, e.getMessage(), e);
            // Return fallback values with error indication
            staffMetrics.put("totalDoctors", 0L);
            staffMetrics.put("activeDoctors", 0L);
            staffMetrics.put("totalStaff", "ERROR: " + e.getMessage());
            staffMetrics.put("activeStaff", 0L);
            return staffMetrics;
        }
    }

    private Map<String, Object> getBranchPatientStatistics(Long branchId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();

        // Total patients in branch
        long totalPatients = customerRepository.countByBranchId(branchId);
        patientStats.put("totalPatients", totalPatients);

        // New patients this month
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = DateUtils.getStartOfBusinessDay(monthStart);
        long newPatients = customerRepository.countByBranchIdAndCreatedAtAfter(branchId, monthStartDateTime);
        patientStats.put("newPatientsThisMonth", newPatients);

        // Active patients (with recent appointments)
        LocalDate threeMonthsAgo = today.minusMonths(3);
        LocalDateTime threeMonthsAgoDateTime = DateUtils.getStartOfBusinessDay(threeMonthsAgo);
        long activePatients = customerRepository.countActivePatientsByBranch(branchId, threeMonthsAgoDateTime);
        patientStats.put("activePatients", activePatients);

        return patientStats;
    }

    private List<Map<String, Object>> getBranchRecentActivities(Long branchId) {
        // Use business timezone (IST) instead of server timezone
        LocalDateTime oneWeekAgo = DateUtils.getBusinessLocalDate().minusDays(7).atStartOfDay();

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
        // Use business timezone (IST) instead of server timezone
        LocalDate thirtyDaysAgo = DateUtils.getBusinessLocalDate().minusDays(30);
        LocalDateTime thirtyDaysAgoDateTime = thirtyDaysAgo.atStartOfDay();
        LocalDateTime now = LocalDateTime.now(DateUtils.getBusinessZone());

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

    @Override
    public ResponseEntity<?> getBranchFinancialOverview(Integer days) {
        // Placeholder for financial integration
        return baseResponse.successResponse("Branch financial overview - feature coming soon",
            Map.of("message", "Financial analytics will be available in future updates"));
    }
}