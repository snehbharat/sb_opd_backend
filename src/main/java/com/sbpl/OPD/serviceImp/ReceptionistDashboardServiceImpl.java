package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.ReceptionistDashboardDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.ReceptionistDashboardService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of receptionist dashboard service.
 * Provides comprehensive reporting and KPIs for receptionist workflow.
 */
@Service
public class ReceptionistDashboardServiceImpl implements ReceptionistDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(ReceptionistDashboardServiceImpl.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private DashboardCommonService dashboardCommonService;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private UserRepository userRepository;

    @Override
    public ResponseEntity<?> getDashboardStatistics() {
        try {
            logger.info("Fetching receptionist dashboard statistics");

            User currentUser = getCurrentUserWithBranch();
            Branch currentBranch = currentUser.getBranch();
            Long companyId = DbUtill.getLoggedInCompanyId();

            if (currentBranch == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                    "User not assigned to any branch");
            }

            ReceptionistDashboardDTO dashboardDTO = new ReceptionistDashboardDTO();

            // Set branch info
            Map<String, Object> branchInfo = new HashMap<>();
            branchInfo.put("branchId", currentBranch.getId());
            branchInfo.put("branchName", currentBranch.getBranchName());
            branchInfo.put("address", currentBranch.getAddress());
            branchInfo.put("phoneNumber", currentBranch.getPhoneNumber());
            branchInfo.put("email", currentBranch.getEmail());
            dashboardDTO.setBranchInfo(branchInfo);

            // Today's appointments summary
            dashboardDTO.setTodaysAppointments(getTodaysAppointmentsStats(currentBranch.getId()));

            // Weekly appointments overview
            dashboardDTO.setWeeklyAppointments(getWeeklyAppointmentsStats(currentBranch.getId()));
            dashboardDTO.setWeeklyPerformance(
                    dashboardCommonService.getWeeklyBranchStats(currentBranch.getId())
            );
            dashboardDTO.setStaffMetrics(
                    dashboardCommonService.getCompanyStaffMetrics(currentBranch.getClinic().getId())
            );
            dashboardDTO.setBranchPerformance(
                    dashboardCommonService.getBranchPerformance(currentBranch.getClinic().getId(),currentBranch.getId())
            );

            // Appointment status distribution
            dashboardDTO.setAppointmentStatusDistribution(getAppointmentStatusDistribution(currentBranch.getId()));

            // Patient statistics
            dashboardDTO.setPatientStats(getPatientStatistics(currentBranch.getId()));

            // Upcoming appointments
//            dashboardDTO.setUpcomingAppointments(getUpcomingAppointments(currentBranch.getId()));

            // Recent activities
//            dashboardDTO.setRecentActivities(getRecentActivities(currentBranch.getId()));

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

            dashboardDTO.setRevenueSummary(
                    dashboardCommonService.getRevenue(companyId, currentBranch.getId())
            );

            dashboardDTO.setBillPaymentTypeStats(
                    dashboardCommonService.getBillPaymentTypeStats(companyId, currentBranch.getId())
            );

            dashboardDTO.setLatestAppointments(
                    dashboardCommonService.getLatestAppointments(companyId, currentBranch.getId())
            );

            return baseResponse.successResponse(
                    "Receptionist dashboard statistics fetched successfully",
                    dashboardDTO
            );

        } catch (Exception e) {
            logger.error("Error fetching receptionist dashboard statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to fetch dashboard statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getTodaysStatistics() {
        try {
            User currentUser = getCurrentUserWithBranch();
            Branch currentBranch = currentUser.getBranch();

            if (currentBranch == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                    "User not assigned to any branch");
            }

            Map<String, Object> todaysStats = getTodaysAppointmentsStats(currentBranch.getId());

            return baseResponse.successResponse(
                    "Today's statistics fetched successfully",
                    todaysStats
            );

        } catch (Exception e) {
            logger.error("Error fetching today's statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to fetch today's statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyOverview() {
        try {
            User currentUser = getCurrentUserWithBranch();
            Branch currentBranch = currentUser.getBranch();

            if (currentBranch == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                    "User not assigned to any branch");
            }

            Map<String, Object> weeklyStats = getWeeklyAppointmentsStats(currentBranch.getId());

            return baseResponse.successResponse(
                    "Weekly overview fetched successfully",
                    weeklyStats
            );

        } catch (Exception e) {
            logger.error("Error fetching weekly overview", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to fetch weekly overview: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAppointmentsStatistics() {
        try {
            User currentUser = getCurrentUserWithBranch();
            Branch currentBranch = currentUser.getBranch();

            if (currentBranch == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                    "User not assigned to any branch");
            }

            Map<String, Object> appointmentStats = new HashMap<>();

            // Get appointment counts by status
            Map<String, Long> statusCounts = new HashMap<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByBranchIdAndStatus(currentBranch.getId(), status);
                statusCounts.put(status.name(), count);
            }
            appointmentStats.put("statusCounts", statusCounts);

            // Total appointments
            long totalAppointments = appointmentRepository.countByBranchId(currentBranch.getId());
            appointmentStats.put("totalAppointments", totalAppointments);

            // Appointments for last 30 days
            // Use business timezone (IST) instead of server timezone
            LocalDate thirtyDaysAgo = DateUtils.getBusinessLocalDate().minusDays(30);
            LocalDateTime thirtyDaysAgoDateTime = thirtyDaysAgo.atStartOfDay();
            long appointmentsLast30Days = appointmentRepository.countByBranchIdAndCreatedAtAfter(
                currentBranch.getId(), thirtyDaysAgoDateTime);
            appointmentStats.put("appointmentsLast30Days", appointmentsLast30Days);

            return baseResponse.successResponse(
                    "Appointment statistics fetched successfully",
                    appointmentStats
            );

        } catch (Exception e) {
            logger.error("Error fetching appointment statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to fetch appointment statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientStatistics() {
        try {
            User currentUser = getCurrentUserWithBranch();
            Branch currentBranch = currentUser.getBranch();

            if (currentBranch == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                    "User not assigned to any branch");
            }

            Map<String, Object> patientStats = new HashMap<>();

            // Total patients in branch
            long totalPatients = customerRepository.countByBranchId(currentBranch.getId());
            patientStats.put("totalPatients", totalPatients);

            // New patients this month
            // Use business timezone (IST) instead of server timezone
            LocalDate monthStart = DateUtils.getBusinessLocalDate().withDayOfMonth(1);
            LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
            long newPatients = customerRepository.countByBranchIdAndCreatedAtAfter(
                currentBranch.getId(), monthStartDateTime);
            patientStats.put("newPatientsThisMonth", newPatients);

            // Active patients (with recent appointments)
            // Use business timezone (IST) instead of server timezone
            LocalDate threeMonthsAgo = DateUtils.getBusinessLocalDate().minusMonths(3);
            LocalDateTime threeMonthsAgoDateTime = threeMonthsAgo.atStartOfDay();
            long activePatients = customerRepository.countActivePatientsByBranch(
                currentBranch.getId(), threeMonthsAgoDateTime);
            patientStats.put("activePatients", activePatients);

            // Patients with appointments in last 30 days
            // Use business timezone (IST) instead of server timezone
            long patientsWithRecentAppointments = appointmentRepository.countDistinctPatientsByBranchIdAndDateRange(
                currentBranch.getId(),
                DateUtils.getBusinessLocalDate().minusDays(30).atStartOfDay(),
                LocalDateTime.now(DateUtils.getBusinessZone())
            );
            patientStats.put("patientsWithRecentAppointments", patientsWithRecentAppointments);

            return baseResponse.successResponse(
                    "Patient statistics fetched successfully",
                    patientStats
            );

        } catch (Exception e) {
            logger.error("Error fetching patient statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to fetch patient statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecentActivities() {
        try {
            User currentUser = getCurrentUserWithBranch();
            Branch currentBranch = currentUser.getBranch();

            if (currentBranch == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, 
                    "User not assigned to any branch");
            }

            List<Map<String, Object>> recentActivities = getRecentActivities(currentBranch.getId());

            return baseResponse.successResponse(
                    "Recent activities fetched successfully",
                    recentActivities
            );

        } catch (Exception e) {
            logger.error("Error fetching recent activities", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Failed to fetch recent activities: " + e.getMessage());
        }
    }

    // Private helper methods
    private Map<String, Object> getTodaysAppointmentsStats(Long branchId) {
        // Use business timezone to determine "today" properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
        LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();

        Map<String, Object> stats = new HashMap<>();

        // Count appointments created today (based on createdAt field)
        long totalToday = appointmentRepository.countByBranchIdAndCreatedAtBetween(
            branchId, startOfDay, endOfDay);
        stats.put("total", totalToday);

        // Confirmed appointments created today
        long confirmed = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
        stats.put("confirmed", confirmed);

        // Completed appointments created today
        long completed = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
        stats.put("completed", completed);

        // Cancelled appointments created today
        long cancelled = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.CANCELLED, startOfDay, endOfDay);
        stats.put("cancelled", cancelled);

        // Pending appointments created today
        long pending = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
            branchId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
        stats.put("pending", pending);

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

    private Map<String, Object> getWeeklyAppointmentsStats(Long branchId) {
        // Use business timezone to determine dates properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime weekStartDateTime = DateUtils.getStartOfBusinessDay(weekStart);
        LocalDateTime weekEndDateTime = DateUtils.getEndOfBusinessDay(today);

        Map<String, Object> weeklyStats = new HashMap<>();

        // Appointment date based statistics
        long weeklyTotal = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
            branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("total", weeklyTotal);

        // Creation date based statistics
        long weeklyCreatedTotal = appointmentRepository.countByBranchIdAndCreatedAtBetween(
            branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalCreated", weeklyCreatedTotal);

        // Daily breakdown (appointment date based)
        Map<String, Long> dailyBreakdown = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = DateUtils.getStartOfBusinessDay(date);
            LocalDateTime dayEnd = DateUtils.getEndOfBusinessDay(date);

            long dayCount = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
                branchId, dayStart, dayEnd);
            dailyBreakdown.put(date.toString(), dayCount);
        }
        weeklyStats.put("dailyBreakdown", dailyBreakdown);

        // Daily breakdown (creation date based)
        Map<String, Long> dailyCreationBreakdown = new HashMap<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = today.minusDays(i);
            LocalDateTime dayStart = DateUtils.getStartOfBusinessDay(date);
            LocalDateTime dayEnd = DateUtils.getEndOfBusinessDay(date);

            long dayCount = appointmentRepository.countByBranchIdAndCreatedAtBetween(
                branchId, dayStart, dayEnd);
            dailyCreationBreakdown.put(date.toString(), dayCount);
        }
//        weeklyStats.put("dailyCreationBreakdown", dailyCreationBreakdown);

        return weeklyStats;
    }

    private Map<String, Long> getAppointmentStatusDistribution(Long branchId) {
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

    private Map<String, Object> getPatientStatistics(Long branchId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();

        // Total patients in branch
        long totalPatients = customerRepository.countByBranchId(branchId);
        patientStats.put("total", totalPatients);

        // New patients this month
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = DateUtils.getStartOfBusinessDay(monthStart);
        long newPatients = customerRepository.countByBranchIdAndCreatedAtAfter(branchId, monthStartDateTime);
        patientStats.put("newThisMonth", newPatients);

        // Active patients (with recent appointments)
        LocalDate threeMonthsAgo = today.minusMonths(3);
        LocalDateTime threeMonthsAgoDateTime = DateUtils.getStartOfBusinessDay(threeMonthsAgo);
        long activePatients = customerRepository.countActivePatientsByBranch(branchId, threeMonthsAgoDateTime);
        patientStats.put("active", activePatients);

        return patientStats;
    }

    private List<Object> getUpcomingAppointments(Long branchId) {
        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate nextWeek = today.plusDays(7);
        LocalDateTime startDateTime = DateUtils.getStartOfBusinessDay(today);
        LocalDateTime endDateTime = DateUtils.getEndOfBusinessDay(nextWeek);

        PageRequest pageRequest = PageRequest.of(0, 10); // Limit to 10 upcoming appointments

        List<Appointment> upcomingAppointments = appointmentRepository
            .findByBranchIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
                branchId, startDateTime, endDateTime, AppointmentStatus.CONFIRMED, pageRequest)
            .getContent();

        return upcomingAppointments.stream()
            .map(appointment -> {
                Map<String, Object> appointmentMap = new HashMap<>();
                appointmentMap.put("id", appointment.getId());
                appointmentMap.put("patientName", appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName());
                appointmentMap.put("doctorName", appointment.getDoctor().getDoctorName());
                appointmentMap.put("appointmentDate", appointment.getAppointmentDate());
                appointmentMap.put("scheduledTime", appointment.getScheduledTime());
                appointmentMap.put("status", appointment.getStatus());
                return appointmentMap;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentActivities(Long branchId) {
        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate oneWeekAgoDate = today.minusDays(7);
        LocalDateTime oneWeekAgo = DateUtils.getStartOfBusinessDay(oneWeekAgoDate);

        List<Appointment> recentAppointments = appointmentRepository
            .findByBranchIdAndUpdatedAtAfterOrderByUpdatedAtDesc(branchId, oneWeekAgo, PageRequest.of(0, 15))
            .getContent();

        return recentAppointments.stream()
            .map(appointment -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", appointment.getId());
                activity.put("type", "APPOINTMENT_" + appointment.getStatus());
                activity.put("patientName", appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName());
                activity.put("doctorName", appointment.getDoctor().getDoctorName());
                activity.put("appointmentDate", appointment.getAppointmentDate());
                activity.put("status", appointment.getStatus());
                activity.put("updatedAt", appointment.getUpdatedAt());
                return activity;
            })
            .collect(Collectors.toList());
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
}