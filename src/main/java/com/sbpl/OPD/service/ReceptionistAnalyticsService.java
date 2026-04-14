package com.sbpl.OPD.service;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.dto.AppointmentDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service for receptionist analytics and dashboard metrics.
 * Provides comprehensive reporting and KPIs for receptionist workflow.
 */
@Service
public class ReceptionistAnalyticsService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BaseResponse baseResponse;

    /**
     * Get comprehensive dashboard statistics for receptionist
     */
    public ResponseEntity<?> getDashboardStatistics() {
        User currentUser = DbUtill.getCurrentUser();
        Branch currentBranch = currentUser.getBranch();

        if (currentBranch == null) {
            return baseResponse.errorResponse(org.springframework.http.HttpStatus.BAD_REQUEST, 
                "User not assigned to any branch");
        }

        Map<String, Object> dashboardData = new HashMap<>();

        // Today's appointments summary
        dashboardData.put("todaysAppointments", getTodaysAppointmentsStats(currentBranch.getId()));

        // Weekly appointments overview
        dashboardData.put("weeklyAppointments", getWeeklyAppointmentsStats(currentBranch.getId()));

        // Appointment status distribution
        dashboardData.put("appointmentStatusDistribution", getAppointmentStatusDistribution(currentBranch.getId()));

        // Patient statistics
        dashboardData.put("patientStats", getPatientStatistics(currentBranch.getId()));

        // Upcoming appointments
        dashboardData.put("upcomingAppointments", getUpcomingAppointments(currentBranch.getId()));

        // Recent activities
        dashboardData.put("recentActivities", getRecentActivities(currentBranch.getId()));

        return baseResponse.successResponse(
                "Receptionist dashboard data fetched successfully",
                dashboardData
        );
    }

    /**
     * Get today's appointment statistics
     */
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

        return stats;
    }

    /**
     * Get weekly appointments overview
     */
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
        weeklyStats.put("dailyCreationBreakdown", dailyCreationBreakdown);

        return weeklyStats;
    }

    /**
     * Get appointment status distribution
     */
    private Map<String, Long> getAppointmentStatusDistribution(Long branchId) {
        Map<String, Long> statusDistribution = new HashMap<>();

        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByBranchIdAndStatus(branchId, status);
            statusDistribution.put(status.name(), count);
        }

        return statusDistribution;
    }

    /**
     * Get patient/customer statistics
     */
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

    /**
     * Get upcoming appointments for next 7 days
     */
    private List<AppointmentDTO> getUpcomingAppointments(Long branchId) {
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
            .map(this::convertToDTO)
            .toList();
    }

    /**
     * Get recent activities for receptionist
     */
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
            .toList();
    }

    /**
     * Convert Appointment entity to DTO
     */
    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setDoctorId(appointment.getDoctor().getId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setScheduledTime(appointment.getScheduledTime());
        dto.setStatus(appointment.getStatus());
        dto.setReason(appointment.getReason());
        dto.setNotes(appointment.getNotes());
        dto.setPatientName(appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName());
        dto.setDoctorName(appointment.getDoctor().getDoctorName());
        
        // Set company and branch information
        if (appointment.getCompany() != null) {
            dto.setCompanyId(appointment.getCompany().getId());
        }
        
        // Set branch from appointment's actual branch (patient's branch used during creation)
        if (appointment.getBranch() != null) {
            dto.setBranchId(appointment.getBranch().getId());
        } else if (appointment.getPatient() != null && appointment.getPatient().getBranch() != null) {
            // Fallback to patient's branch
            dto.setBranchId(appointment.getPatient().getBranch().getId());
        } else if (appointment.getDoctor() != null && appointment.getDoctor().getBranch() != null) {
            // Last fallback to doctor's branch
            dto.setBranchId(appointment.getDoctor().getBranch().getId());
        }
        
        return dto;
    }
