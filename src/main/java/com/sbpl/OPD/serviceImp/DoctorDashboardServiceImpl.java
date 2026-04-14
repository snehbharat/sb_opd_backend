package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.DoctorBasicInfoDTO;
import com.sbpl.OPD.dto.DoctorDashboardDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.DoctorDashboardService;
import com.sbpl.OPD.utils.DateUtils;
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
import java.util.stream.Collectors;

/**
 * Implementation of doctor dashboard service.
 * Provides comprehensive performance metrics and reporting for doctors.
 */
@Service
public class DoctorDashboardServiceImpl implements DoctorDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorDashboardServiceImpl.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Override
    public ResponseEntity<?> getDoctorDashboardStatistics(Long doctorId) {
        try {
            Doctor doctor = getDoctorOrThrow(doctorId);

            DoctorDashboardDTO dashboardDTO = new DoctorDashboardDTO();

            // Set basic doctor info
            DoctorBasicInfoDTO doctorInfo = new DoctorBasicInfoDTO();
            doctorInfo.setDoctorId(doctor.getId());
            doctorInfo.setDoctorName(doctor.getDoctorName());
            doctorInfo.setSpecialization(doctor.getSpecialization());
            doctorInfo.setDepartment(doctor.getDepartment());
            doctorInfo.setConsultationFee(doctor.getConsultationFee());
            doctorInfo.setIsActive(doctor.getIsActive());
            doctorInfo.setExperienceYears(doctor.getExperienceYears());
            dashboardDTO.setDoctorInfo(doctorInfo);

            // Today's appointments summary
            dashboardDTO.setTodaysAppointments(getTodaysAppointmentsStats(doctorId));

            // Weekly performance overview
            dashboardDTO.setWeeklyPerformance(getWeeklyPerformanceStats(doctorId));

            // Appointment status distribution
            dashboardDTO.setAppointmentStatusDistribution(getAppointmentStatusDistribution(doctorId));

            // Patient statistics
            dashboardDTO.setPatientStats(getDoctorPatientStatistics(doctorId));

            // Upcoming appointments
            dashboardDTO.setUpcomingAppointments(getUpcomingAppointments(doctorId));

            // Recent activities
            dashboardDTO.setRecentActivities(getRecentActivities(doctorId));

            // Performance metrics
            dashboardDTO.setPerformanceMetrics(getPerformanceMetrics(doctorId));

            return baseResponse.successResponse("Doctor dashboard statistics fetched successfully", dashboardDTO);

        } catch (IllegalArgumentException e) {
            logger.warn("Doctor dashboard statistics failed [doctorId={}] | {}", doctorId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching doctor dashboard statistics [doctorId={}]", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch dashboard statistics");
        }
    }

    @Override
    public ResponseEntity<?> getDoctorAppointmentStats(Long doctorId, Integer days) {
        try {
            getDoctorOrThrow(doctorId);
            int daysBack = days != null ? days : 30;
            // Use business timezone (IST) instead of server timezone
            LocalDate startDate = DateUtils.getBusinessLocalDate().minusDays(daysBack);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = DateUtils.getEndOfBusinessDay();

            Map<String, Object> stats = new HashMap<>();

            // Appointment counts by status
            Map<String, Long> statusCounts = new HashMap<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
                    doctorId, status, startDateTime, endDateTime);
                statusCounts.put(status.name(), count);
            }
            stats.put("statusCounts", statusCounts);

            // Total appointments
            long totalAppointments = appointmentRepository.countByDoctorIdAndAppointmentDateBetween(
                doctorId, startDateTime, endDateTime);
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

            return baseResponse.successResponse("Doctor appointment stats fetched successfully", stats);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching doctor appointment stats [doctorId={}]", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch appointment stats");
        }
    }

    @Override
    public ResponseEntity<?> getDoctorPerformanceMetrics(Long doctorId) {
        try {
            getDoctorOrThrow(doctorId);
            return baseResponse.successResponse("Doctor performance metrics fetched successfully",
                getPerformanceMetrics(doctorId));
        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching doctor performance metrics [doctorId={}]", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch performance metrics");
        }
    }

    @Override
    public ResponseEntity<?> getDoctorPatientStats(Long doctorId) {
        try {
            getDoctorOrThrow(doctorId);
            return baseResponse.successResponse("Doctor patient stats fetched successfully",
                getDoctorPatientStatistics(doctorId));
        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching doctor patient stats [doctorId={}]", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch patient stats");
        }
    }

    @Override
    public ResponseEntity<?> getDoctorRevenueStats(Long doctorId, Integer days) {
        // Implementation would require billing integration
        // This is a placeholder for future implementation
        return baseResponse.successResponse("Doctor revenue stats - feature coming soon",
            Map.of("message", "Revenue analytics will be available in future updates"));
    }

    @Override
    public ResponseEntity<?> getDoctorScheduleUtilization(Long doctorId, Integer days) {
        // Implementation would require schedule integration
        // This is a placeholder for future implementation
        return baseResponse.successResponse("Doctor schedule utilization - feature coming soon",
            Map.of("message", "Schedule utilization analytics will be available in future updates"));
    }

    // Private helper methods
    private Doctor getDoctorOrThrow(Long doctorId) {
        return doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
    }

    private Map<String, Object> getTodaysAppointmentsStats(Long doctorId) {
        // Use business timezone to determine "today" properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
        LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();

        Map<String, Object> stats = new HashMap<>();

        // Count appointments created today (based on createdAt field)
        long totalToday = appointmentRepository.countByDoctorIdAndCreatedAtBetween(
            doctorId, startOfDay, endOfDay);
        stats.put("total", totalToday);

        // Confirmed appointments created today
        long confirmed = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
        stats.put("confirmed", confirmed);

        // Completed appointments created today
        long completed = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
        stats.put("completed", completed);

        // Pending appointments created today
        long pending = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
        stats.put("pending", pending);

        // Cancelled appointments created today
        long cancelled = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.CANCELLED, startOfDay, endOfDay);
        stats.put("cancelled", cancelled);

        // Rescheduled appointments created today
        long rescheduled = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.RESCHEDULED, startOfDay, endOfDay);
        stats.put("rescheduled", rescheduled);

        // No-show appointments created today
        long noShow = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.NO_SHOW, startOfDay, endOfDay);
        stats.put("noShow", noShow);

        return stats;
    }

    private Map<String, Object> getWeeklyPerformanceStats(Long doctorId) {
        // Use business timezone to determine dates properly
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate weekStart = today.minusDays(7);
        LocalDateTime weekStartDateTime = DateUtils.getStartOfBusinessDay(weekStart);
        LocalDateTime weekEndDateTime = DateUtils.getEndOfBusinessDay(today);

        Map<String, Object> weeklyStats = new HashMap<>();

        // Appointment date based statistics
        long weeklyTotal = appointmentRepository.countByDoctorIdAndAppointmentDateBetween(
            doctorId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("total", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
            (double) weeklyCompleted / weeklyTotal * 100 : 0.0;
        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // No-show appointments in week
        long weeklyNoShow = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.NO_SHOW, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("noShow", weeklyNoShow);

        double noShowRate = weeklyTotal > 0 ?
            (double) weeklyNoShow / weeklyTotal * 100 : 0.0;
        weeklyStats.put("noShowRate", Math.round(noShowRate * 100.0) / 100.0);

        // Creation date based statistics
        long weeklyCreatedTotal = appointmentRepository.countByDoctorIdAndCreatedAtBetween(
            doctorId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalCreated", weeklyCreatedTotal);

        long weeklyCreatedCompleted = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("createdCompleted", weeklyCreatedCompleted);

        double createdCompletionRate = weeklyCreatedTotal > 0 ?
            (double) weeklyCreatedCompleted / weeklyCreatedTotal * 100 : 0.0;
        weeklyStats.put("createdCompletionRate", Math.round(createdCompletionRate * 100.0) / 100.0);

        return weeklyStats;
    }

    private Map<String, Long> getAppointmentStatusDistribution(Long doctorId) {
        Map<String, Long> statusDistribution = new HashMap<>();

        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByDoctorIdAndStatus(doctorId, status);
            statusDistribution.put(status.name(), count);
        }

        // Add no-show rate calculation
        Long total = statusDistribution.values().stream().mapToLong(Long::longValue).sum();
        Long noShowCount = statusDistribution.getOrDefault("NO_SHOW", 0L);
        double noShowRate = total > 0 ? (double) noShowCount / total * 100 : 0.0;
        statusDistribution.put("noShowRate", (long) (Math.round(noShowRate * 100.0) / 100.0));

        return statusDistribution;
    }

    private Map<String, Object> getDoctorPatientStatistics(Long doctorId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();

        // Total unique patients
        long totalPatients = customerRepository.countByDoctorId(doctorId);
        patientStats.put("totalUniquePatients", totalPatients);

        // New patients this month
        LocalDate monthStart = today.withDayOfMonth(1);
        LocalDateTime monthStartDateTime = DateUtils.getStartOfBusinessDay(monthStart);
        long newPatients = customerRepository.countNewPatientsByDoctorIdAndDate(
            doctorId, monthStartDateTime);
        patientStats.put("newPatientsThisMonth", newPatients);

        // Repeat patients (patients with multiple appointments)
        long repeatPatients = customerRepository.countRepeatPatientsByDoctorId(doctorId);
        patientStats.put("repeatPatients", repeatPatients);

        return patientStats;
    }

    private List<Map<String, Object>> getUpcomingAppointments(Long doctorId) {
        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate nextWeek = today.plusDays(7);
        LocalDateTime startDateTime = DateUtils.getStartOfBusinessDay(today);
        LocalDateTime endDateTime = DateUtils.getEndOfBusinessDay(nextWeek);

        PageRequest pageRequest = PageRequest.of(0, 10);

        List<Appointment> upcomingAppointments = appointmentRepository
            .findByDoctorIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
                doctorId, startDateTime, endDateTime, AppointmentStatus.CONFIRMED, pageRequest)
            .getContent();

        return upcomingAppointments.stream()
            .map(appointment -> {
                Map<String, Object> appointmentMap = new HashMap<>();
                appointmentMap.put("id", appointment.getId());
                appointmentMap.put("patientName", appointment.getPatient().getFirstName() + " " +
                    appointment.getPatient().getLastName());
                appointmentMap.put("appointmentDate", appointment.getAppointmentDate());
                appointmentMap.put("scheduledTime", appointment.getScheduledTime());
                appointmentMap.put("status", appointment.getStatus());
                return appointmentMap;
            })
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentActivities(Long doctorId) {
        // Use business timezone for date calculations
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDate oneWeekAgoDate = today.minusDays(7);
        LocalDateTime oneWeekAgo = DateUtils.getStartOfBusinessDay(oneWeekAgoDate);

        List<Appointment> recentAppointments = appointmentRepository
            .findByDoctorIdAndUpdatedAtAfterOrderByUpdatedAtDesc(
                doctorId, oneWeekAgo, PageRequest.of(0, 15))
            .getContent();

        return recentAppointments.stream()
            .map(appointment -> {
                Map<String, Object> activity = new HashMap<>();
                activity.put("id", appointment.getId());
                activity.put("type", "APPOINTMENT_" + appointment.getStatus());
                activity.put("patientName", appointment.getPatient().getFirstName() + " " +
                    appointment.getPatient().getLastName());
                activity.put("appointmentDate", appointment.getAppointmentDate());
                activity.put("status", appointment.getStatus());
                activity.put("updatedAt", appointment.getUpdatedAt());
                return activity;
            })
            .collect(Collectors.toList());
    }

    private Map<String, Object> getPerformanceMetrics(Long doctorId) {
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

        long cancelledAppointments = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.CANCELLED, thirtyDaysAgoDateTime, now);

        long noShowAppointments = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.NO_SHOW, thirtyDaysAgoDateTime, now);

        // Completion rate
        double completionRate = totalAppointments > 0 ?
            (double) completedAppointments / totalAppointments * 100 : 0.0;
        metrics.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // No-show rate
        double noShowRate = totalAppointments > 0 ?
            (double) noShowAppointments / totalAppointments * 100 : 0.0;
        metrics.put("noShowRate", Math.round(noShowRate * 100.0) / 100.0);

        // Cancellation rate
        double cancellationRate = totalAppointments > 0 ?
            (double) cancelledAppointments / totalAppointments * 100 : 0.0;
        metrics.put("cancellationRate", Math.round(cancellationRate * 100.0) / 100.0);

        // Average daily appointments
        int daysInPeriod = 30;
        double avgDaily = (double) totalAppointments / daysInPeriod;
        metrics.put("avgDailyAppointments", Math.round(avgDaily * 100.0) / 100.0);

        // Patient satisfaction proxy (repeat patients ratio)
        long totalUniquePatients = customerRepository.countByDoctorId(doctorId);
        long repeatPatients = customerRepository.countRepeatPatientsByDoctorId(doctorId);
        double patientRetentionRate = totalUniquePatients > 0 ?
            (double) repeatPatients / totalUniquePatients * 100 : 0.0;
        metrics.put("patientRetentionRate", Math.round(patientRetentionRate * 100.0) / 100.0);

        return metrics;
    }
}