package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.SaasAdminDashboardDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.exception.BusinessException;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.SaasAdminDashboardService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * Implementation of SAAS admin dashboard service.
 * Provides comprehensive performance metrics and reporting for company-wide monitoring.
 */
@Service
public class SaasAdminDashboardServiceImpl implements SaasAdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(SaasAdminDashboardServiceImpl.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DashboardCommonService dashboardCommonService;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BaseResponse baseResponse;


    @Autowired
    private UserRepository userRepository;

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCompanyDashboardStatistics(Long branchId) {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

            SaasAdminDashboardDTO dashboardDTO = new SaasAdminDashboardDTO();

            // Set company info
            Map<String, Object> companyInfo = new HashMap<>();
            companyInfo.put("companyId", currentCompany.getId());
            companyInfo.put("companyName", currentCompany.getCompanyName());
            companyInfo.put("email", currentCompany.getEmail());
            companyInfo.put("phoneNumber", currentCompany.getPhoneNumber() != null ? currentCompany.getPhoneNumber() : currentCompany.getCompanyPhone());
            companyInfo.put("address", currentCompany.getAddress());
            companyInfo.put("gstinNumber", currentCompany.getGstinNumber());
            dashboardDTO.setCompanyInfo(companyInfo);

            // If branchId is provided, show branch-specific dashboard
            if (branchId != null) {
                // Validate that the branch belongs to the company
                Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new BusinessException("Branch not found"));

                if (!branch.getClinic().getId().equals(currentCompany.getId())) {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                            "Branch does not belong to your company");
                }

                // Set branch info
                Map<String, Object> branchInfo = new HashMap<>();
                branchInfo.put("branchId", branch.getId());
                branchInfo.put("branchName", branch.getBranchName());
                branchInfo.put("address", branch.getAddress());
                branchInfo.put("contactNumber", branch.getPhoneNumber());
                dashboardDTO.setBranchInfo(branchInfo);

                // Today's overview for branch
                dashboardDTO.setTodaysOverview(getTodaysBranchStats(branchId));

                // Weekly performance for branch
                dashboardDTO.setWeeklyPerformance(getWeeklyBranchStats(branchId));

                // Appointment status distribution for branch
                dashboardDTO.setAppointmentStatusDistribution(getBranchAppointmentStatusDistribution(branchId));

                // Staff metrics for branch
                dashboardDTO.setStaffMetrics(getBranchStaffMetrics(branchId));

                // Patient statistics for branch
                dashboardDTO.setPatientStats(getBranchPatientStatistics(branchId));

                dashboardDTO.setRevenueSummary(
                        dashboardCommonService.getRevenue(currentCompany.getId(), branchId));

                dashboardDTO.setBillPaymentTypeStats(
                        dashboardCommonService.getBillPaymentTypeStats(currentCompany.getId(), branchId));

                dashboardDTO.setLatestAppointments(
                        dashboardCommonService.getLatestAppointments(currentCompany.getId(), branchId));

                dashboardDTO.setEmployees(
                        dashboardCommonService.getEmployees(currentCompany.getId(), branchId));

                dashboardDTO.setDoctors(
                        dashboardCommonService.getDoctors(currentCompany.getId(), branchId));

                dashboardDTO.setLatestPatients(
                        dashboardCommonService.getLatestPatients(currentCompany.getId()));

                dashboardDTO.setStaffPerformanceRanking(
                        dashboardCommonService.getStaffPerformanceRanking(currentCompany.getId(), branchId));

                return baseResponse.successResponse("Branch dashboard statistics fetched successfully", dashboardDTO);
            } else {
                // Show company-wide dashboard
                // Today's overview
                dashboardDTO.setTodaysOverview(getTodaysCompanyStats(currentCompany.getId()));

                // Weekly performance
                dashboardDTO.setWeeklyPerformance(getWeeklyCompanyStats(currentCompany.getId()));

                // Appointment status distribution
                dashboardDTO.setAppointmentStatusDistribution(getCompanyAppointmentStatusDistribution(currentCompany.getId()));

                // Branch performance
                dashboardDTO.setBranchPerformance(getBranchPerformance(currentCompany.getId()));

                // Staff metrics
                dashboardDTO.setStaffMetrics(getCompanyStaffMetrics(currentCompany.getId()));

                dashboardDTO.setEmployees(
                        dashboardCommonService.getEmployees(currentCompany.getId(), null));

                dashboardDTO.setDoctors(
                        dashboardCommonService.getDoctors(currentCompany.getId(), null));

                dashboardDTO.setLatestPatients(
                        dashboardCommonService.getLatestPatients(currentCompany.getId()));

                dashboardDTO.setRevenueSummary(
                        dashboardCommonService.getRevenue(currentCompany.getId(), null));

                dashboardDTO.setStaffPerformanceRanking(
                        dashboardCommonService.getStaffPerformanceRanking(currentCompany.getId(), null));

                dashboardDTO.setBillPaymentTypeStats(
                        dashboardCommonService.getBillPaymentTypeStats(currentCompany.getId(), null));

                dashboardDTO.setLatestAppointments(
                        dashboardCommonService.getLatestAppointments(currentCompany.getId(), null));

                // Patient statistics
                dashboardDTO.setPatientStats(getCompanyPatientStatistics(currentCompany.getId()));

                return baseResponse.successResponse("Company dashboard statistics fetched successfully", dashboardDTO);
            }

        } catch (Exception e) {
            logger.error("Error fetching dashboard statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch dashboard statistics");
        }
    }

    @Override
    public ResponseEntity<?> getCompanyAppointmentStats(Integer days) {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

            int daysBack = days != null ? days : 30;
            LocalDate today = DateUtils.getBusinessLocalDate();
            LocalDate startDate = today.minusDays(daysBack);
            LocalDateTime startDateTime = DateUtils.getStartOfBusinessDay(startDate);
            LocalDateTime endDateTime = DateUtils.getEndOfBusinessDay(today);

            Map<String, Object> stats = new HashMap<>();

            // Appointment counts by status (based on appointment date)
            Map<String, Long> statusCounts = new HashMap<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByCompanyIdAndStatusAndAppointmentDateBetween(
                        currentCompany.getId(), status, startDateTime, endDateTime);
                statusCounts.put(status.name(), count);
            }
            stats.put("statusCounts", statusCounts);

            // Total appointments (based on appointment date)
            long totalAppointments = appointmentRepository.countByCompanyIdAndAppointmentDateBetween(
                    currentCompany.getId(), startDateTime, endDateTime);
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

            // No-show rate
            long noShow = statusCounts.getOrDefault("NO_SHOW", 0L);
            double noShowRate = totalAppointments > 0 ?
                    (double) noShow / totalAppointments * 100 : 0.0;
            stats.put("noShowRate", Math.round(noShowRate * 100.0) / 100.0);

            // Also include creation date statistics
            Map<String, Object> creationStats = new HashMap<>();

            // Appointment counts by status (based on creation date)
            Map<String, Long> creationStatusCounts = new HashMap<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                        currentCompany.getId(), status, startDateTime, endDateTime);
                creationStatusCounts.put(status.name(), count);
            }
            creationStats.put("statusCounts", creationStatusCounts);

            // Total appointments created
            long totalCreated = appointmentRepository.countByCompanyIdAndCreatedAtBetween(
                    currentCompany.getId(), startDateTime, endDateTime);
            creationStats.put("totalCreated", totalCreated);

            stats.put("creationStats", creationStats);

            return baseResponse.successResponse("Company appointment stats fetched successfully", stats);

        } catch (Exception e) {
            logger.error("Error fetching company appointment stats", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch appointment stats");
        }
    }

    @Override
    public ResponseEntity<?> getCompanyStaffPerformance() {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

            // Get all branches in company
            List<Branch> branches = branchRepository.findByClinicId(currentCompany.getId());

            Map<String, Object> staffPerformance = new HashMap<>();

            for (Branch branch : branches) {
                // Get all doctors in branch
                List<Doctor> doctors = doctorRepository.findByBranchId(branch.getId());

                Map<String, Object> branchStaff = new HashMap<>();

                for (Doctor doctor : doctors) {
                    Map<String, Object> doctorPerformance = new HashMap<>();
                    doctorPerformance.put("doctorId", doctor.getId());
                    doctorPerformance.put("doctorName", doctor.getDoctorName());
                    doctorPerformance.put("specialization", doctor.getSpecialization());
                    doctorPerformance.put("department", doctor.getDepartment());
                    doctorPerformance.put("branchName", branch.getBranchName());

                    // Get doctor's performance metrics
                    doctorPerformance.put("performanceMetrics", getDoctorPerformanceMetrics(doctor.getId()));

                    branchStaff.put(doctor.getDoctorName(), doctorPerformance);
                }

                staffPerformance.put(branch.getBranchName(), branchStaff);
            }

            return baseResponse.successResponse("Company staff performance fetched successfully", staffPerformance);

        } catch (Exception e) {
            logger.error("Error fetching company staff performance", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch staff performance");
        }
    }

    @Override
    public ResponseEntity<?> getCompanyFinancialOverview(Integer days) {
        // Placeholder for financial integration
        return baseResponse.successResponse("Company financial overview - feature coming soon",
                Map.of("message", "Financial analytics will be available in future updates"));
    }

    @Override
    public ResponseEntity<?> getCompanyPatientStats() {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

            return baseResponse.successResponse("Company patient stats fetched successfully",
                    getCompanyPatientStatistics(currentCompany.getId()));

        } catch (Exception e) {
            logger.error("Error fetching company patient stats", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch patient stats");
        }
    }

    @Override
    public ResponseEntity<?> getBranchAnalytics() {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

            Map<String, Object> branchAnalytics = new HashMap<>();

            // Get all branches in company
            List<Branch> branches = branchRepository.findByClinicId(currentCompany.getId());

            Map<String, Object> branchStats = new HashMap<>();
            for (Branch branch : branches) {
                Map<String, Object> branchData = new HashMap<>();

                // Get branch appointments
                long branchAppointments = appointmentRepository.countByBranchId(branch.getId());
                branchData.put("totalAppointments", branchAppointments);

                // Get completed appointments
                long completedAppointments = appointmentRepository.countByBranchIdAndStatus(
                        branch.getId(), AppointmentStatus.COMPLETED);
                branchData.put("completedAppointments", completedAppointments);

                // Get doctors count
                long doctorCount = doctorRepository.countByBranchId(branch.getId());
                branchData.put("doctorCount", doctorCount);

                // Get patient count
                long patientCount = customerRepository.countByBranchId(branch.getId());
                branchData.put("patientCount", patientCount);

                // Completion rate
                double completionRate = branchAppointments > 0 ?
                        (double) completedAppointments / branchAppointments * 100 : 0.0;
                branchData.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

                branchStats.put(branch.getBranchName(), branchData);
            }

            branchAnalytics.put("branches", branchStats);
            branchAnalytics.put("totalBranches", branches.size());

            return baseResponse.successResponse("Branch analytics fetched successfully", branchAnalytics);

        } catch (Exception e) {
            logger.error("Error fetching branch analytics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch branch analytics");
        }
    }

    @Override
    public ResponseEntity<?> getCompanyResourceUtilization(Integer days) {
        // Placeholder for resource utilization integration
        return baseResponse.successResponse("Company resource utilization - feature coming soon",
                Map.of("message", "Resource utilization analytics will be available in future updates"));
    }

    @Override
    public ResponseEntity<?> getComparativeCompanyAnalytics(Long[] companyIds) {
        // This would be used by super admins to compare companies
        return baseResponse.successResponse("Comparative company analytics - feature coming soon",
                Map.of("message", "Comparative analytics will be available in future updates"));
    }

    @Override
    public ResponseEntity<?> getSystemHealthMetrics() {
        try {
            Map<String, Object> healthMetrics = new HashMap<>();

            // User activity metrics
            healthMetrics.put("activeUsers", "TBD - requires user activity tracking");
            healthMetrics.put("userLoginsToday", "TBD - requires login tracking");

            // System performance indicators
            healthMetrics.put("apiResponseTime", "TBD - requires monitoring integration");
            healthMetrics.put("databasePerformance", "TBD - requires DB monitoring");

            // Operational metrics
            healthMetrics.put("systemUptime", "TBD - requires uptime monitoring");
            healthMetrics.put("errorRate", "TBD - requires error tracking");

            return baseResponse.successResponse("System health metrics fetched successfully", healthMetrics);

        } catch (Exception e) {
            logger.error("Error fetching system health metrics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch health metrics");
        }
    }

    /**
     * Gets the current user with the company eagerly loaded to avoid LazyInitializationException
     */
    private User getCurrentUserWithCompany() {
        User currentUser = DbUtill.getCurrentUser();

        // Fetch the user again with the company eagerly loaded
        Optional<User> userWithCompany = userRepository.findByIdWithCompany(currentUser.getId());
        if (userWithCompany.isPresent()) {
            return userWithCompany.get();
        } else {
            throw new IllegalStateException("Current user not found in database");
        }
    }

    // Private helper methods
    private Map<String, Object> getTodaysCompanyStats(Long companyId) {
        // Use business timezone to determine "today" properly
        LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
        LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();

        Map<String, Object> stats = new HashMap<>();

        // Count appointments created today (based on createdAt field)
        long totalToday = appointmentRepository.countByCompanyIdAndCreatedAtBetween(
                companyId, startOfDay, endOfDay);
        stats.put("totalAppointments", totalToday);

        long confirmed = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                companyId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
        stats.put("confirmed", confirmed);

        long completed = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                companyId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
        stats.put("completed", completed);

        long pending = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                companyId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
        stats.put("pending", pending);

        // Also count cancelled and rescheduled appointments created today
        long cancelled = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                companyId, AppointmentStatus.CANCELLED, startOfDay, endOfDay);
        stats.put("cancelled", cancelled);

        long rescheduled = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                companyId, AppointmentStatus.RESCHEDULED, startOfDay, endOfDay);
        stats.put("rescheduled", rescheduled);

        return stats;
    }

    private Map<String, Object> getTodaysBranchStats(Long branchId) {
        // Use business timezone to determine "today" properly
        LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
        LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();

        Map<String, Object> stats = new HashMap<>();

        // Count appointments created today for this branch
        long totalToday = appointmentRepository.countByBranchIdAndCreatedAtBetween(
                branchId, startOfDay, endOfDay);
        stats.put("totalAppointments", totalToday);

        long confirmed = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
        stats.put("confirmed", confirmed);

        long completed = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
        stats.put("completed", completed);

        long pending = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
        stats.put("pending", pending);

        // Also count cancelled and rescheduled appointments created today
        long cancelled = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, AppointmentStatus.CANCELLED, startOfDay, endOfDay);
        stats.put("cancelled", cancelled);

        long rescheduled = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, AppointmentStatus.RESCHEDULED, startOfDay, endOfDay);
        stats.put("rescheduled", rescheduled);

        return stats;
    }

    private Map<String, Object> getWeeklyCompanyStats(Long companyId) {
        // Use business timezone to determine dates properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime weekStartDateTime = DateUtils.getStartOfBusinessDay(weekStart);
        LocalDateTime weekEndDateTime = DateUtils.getEndOfBusinessDay(today);

        Map<String, Object> weeklyStats = new HashMap<>();

        // Appointment date based statistics
        long weeklyTotal = appointmentRepository.countByCompanyIdAndAppointmentDateBetween(
                companyId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalAppointments", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByCompanyIdAndStatusAndAppointmentDateBetween(
                companyId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
                (double) weeklyCompleted / weeklyTotal * 100 : 0.0;
        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // Creation date based statistics
        long weeklyCreatedTotal = appointmentRepository.countByCompanyIdAndCreatedAtBetween(
                companyId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalCreated", weeklyCreatedTotal);

        long weeklyCreatedCompleted = appointmentRepository.countByCompanyIdAndStatusAndCreatedAtBetween(
                companyId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("createdCompleted", weeklyCreatedCompleted);

        double createdCompletionRate = weeklyCreatedTotal > 0 ?
                (double) weeklyCreatedCompleted / weeklyCreatedTotal * 100 : 0.0;
        weeklyStats.put("createdCompletionRate", Math.round(createdCompletionRate * 100.0) / 100.0);

        return weeklyStats;
    }

    private Map<String, Object> getWeeklyBranchStats(Long branchId) {
        // Use business timezone to determine dates properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime weekStartDateTime = DateUtils.getStartOfBusinessDay(weekStart);
        LocalDateTime weekEndDateTime = DateUtils.getEndOfBusinessDay(today);

        Map<String, Object> weeklyStats = new HashMap<>();

        // Appointment date based statistics for branch
        long weeklyTotal = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
                branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalAppointments", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
                branchId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
                (double) weeklyCompleted / weeklyTotal * 100 : 0.0;
        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // Creation date based statistics for branch
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

    private Map<String, Long> getCompanyAppointmentStatusDistribution(Long companyId) {
        Map<String, Long> statusDistribution = new HashMap<>();

        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByCompanyIdAndStatus(companyId, status);
            statusDistribution.put(status.name(), count);
        }

        return statusDistribution;
    }

//    private Map<String, Long> getBranchAppointmentStatusDistribution(Long branchId) {
//        Map<String, Long> statusDistribution = new HashMap<>();
//
//        for (AppointmentStatus status : AppointmentStatus.values()) {
//            long count = appointmentRepository.countByBranchIdAndStatus(branchId, status);
//            statusDistribution.put(status.name(), count);
//        }
//
//        return statusDistribution;
//    }

    private Map<String, Long> getBranchAppointmentStatusDistribution(Long branchId) {

        Map<String, Long> statusDistribution = new HashMap<>();

        Long[] range = DateUtils.getMonthlyRangeInMilli(); // current month start & end

        for (AppointmentStatus status : AppointmentStatus.values()) {

            long count = appointmentRepository.countByBranchIdAndStatusAndMonth(
                    branchId,
                    status,
                    range[0],
                    range[1]
            );

            statusDistribution.put(status.name(), count);
        }

        return statusDistribution;
    }

    private Map<String, Object> getBranchPerformance(Long companyId) {
        Map<String, Object> branchPerformance = new HashMap<>();
        Long[] monthRange = DateUtils.getMonthlyRangeInMilli();
        List<Branch> branches = branchRepository.findByClinicId(companyId);

        for (Branch branch : branches) {
            Map<String, Object> branchStats = new HashMap<>();

            // Get appointments for this branch
            long branchAppointments = appointmentRepository.countByBranchIdAndCreatedAtBetween(
                    branch.getId(),
                    monthRange[0],
                    monthRange[1]);
            branchStats.put("totalAppointments", branchAppointments);

            // Get completed appointments
            long completedAppointments = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                    branch.getId(),
                    AppointmentStatus.COMPLETED,
                    monthRange[0],
                    monthRange[1]);
            branchStats.put("completedAppointments", completedAppointments);

            // Get doctors count
            long doctorCount = doctorRepository.countByBranchId(
                    branch.getId()
            );
            branchStats.put("doctorCount", doctorCount);

            // Completion rate
            double completionRate = branchAppointments > 0 ?
                    (double) completedAppointments / branchAppointments * 100 : 0.0;
            branchStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

            branchPerformance.put(branch.getBranchName(), branchStats);
        }

        return branchPerformance;
    }

    private Map<String, Object> getCompanyStaffMetrics(Long companyId) {
        Map<String, Object> staffMetrics = new HashMap<>();

        List<Branch> branches = branchRepository.findByClinicId(companyId);

        long totalDoctors = 0;
        long activeDoctors = 0;
        long totalStaff = 0;
        long activeStaff = 0;

        // Define staff roles (excluding admin roles)
        List<UserRole> staffRoles = Arrays.asList(
                UserRole.RECEPTIONIST,
                UserRole.BILLING_STAFF,
                UserRole.BRANCH_MANAGER
        );

        for (Branch branch : branches) {
            totalDoctors += doctorRepository.countByBranchId(branch.getId());
            activeDoctors += doctorRepository.countByBranchIdAndIsActive(branch.getId(), true);
            totalStaff += userRepository.countStaffByBranchIdAndRoles(branch.getId(), staffRoles);
            activeStaff += userRepository.countActiveStaffByBranchIdAndRoles(branch.getId(), staffRoles);
        }

        staffMetrics.put("totalDoctors", totalDoctors);
        staffMetrics.put("activeDoctors", activeDoctors);
        staffMetrics.put("totalBranches", branches.size());
        staffMetrics.put("totalStaff", totalStaff);
        staffMetrics.put("activeStaff", activeStaff);

        return staffMetrics;
    }

    private Map<String, Object> getBranchStaffMetrics(Long branchId) {
        Map<String, Object> staffMetrics = new HashMap<>();

        // Define staff roles (excluding admin roles)
        List<UserRole> staffRoles = Arrays.asList(
                UserRole.DOCTOR,
                UserRole.RECEPTIONIST,
                UserRole.BILLING_STAFF,
                UserRole.BRANCH_MANAGER
        );

        long totalDoctors = doctorRepository.countByBranchId(branchId);
        long activeDoctors = doctorRepository.countByBranchIdAndIsActive(branchId, true);
        long totalStaff = userRepository.countStaffByBranchIdAndRoles(branchId, staffRoles);
        long activeStaff = userRepository.countActiveStaffByBranchIdAndRoles(branchId, staffRoles);

        staffMetrics.put("totalDoctors", totalDoctors);
        staffMetrics.put("activeDoctors", activeDoctors);
        staffMetrics.put("totalStaff", totalStaff);
        staffMetrics.put("activeStaff", activeStaff);

        return staffMetrics;
    }

    private Map<String, Object> getCompanyPatientStatistics(Long companyId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Get all branches in company
        List<Branch> branches = branchRepository.findByClinicId(companyId);

        long totalPatients = 0;
        long newPatientsThisMonth = 0;
        long activePatients = 0;

        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = DateUtils.getStartOfBusinessDay(monthStart);
        LocalDate threeMonthsAgo = today.minusMonths(3);
        LocalDateTime threeMonthsAgoDateTime = DateUtils.getStartOfBusinessDay(threeMonthsAgo);

        for (Branch branch : branches) {
            totalPatients += customerRepository.countByBranchId(branch.getId());
            newPatientsThisMonth += customerRepository.countByBranchIdAndCreatedAtAfter(
                    branch.getId(), monthStartDateTime);
            activePatients += customerRepository.countActivePatientsByBranch(
                    branch.getId(), threeMonthsAgoDateTime);
        }

        patientStats.put("totalPatients", totalPatients);
        patientStats.put("newPatientsThisMonth", newPatientsThisMonth);
        patientStats.put("activePatients", activePatients);

        return patientStats;
    }

    private Map<String, Object> getBranchPatientStatistics(Long branchId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = DateUtils.getStartOfBusinessDay(monthStart);
        LocalDate threeMonthsAgo = today.minusMonths(3);
        LocalDateTime threeMonthsAgoDateTime = DateUtils.getStartOfBusinessDay(threeMonthsAgo);

        long totalPatients = customerRepository.countByBranchId(branchId);
        long newPatientsThisMonth = customerRepository.countByBranchIdAndCreatedAtAfter(
                branchId, monthStartDateTime);
        long activePatients = customerRepository.countActivePatientsByBranch(
                branchId, threeMonthsAgoDateTime);

        patientStats.put("totalPatients", totalPatients);
        patientStats.put("newPatientsThisMonth", newPatientsThisMonth);
        patientStats.put("activePatients", activePatients);

        return patientStats;
    }

    private Map<String, Object> getDoctorPerformanceMetrics(Long doctorId) {
        Map<String, Object> metrics = new HashMap<>();

        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate thirtyDaysAgo = today.minusDays(30);

        // Get last 30 days data
        LocalDateTime thirtyDaysAgoDateTime = DateUtils.getStartOfBusinessDay(thirtyDaysAgo);
        LocalDateTime now = DateUtils.getEndOfBusinessDay(today);

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
