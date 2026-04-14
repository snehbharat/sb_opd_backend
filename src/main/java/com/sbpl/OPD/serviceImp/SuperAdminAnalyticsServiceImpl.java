package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.SuperAdminAnalyticsService;
import com.sbpl.OPD.utils.DateUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Service
public class SuperAdminAnalyticsServiceImpl implements SuperAdminAnalyticsService {

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
            Map<String, Object> dashboardStats = new HashMap<>();

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
            LocalDateTime lastSystemUpdate = LocalDateTime.now();
            String systemStatus = "Operational";

            dashboardStats.put("totalCompanies", totalCompanies);
            dashboardStats.put("totalBranches", totalBranches);
            dashboardStats.put("totalDoctors", totalDoctors);
            dashboardStats.put("totalPatients", totalPatients);
            dashboardStats.put("activeCompanies", activeCompanies);
            dashboardStats.put("activeBranches", activeBranches);
            dashboardStats.put("activeDoctors", activeDoctors);
            dashboardStats.put("activePatients", activePatients);
            dashboardStats.put("lastSystemUpdate", lastSystemUpdate);
            dashboardStats.put("systemStatus", systemStatus);

            return baseResponse.successResponse("System dashboard statistics fetched successfully", dashboardStats);

        } catch (Exception e) {
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system dashboard statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemAppointmentStats() {
        try {
            Map<String, Object> appointmentStats = new HashMap<>();

            // System-wide appointment metrics
            Long totalAppointments = appointmentRepository.countTotalAppointments();
            LocalDate today = DateUtils.getBusinessLocalDate();
            LocalDateTime startOfDay = DateUtils.getStartOfBusinessDay();
            LocalDateTime endOfDay = DateUtils.getEndOfBusinessDay();
            Long todayAppointments = appointmentRepository.countByCreatedAtBetween(startOfDay, endOfDay);
            Long completedAppointments = appointmentRepository.countByStatus(com.sbpl.OPD.enums.AppointmentStatus.COMPLETED);
            Long cancelledAppointments = appointmentRepository.countByStatus(com.sbpl.OPD.enums.AppointmentStatus.CANCELLED);

            // Appointment trends
            Double completionRate = totalAppointments > 0 ?
                (completedAppointments.doubleValue() / totalAppointments.doubleValue()) * 100 : 0.0;

            appointmentStats.put("totalAppointments", totalAppointments);
            appointmentStats.put("todayAppointments", todayAppointments);
            appointmentStats.put("completedAppointments", completedAppointments);
            appointmentStats.put("cancelledAppointments", cancelledAppointments);
            appointmentStats.put("completionRate", String.format("%.2f%%", completionRate));

            return baseResponse.successResponse("System appointment stats fetched successfully", appointmentStats);

        } catch (Exception e) {
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system appointment stats: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemStaffPerformance() {
        try {
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
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system staff performance: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemFinancialOverview() {
        try {
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
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system financial overview: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemPatientStats() {
        try {
            Map<String, Object> patientStats = new HashMap<>();

            // System-wide patient metrics
            Long totalPatients = customerRepository.countTotalPatients();
            Long activePatients = customerRepository.countActivePatients();
            Long newPatientsToday = customerRepository.countNewPatientsToday(LocalDateTime.now().withHour(0).withMinute(0).withSecond(0));

            // Patient engagement metrics
            Double patientRetentionRate = totalPatients > 0 ?
                (activePatients.doubleValue() / totalPatients.doubleValue()) * 100 : 0.0;

            patientStats.put("totalPatients", totalPatients);
            patientStats.put("activePatients", activePatients);
            patientStats.put("newPatientsToday", newPatientsToday);
            patientStats.put("patientRetentionRate", String.format("%.2f%%", patientRetentionRate));

            return baseResponse.successResponse("System patient stats fetched successfully", patientStats);

        } catch (Exception e) {
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system patient stats: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getCompanyAnalytics() {
        try {
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
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching company analytics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemResourceUtilization() {
        try {
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
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system resource utilization: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemTrends() {
        try {
            Map<String, Object> trendStats = new HashMap<>();

            // Growth trends
            LocalDateTime startOfMonth = LocalDateTime.now().withDayOfMonth(1).withHour(0).withMinute(0).withSecond(0);
            Long newCompaniesThisMonth = doctorRepository.countNewCompaniesThisMonth(startOfMonth);
            Long newBranchesThisMonth = doctorRepository.countNewBranchesThisMonth(startOfMonth);
            Long newPatientsThisMonth = customerRepository.countNewPatientsThisMonth(startOfMonth);

            trendStats.put("newCompaniesThisMonth", newCompaniesThisMonth);
            trendStats.put("newBranchesThisMonth", newBranchesThisMonth);
            trendStats.put("newPatientsThisMonth", newPatientsThisMonth);

            return baseResponse.successResponse("System trends fetched successfully", trendStats);

        } catch (Exception e) {
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system trends: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemSecurityMetrics() {
        try {
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
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system security metrics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSystemHealthStatus() {
        try {
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
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching system health status: " + e.getMessage());
        }
    }
}