//
    /**
     * Get appointment trends for reporting
     */
    public ResponseEntity<?> getAppointmentTrends(LocalDate startDate, LocalDate endDate) {
        User currentUser = DbUtill.getCurrentUser();
        Branch currentBranch = currentUser.getBranch();

        if (currentBranch == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "User not assigned to any branch"));
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

        Map<String, Object> trends = new HashMap<>();

        // Daily appointment counts
        Map<String, Long> dailyCounts = new HashMap<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            LocalDateTime dayStart = DateUtils.getStartOfBusinessDay(currentDate);
            LocalDateTime dayEnd = DateUtils.getEndOfBusinessDay(currentDate);

            long count = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
                currentBranch.getId(), dayStart, dayEnd);
            dailyCounts.put(currentDate.toString(), count);

            currentDate = currentDate.plusDays(1);
        }
        trends.put("dailyCounts", dailyCounts);

        // Daily creation counts
        Map<String, Long> dailyCreationCounts = new HashMap<>();
        LocalDate creationDate = startDate;
        while (!creationDate.isAfter(endDate)) {
            LocalDateTime dayStart = DateUtils.getStartOfBusinessDay(creationDate);
            LocalDateTime dayEnd = DateUtils.getEndOfBusinessDay(creationDate);

            long count = appointmentRepository.countByBranchIdAndCreatedAtBetween(
                currentBranch.getId(), dayStart, dayEnd);
            dailyCreationCounts.put(creationDate.toString(), count);

            creationDate = creationDate.plusDays(1);
        }
        trends.put("dailyCreationCounts", dailyCreationCounts);

        // Status breakdown for period
        Map<String, Long> statusBreakdown = new HashMap<>();
        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
                currentBranch.getId(), status, startDateTime, endDateTime);
            statusBreakdown.put(status.name(), count);
        }
        trends.put("statusBreakdown", statusBreakdown);

        return baseResponse.successResponse(
                "Appointment trends fetched successfully",
                trends
        );
    }

    /**
     * Get receptionist performance metrics
     */
    public ResponseEntity<?> getReceptionistPerformance() {
        User currentUser = DbUtill.getCurrentUser();
        Branch currentBranch = currentUser.getBranch();

        if (currentBranch == null) {
            return baseResponse.errorResponse(org.springframework.http.HttpStatus.BAD_REQUEST, 
                "User not assigned to any branch");
        }

        Map<String, Object> performance = new HashMap<>();

        // Appointments created by current receptionist
        // Note: This requires createdBy field in Appointment entity
        long appointmentsCreated = 0; // Placeholder - implement when createdBy field exists
        performance.put("appointmentsCreated", appointmentsCreated);

        // Appointments confirmed by receptionist
        // Note: This requires confirmedBy field in Appointment entity
        long appointmentsConfirmed = 0; // Placeholder - implement when confirmedBy field exists
        performance.put("appointmentsConfirmed", appointmentsConfirmed);

        // Average daily appointments handled
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = DateUtils.getStartOfBusinessDay(monthStart);
        long monthlyAppointments = appointmentRepository.countByBranchIdAndCreatedAtAfter(
            currentBranch.getId(), monthStartDateTime);
        int daysInMonth = today.lengthOfMonth();
        double avgDaily = (double) monthlyAppointments / daysInMonth;
        performance.put("avgDailyAppointments", Math.round(avgDaily * 100.0) / 100.0);

        // Completion rate
        LocalDate thirtyDaysAgoDate = today.minusDays(30);
        LocalDateTime thirtyDaysAgo = DateUtils.getStartOfBusinessDay(thirtyDaysAgoDate);
        long totalAppointments = appointmentRepository.countByBranchIdAndCreatedAtAfter(
            currentBranch.getId(), thirtyDaysAgo);
        long completedAppointments = appointmentRepository.countByBranchIdAndStatusAndCreatedAtAfter(
            currentBranch.getId(), AppointmentStatus.COMPLETED, thirtyDaysAgo);
        
        double completionRate = totalAppointments > 0 ? 
            (double) completedAppointments / totalAppointments * 100 : 0.0;
        performance.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        return baseResponse.successResponse(
                "Receptionist performance metrics fetched successfully",
                performance
        );
    }
}