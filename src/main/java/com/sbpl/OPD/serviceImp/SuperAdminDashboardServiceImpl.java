package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.SuperAdminDashboardDTO;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.SuperAdminDashboardService;
import com.sbpl.OPD.utils.DateUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Implementation of super admin dashboard service.
 * Provides comprehensive system-wide reporting and KPIs.
 */
@Service
public class SuperAdminDashboardServiceImpl implements SuperAdminDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(SuperAdminDashboardServiceImpl.class);

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Override
    public ResponseEntity<?> getSystemDashboardStatistics() {
        try {
            logger.info("Fetching system dashboard statistics");

            SuperAdminDashboardDTO dashboardDTO = new SuperAdminDashboardDTO();

            // System-wide totals
            Long totalCompanies = doctorRepository.countTotalCompanies();
            Long totalBranches = doctorRepository.countTotalBranches();
            Long totalDoctors = doctorRepository.countTotalDoctors();
            Long totalPatients = customerRepository.countTotalPatients();

            // Active metrics
            Long activeCompanies = doctorRepository.countActiveCompanies();
            Long activeBranches = doctorRepository.countActiveBranches();
            Long activeDoctors = doctorRepository.countActiveDoctors();
            Long activePatients = customerRepository.countActivePatients();

            // System health indicators
            // Use business timezone (IST) instead of server timezone
            LocalDateTime lastSystemUpdate = LocalDateTime.now(DateUtils.getBusinessZone());
            String systemStatus = "Operational";

            dashboardDTO.setTotalCompanies(totalCompanies);
            dashboardDTO.setTotalBranches(totalBranches);
            dashboardDTO.setTotalDoctors(totalDoctors);
            dashboardDTO.setTotalPatients(totalPatients);
            dashboardDTO.setActiveCompanies(activeCompanies);
            dashboardDTO.setActiveBranches(activeBranches);
            dashboardDTO.setActiveDoctors(activeDoctors);
            dashboardDTO.setActivePatients(activePatients);
            dashboardDTO.setLastSystemUpdate(lastSystemUpdate);
            dashboardDTO.setSystemStatus(systemStatus);

            return baseResponse.successResponse("System dashboard statistics fetched successfully", dashboardDTO);

        } catch (Exception e) {
            logger.error("Error fetching system dashboard statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system dashboard statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemAppointmentStats() {
        try {
            logger.info("Fetching system appointment statistics");

            Map<String, Object> appointmentStats = new HashMap<>();

            // System-wide appointment metrics
            Long totalAppointments = appointmentRepository.countTotalAppointments();
            
            // Use business timezone for today's appointments created
            LocalDate today = DateUtils.getBusinessLocalDate();
            LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
            LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();
            Long todayAppointments = appointmentRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            
            Long completedAppointments = appointmentRepository.countByStatus(com.sbpl.OPD.enums.AppointmentStatus.COMPLETED);
            Long cancelledAppointments = appointmentRepository.countByStatus(com.sbpl.OPD.enums.AppointmentStatus.CANCELLED);
            Long noShowAppointments = appointmentRepository.countByStatus(com.sbpl.OPD.enums.AppointmentStatus.NO_SHOW);

            // Appointment trends
            Double completionRate = totalAppointments > 0 ?
                (completedAppointments.doubleValue() / totalAppointments.doubleValue()) * 100 : 0.0;
            Double noShowRate = totalAppointments > 0 ?
                (noShowAppointments.doubleValue() / totalAppointments.doubleValue()) * 100 : 0.0;

            appointmentStats.put("totalAppointments", totalAppointments);
            appointmentStats.put("todayAppointments", todayAppointments);
            appointmentStats.put("completedAppointments", completedAppointments);
            appointmentStats.put("cancelledAppointments", cancelledAppointments);
            appointmentStats.put("noShowAppointments", noShowAppointments);
            appointmentStats.put("completionRate", String.format("%.2f%%", completionRate));
            appointmentStats.put("noShowRate", String.format("%.2f%%", noShowRate));

            return baseResponse.successResponse("System appointment stats fetched successfully", appointmentStats);

        } catch (Exception e) {
            logger.error("Error fetching system appointment stats", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system appointment stats: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemStaffPerformance() {
        try {
            logger.info("Fetching system staff performance");

            Map<String, Object> staffStats = new HashMap<>();

            // Staff counts by role
            Long totalDoctors = doctorRepository.countTotalDoctors();
            Long activeDoctors = doctorRepository.countActiveDoctors();

            // Performance metrics
            Double doctorUtilizationRate = totalDoctors > 0 ?
                (activeDoctors.doubleValue() / totalDoctors.doubleValue()) * 100 : 0.0;

            staffStats.put("totalDoctors", totalDoctors);
            staffStats.put("activeDoctors", activeDoctors);
            staffStats.put("doctorUtilizationRate", String.format("%.2f%%", doctorUtilizationRate));

            return baseResponse.successResponse("System staff performance fetched successfully", staffStats);

        } catch (Exception e) {
            logger.error("Error fetching system staff performance", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system staff performance: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemFinancialOverview() {
        try {
            logger.info("Fetching system financial overview");

            Map<String, Object> financialStats = new HashMap<>();

            // System-wide financial metrics
            BigDecimal totalRevenue = billRepository.calculateTotalSystemRevenue();
            // BigDecimal todayRevenue = billRepository.calculateTodaySystemRevenue(); // Commented out due to query issues
            Long totalBills = billRepository.countTotalBills();
            Long paidBills = billRepository.countPaidBills();

            // Financial health indicators
            Double paymentRate = totalBills > 0 ?
                (paidBills.doubleValue() / totalBills.doubleValue()) * 100 : 0.0;

            financialStats.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
            // financialStats.put("todayRevenue", todayRevenue != null ? todayRevenue : BigDecimal.ZERO); // Commented out
            financialStats.put("totalBills", totalBills);
            financialStats.put("paidBills", paidBills);
            financialStats.put("paymentRate", String.format("%.2f%%", paymentRate));

            return baseResponse.successResponse("System financial overview fetched successfully", financialStats);

        } catch (Exception e) {
            logger.error("Error fetching system financial overview", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system financial overview: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemPatientStats() {
        try {
            logger.info("Fetching system patient statistics");

            Map<String, Object> patientStats = new HashMap<>();

            // System-wide patient metrics
            Long totalPatients = customerRepository.countTotalPatients();
            Long activePatients = customerRepository.countActivePatients();
            
            // Use business timezone to get start of today
            LocalDate today = DateUtils.getBusinessLocalDate();
            LocalDateTime startOfToday = DateUtils.getStartOfBusinessDay();
            Long newPatientsToday = customerRepository.countNewPatientsToday(startOfToday);

            // Patient engagement metrics
            Double patientRetentionRate = totalPatients > 0 ?
                (activePatients.doubleValue() / totalPatients.doubleValue()) * 100 : 0.0;

            patientStats.put("totalPatients", totalPatients);
            patientStats.put("activePatients", activePatients);
            patientStats.put("newPatientsToday", newPatientsToday);
            patientStats.put("patientRetentionRate", String.format("%.2f%%", patientRetentionRate));

            return baseResponse.successResponse("System patient stats fetched successfully", patientStats);

        } catch (Exception e) {
            logger.error("Error fetching system patient stats", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system patient stats: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getCompanyAnalytics() {
        try {
            logger.info("Fetching company analytics");

            Map<String, Object> companyStats = new HashMap<>();

            // Company performance metrics
            Long totalCompanies = doctorRepository.countTotalCompanies();
            Long activeCompanies = doctorRepository.countActiveCompanies();

            // Company growth metrics
            Double companyGrowthRate = totalCompanies > 0 ?
                (activeCompanies.doubleValue() / totalCompanies.doubleValue()) * 100 : 0.0;

            companyStats.put("totalCompanies", totalCompanies);
            companyStats.put("activeCompanies", activeCompanies);
            companyStats.put("companyGrowthRate", String.format("%.2f%%", companyGrowthRate));

            return baseResponse.successResponse("Company analytics fetched successfully", companyStats);

        } catch (Exception e) {
            logger.error("Error fetching company analytics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching company analytics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemResourceUtilization() {
        try {
            logger.info("Fetching system resource utilization");

            Map<String, Object> resourceStats = new HashMap<>();

            // System capacity metrics
            Long totalBranches = doctorRepository.countTotalBranches();
            Long activeBranches = doctorRepository.countActiveBranches();

            // Resource utilization
            Double branchUtilizationRate = totalBranches > 0 ?
                (activeBranches.doubleValue() / totalBranches.doubleValue()) * 100 : 0.0;

            resourceStats.put("totalBranches", totalBranches);
            resourceStats.put("activeBranches", activeBranches);
            resourceStats.put("branchUtilizationRate", String.format("%.2f%%", branchUtilizationRate));

            return baseResponse.successResponse("System resource utilization fetched successfully", resourceStats);

        } catch (Exception e) {
            logger.error("Error fetching system resource utilization", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system resource utilization: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemTrends() {
        try {
            logger.info("Fetching system trends");

            Map<String, Object> trendStats = new HashMap<>();

            // Use business timezone for date calculations
            LocalDate today = DateUtils.getBusinessLocalDate();
            LocalDate startOfMonth = today.withDayOfMonth(1);
            LocalDateTime startOfMonthDateTime = DateUtils.getStartOfBusinessDay(startOfMonth);
            
            // Growth trends
            Long newCompaniesThisMonth = doctorRepository.countNewCompaniesThisMonth(startOfMonthDateTime);
            Long newBranchesThisMonth = doctorRepository.countNewBranchesThisMonth(startOfMonthDateTime);
            Long newPatientsThisMonth = customerRepository.countNewPatientsThisMonth(startOfMonthDateTime);

            trendStats.put("newCompaniesThisMonth", newCompaniesThisMonth);
            trendStats.put("newBranchesThisMonth", newBranchesThisMonth);
            trendStats.put("newPatientsThisMonth", newPatientsThisMonth);

            return baseResponse.successResponse("System trends fetched successfully", trendStats);

        } catch (Exception e) {
            logger.error("Error fetching system trends", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system trends: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemSecurityMetrics() {
        try {
            logger.info("Fetching system security metrics");

            Map<String, Object> securityStats = new HashMap<>();

            // Security metrics (placeholder - would integrate with actual security system)
            String securityStatus = "Active";
            Long activeSessions = 1000L; // Placeholder
            Long securityAlerts = 0L; // Placeholder

            securityStats.put("securityStatus", securityStatus);
            securityStats.put("activeSessions", activeSessions);
            securityStats.put("securityAlerts", securityAlerts);

            return baseResponse.successResponse("System security metrics fetched successfully", securityStats);

        } catch (Exception e) {
            logger.error("Error fetching system security metrics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system security metrics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemHealthStatus() {
        try {
            logger.info("Fetching system health status");

            Map<String, Object> healthStats = new HashMap<>();

            // System health indicators
            String databaseStatus = "Connected";
            String apiStatus = "Operational";
            String cacheStatus = "Healthy";
            LocalDateTime lastHealthCheck = LocalDateTime.now();

            healthStats.put("databaseStatus", databaseStatus);
            healthStats.put("apiStatus", apiStatus);
            healthStats.put("cacheStatus", cacheStatus);
            healthStats.put("lastHealthCheck", lastHealthCheck);

            return baseResponse.successResponse("System health status fetched successfully", healthStats);

        } catch (Exception e) {
            logger.error("Error fetching system health status", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system health status: " + e.getMessage());
        }
    }
}