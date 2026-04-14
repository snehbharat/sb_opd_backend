package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.AppointmentDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.DoctorAnalyticsService;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of doctor analytics service.
 * Provides comprehensive performance metrics and reporting for doctors.
 */
@Service
public class DoctorAnalyticsServiceImpl implements DoctorAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(DoctorAnalyticsServiceImpl.class);

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

            Map<String, Object> dashboardData = new HashMap<>();

            // Basic doctor info
            dashboardData.put("doctorInfo", getDoctorBasicInfo(doctor));

            // Today's appointments summary
            dashboardData.put("todaysAppointments", getTodaysAppointmentsStats(doctorId));

            // Weekly performance overview
            dashboardData.put("weeklyPerformance", getWeeklyPerformanceStats(doctorId));

            // Appointment status distribution
            dashboardData.put("appointmentStatusDistribution", getAppointmentStatusDistribution(doctorId));

            // Patient statistics
            dashboardData.put("patientStats", getDoctorPatientStats(doctorId));

            // Upcoming appointments
            dashboardData.put("upcomingAppointments", getUpcomingAppointments(doctorId));

            // Recent activities
            dashboardData.put("recentActivities", getRecentActivities(doctorId));

            // Performance metrics
            dashboardData.put("performanceMetrics", getPerformanceMetrics(doctorId));

            return baseResponse.successResponse("Doctor dashboard statistics fetched successfully", dashboardData);

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
            LocalDate startDate = LocalDate.now().minusDays(daysBack);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = LocalDateTime.now();

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

    @Override
    public ResponseEntity<?> getDoctorSpecializationAnalytics(Long doctorId) {
        try {
            Doctor doctor = getDoctorOrThrow(doctorId);

            Map<String, Object> analytics = new HashMap<>();
            analytics.put("specialization", doctor.getSpecialization());
            analytics.put("subSpecialization", doctor.getSubSpecialization());
            analytics.put("department", doctor.getDepartment());

            // Get appointment distribution by reason (proxy for specialization usage)
            List<Appointment> recentAppointments = appointmentRepository
                .findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateDesc(
                    doctorId,
                    LocalDate.now().minusDays(90).atStartOfDay(),
                    LocalDateTime.now(),
                    PageRequest.of(0, 100))
                .getContent();

            Map<String, Long> reasonDistribution = recentAppointments.stream()
                .collect(Collectors.groupingBy(
                    Appointment::getReason,
                    Collectors.counting()));

            analytics.put("appointmentReasonDistribution", reasonDistribution);

            return baseResponse.successResponse("Doctor specialization analytics fetched successfully", analytics);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching doctor specialization analytics [doctorId={}]", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch specialization analytics");
        }
    }

    @Override
    public ResponseEntity<?> getComparativeDoctorAnalytics(Long[] doctorIds) {
        try {
            List<Map<String, Object>> comparativeData = new ArrayList<>();

            for (Long doctorId : doctorIds) {
                Doctor doctor = getDoctorOrThrow(doctorId);
                Map<String, Object> doctorData = new HashMap<>();

                doctorData.put("doctorId", doctorId);
                doctorData.put("doctorName", doctor.getDoctorName());
                doctorData.put("specialization", doctor.getSpecialization());
                doctorData.put("performanceMetrics", getPerformanceMetrics(doctorId));
                doctorData.put("appointmentStats", getDoctorAppointmentStats(doctorId, 30).getBody());

                comparativeData.add(doctorData);
            }

            return baseResponse.successResponse("Comparative doctor analytics fetched successfully", comparativeData);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching comparative doctor analytics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch comparative analytics");
        }
    }

    @Override
    public ResponseEntity<?> getDoctorTrends(Long doctorId, Integer days) {
        try {
            getDoctorOrThrow(doctorId);
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

                long count = appointmentRepository.countByDoctorIdAndAppointmentDateBetween(
                    doctorId, dayStart, dayEnd);
                dailyCounts.put(currentDate.toString(), count);

                currentDate = currentDate.plusDays(1);
            }
            trends.put("dailyAppointmentCounts", dailyCounts);

            // Status breakdown for period
            Map<String, Long> statusBreakdown = new HashMap<>();
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);

            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
                    doctorId, status, startDateTime, endDateTime);
                statusBreakdown.put(status.name(), count);
            }
            trends.put("statusBreakdown", statusBreakdown);

            return baseResponse.successResponse("Doctor trends fetched successfully", trends);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching doctor trends [doctorId={}]", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch trends");
        }
    }

    // Private helper methods
    private Doctor getDoctorOrThrow(Long doctorId) {
        return doctorRepository.findById(doctorId)
            .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
    }

    private Map<String, Object> getDoctorBasicInfo(Doctor doctor) {
        Map<String, Object> info = new HashMap<>();
        info.put("doctorId", doctor.getId());
        info.put("doctorName", doctor.getDoctorName());
        info.put("specialization", doctor.getSpecialization());
        info.put("department", doctor.getDepartment());
        info.put("consultationFee", doctor.getConsultationFee());
        info.put("isActive", doctor.getIsActive());
        info.put("experienceYears", doctor.getExperienceYears());
        return info;
    }

    private Map<String, Object> getTodaysAppointmentsStats(Long doctorId) {
        // Get today's date range in business timezone
        LocalDateTime[] todayRange = DateUtils.getBusinessDayRange();
        LocalDateTime startOfDay = todayRange[0];
        LocalDateTime endOfDay = todayRange[1];

        Map<String, Object> stats = new HashMap<>();

        long totalToday = appointmentRepository.countByDoctorIdAndCreatedAtBetween(
            doctorId, startOfDay, endOfDay);
        stats.put("total", totalToday);

        long confirmed = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
        stats.put("confirmed", confirmed);

        long completed = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
        stats.put("completed", completed);

        long pending = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
        stats.put("pending", pending);

        return stats;
    }

    private Map<String, Object> getWeeklyPerformanceStats(Long doctorId) {
        // Get week's date range in business timezone
        LocalDateTime[] weekRange = DateUtils.getBusinessWeekRange();
        LocalDateTime weekStartDateTime = weekRange[0];
        LocalDateTime weekEndDateTime = weekRange[1];

        Map<String, Object> weeklyStats = new HashMap<>();

        long weeklyTotal = appointmentRepository.countByDoctorIdAndCreatedAtBetween(
            doctorId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("total", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByDoctorIdAndStatusAndCreatedAtBetween(
            doctorId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
            (double) weeklyCompleted / weeklyTotal * 100 : 0.0;
        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        return weeklyStats;
    }

    private Map<String, Long> getAppointmentStatusDistribution(Long doctorId) {
        Map<String, Long> statusDistribution = new HashMap<>();

        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByDoctorIdAndStatus(doctorId, status);
            statusDistribution.put(status.name(), count);
        }

        return statusDistribution;
    }

    private Map<String, Object> getDoctorPatientStatistics(Long doctorId) {
        Map<String, Object> patientStats = new HashMap<>();

        // Total unique patients
        long totalPatients = customerRepository.countByDoctorId(doctorId);
        patientStats.put("totalUniquePatients", totalPatients);

        // New patients this month
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        long newPatients = customerRepository.countNewPatientsByDoctorIdAndDate(
            doctorId, monthStartDateTime);
        patientStats.put("newPatientsThisMonth", newPatients);

        // Repeat patients (patients with multiple appointments)
        long repeatPatients = customerRepository.countRepeatPatientsByDoctorId(doctorId);
        patientStats.put("repeatPatients", repeatPatients);

        return patientStats;
    }

    private List<AppointmentDTO> getUpcomingAppointments(Long doctorId) {
        LocalDate today = LocalDate.now();
        LocalDate nextWeek = today.plusDays(7);
        LocalDateTime startDateTime = today.atStartOfDay();
        LocalDateTime endDateTime = nextWeek.atTime(23, 59, 59);

        PageRequest pageRequest = PageRequest.of(0, 10);

        List<Appointment> upcomingAppointments = appointmentRepository
            .findByDoctorIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
                doctorId, startDateTime, endDateTime, AppointmentStatus.CONFIRMED, pageRequest)
            .getContent();

        return upcomingAppointments.stream()
            .map(this::convertToDTO)
            .collect(Collectors.toList());
    }

    private List<Map<String, Object>> getRecentActivities(Long doctorId) {
        LocalDateTime oneWeekAgo = LocalDate.now().minusDays(7).atStartOfDay();

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

        // Get last 30 days data
        LocalDate thirtyDaysAgo = LocalDate.now().minusDays(30);
        LocalDateTime thirtyDaysAgoDateTime = thirtyDaysAgo.atStartOfDay();
        LocalDateTime now = LocalDateTime.now();

        long totalAppointments = appointmentRepository.countByDoctorIdAndAppointmentDateBetween(
            doctorId, thirtyDaysAgoDateTime, now);

        long completedAppointments = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.COMPLETED, thirtyDaysAgoDateTime, now);

        long cancelledAppointments = appointmentRepository.countByDoctorIdAndStatusAndAppointmentDateBetween(
            doctorId, AppointmentStatus.CANCELLED, thirtyDaysAgoDateTime, now);

        // Completion rate
        double completionRate = totalAppointments > 0 ?
            (double) completedAppointments / totalAppointments * 100 : 0.0;
        metrics.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

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
        dto.setPatientName(appointment.getPatient().getFirstName() + " " +
            appointment.getPatient().getLastName());
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
}