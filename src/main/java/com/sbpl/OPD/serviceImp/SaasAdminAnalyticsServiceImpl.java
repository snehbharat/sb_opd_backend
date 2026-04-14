package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.SaasAdminAnalyticsService;
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

/**
 * Implementation of SAAS admin analytics service.
 * Provides comprehensive performance metrics and reporting for company-wide monitoring.
 */
@Service
public class SaasAdminAnalyticsServiceImpl implements SaasAdminAnalyticsService {

    private static final Logger logger = LoggerFactory.getLogger(SaasAdminAnalyticsServiceImpl.class);

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private CustomerRepository customerRepository;

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
    public ResponseEntity<?> getCompanyDashboardStatistics() {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

            Map<String, Object> dashboardData = new HashMap<>();
            
            // Basic company info
            dashboardData.put("companyInfo", getCompanyBasicInfo(currentCompany));
            
            // Today's overview
            dashboardData.put("todaysOverview", getTodaysCompanyStats(currentCompany.getId()));
            
            // Weekly performance
            dashboardData.put("weeklyPerformance", getWeeklyCompanyStats(currentCompany.getId()));
            
            // Appointment status distribution
            dashboardData.put("appointmentStatusDistribution", getCompanyAppointmentStatusDistribution(currentCompany.getId()));
            
            // Branch performance
            dashboardData.put("branchPerformance", getBranchPerformance(currentCompany.getId()));
            
            // Staff metrics
            dashboardData.put("staffMetrics", getCompanyStaffMetrics(currentCompany.getId()));
            
            // Patient statistics
//            dashboardData.put("patientStats", getCompanyPatientStats(currentCompany.getId()));
            
            // Financial overview
            dashboardData.put("financialOverview", getCompanyFinancialOverview(30).getBody());
            
            // Recent activities
            dashboardData.put("recentActivities", getCompanyRecentActivities(currentCompany.getId()));
            
            // System health metrics
            dashboardData.put("systemHealth", getSystemHealthMetrics().getBody());
            
            return baseResponse.successResponse("Company dashboard statistics fetched successfully", dashboardData);
            
        } catch (Exception e) {
            logger.error("Error fetching company dashboard statistics", e);
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
            LocalDate startDate = LocalDate.now().minusDays(daysBack);
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = LocalDateTime.now();
            
            Map<String, Object> stats = new HashMap<>();
            
            // Appointment counts by status
            Map<String, Long> statusCounts = new HashMap<>();
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByCompanyIdAndStatusAndAppointmentDateBetween(
                    currentCompany.getId(), status, startDateTime, endDateTime);
                statusCounts.put(status.name(), count);
            }
            stats.put("statusCounts", statusCounts);
            
            // Total appointments
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
            
            List<Map<String, Object>> staffPerformance = new ArrayList<>();
            
            for (Branch branch : branches) {
                // Get all doctors in branch
                List<Doctor> doctors = doctorRepository.findByBranchId(branch.getId());
                
                for (Doctor doctor : doctors) {
                    Map<String, Object> doctorPerformance = new HashMap<>();
                    doctorPerformance.put("doctorId", doctor.getId());
                    doctorPerformance.put("doctorName", doctor.getDoctorName());
                    doctorPerformance.put("specialization", doctor.getSpecialization());
                    doctorPerformance.put("department", doctor.getDepartment());
                    doctorPerformance.put("branchName", branch.getBranchName());
                    doctorPerformance.put("branchId", branch.getId());
                    
                    // Get doctor's performance metrics
                    doctorPerformance.put("performanceMetrics", getDoctorPerformanceMetrics(doctor.getId()));
                    
                    staffPerformance.add(doctorPerformance);
                }
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
    public ResponseEntity<?> getCompanyTrends(Integer days) {
        try {
            User currentUser = getCurrentUserWithCompany();

            if (currentUser.getCompany() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any company");
            }

            CompanyProfile currentCompany = currentUser.getCompany();

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
                
                long count = appointmentRepository.countByCompanyIdAndAppointmentDateBetween(
                    currentCompany.getId(), dayStart, dayEnd);
                dailyCounts.put(currentDate.toString(), count);
                
                currentDate = currentDate.plusDays(1);
            }
            trends.put("dailyAppointmentCounts", dailyCounts);
            
            // Status breakdown for period
            Map<String, Long> statusBreakdown = new HashMap<>();
            LocalDateTime startDateTime = startDate.atStartOfDay();
            LocalDateTime endDateTime = endDate.atTime(23, 59, 59);
            
            for (AppointmentStatus status : AppointmentStatus.values()) {
                long count = appointmentRepository.countByCompanyIdAndStatusAndAppointmentDateBetween(
                    currentCompany.getId(), status, startDateTime, endDateTime);
                statusBreakdown.put(status.name(), count);
            }
            trends.put("statusBreakdown", statusBreakdown);
            
            return baseResponse.successResponse("Company trends fetched successfully", trends);
            
        } catch (Exception e) {
            logger.error("Error fetching company trends", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch trends");
        }
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
    private Map<String, Object> getCompanyBasicInfo(CompanyProfile company) {
        Map<String, Object> info = new HashMap<>();
        info.put("companyId", company.getId());
        info.put("companyName", company.getCompanyName());
        info.put("email", company.getEmail());
        info.put("phoneNumber", company.getPhoneNumber() != null ? company.getPhoneNumber() : company.getCompanyPhone());
        info.put("address", company.getAddress());
        info.put("gstinNumber", company.getGstinNumber());
        return info;
    }

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

    private Map<String, Long> getCompanyAppointmentStatusDistribution(Long companyId) {
        Map<String, Long> statusDistribution = new HashMap<>();
        
        for (AppointmentStatus status : AppointmentStatus.values()) {
            long count = appointmentRepository.countByCompanyIdAndStatus(companyId, status);
            statusDistribution.put(status.name(), count);
        }
        
        return statusDistribution;
    }

    private Map<String, Object> getBranchPerformance(Long companyId) {
        Map<String, Object> branchPerformance = new HashMap<>();
        
        List<Branch> branches = branchRepository.findByClinicId(companyId);
        
        for (Branch branch : branches) {
            Map<String, Object> branchStats = new HashMap<>();
            
            // Get appointments for this branch
            long branchAppointments = appointmentRepository.countByBranchId(branch.getId());
            branchStats.put("totalAppointments", branchAppointments);
            
            // Get completed appointments
            long completedAppointments = appointmentRepository.countByBranchIdAndStatus(
                branch.getId(), AppointmentStatus.COMPLETED);
            branchStats.put("completedAppointments", completedAppointments);
            
            // Get doctors count
            long doctorCount = doctorRepository.countByBranchId(branch.getId());
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
        
        // Get all branches in company
        List<Branch> branches = branchRepository.findByClinicId(companyId);
        
        long totalDoctors = 0;
        long activeDoctors = 0;
        long totalStaff = 0;
        long activeStaff = 0;
        
        // Define staff roles (excluding admin roles)
        List<UserRole> staffRoles = Arrays.asList(
            UserRole.DOCTOR,
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

    private Map<String, Object> getCompanyPatientStatistics(Long companyId) {
        Map<String, Object> patientStats = new HashMap<>();
        
        // Get all branches in company
        List<Branch> branches = branchRepository.findByClinicId(companyId);
        
        long totalPatients = 0;
        long newPatientsThisMonth = 0;
        long activePatients = 0;
        
        LocalDate monthStart = LocalDate.now().withDayOfMonth(1);
        LocalDateTime monthStartDateTime = monthStart.atStartOfDay();
        LocalDate threeMonthsAgo = LocalDate.now().minusMonths(3);
        LocalDateTime threeMonthsAgoDateTime = threeMonthsAgo.atStartOfDay();
        
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

    private List<Map<String, Object>> getCompanyRecentActivities(Long companyId) {
        LocalDateTime oneWeekAgo = LocalDate.now().minusDays(7).atStartOfDay();
        List<Map<String, Object>> activities = new ArrayList<>();
        
        try {
            // Get all branches for the company
            List<Branch> branches = branchRepository.findByClinicId(companyId);
            
            // Collect recent appointments
            for (Branch branch : branches) {
                List<Object[]> recentAppointments = appointmentRepository.findRecentAppointmentsByBranchId(
                    branch.getId(), oneWeekAgo, PageRequest.of(0, 10));
                
                for (Object[] appointmentData : recentAppointments) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "APPOINTMENT");
                    activity.put("branchName", branch.getBranchName());
                    activity.put("patientName", appointmentData[0] + " " + appointmentData[1]);
                    activity.put("doctorName", appointmentData[2]);
                    activity.put("status", appointmentData[3]);
                    activity.put("appointmentDate", appointmentData[4]);
                    activity.put("message", String.format("New appointment for %s with Dr. %s at %s", 
                        appointmentData[0] + " " + appointmentData[1], appointmentData[2], branch.getBranchName()));
                    activities.add(activity);
                }
            }
            
            // Collect recent customer registrations
            for (Branch branch : branches) {
                List<Customer> recentCustomers = customerRepository.findRecentCustomersByBranchId(
                    branch.getId(), oneWeekAgo, PageRequest.of(0, 5));
                
                for (Customer customer : recentCustomers) {
                    Map<String, Object> activity = new HashMap<>();
                    activity.put("type", "CUSTOMER_REGISTRATION");
                    activity.put("branchName", branch.getBranchName());
                    activity.put("customerName", customer.getFirstName() + " " + customer.getLastName());
                    activity.put("phoneNumber", customer.getPhoneNumber());
                    activity.put("registrationDate", customer.getCreatedAt());
                    activity.put("message", String.format("New patient registered: %s at %s", 
                        customer.getFirstName() + " " + customer.getLastName(), branch.getBranchName()));
                    activities.add(activity);
                }
            }
            
            // Sort by date and limit to 20 most recent activities
            activities.sort((a, b) -> {
                LocalDateTime dateA = (LocalDateTime) a.get("appointmentDate");
                LocalDateTime dateB = (LocalDateTime) b.get("appointmentDate");
                if (dateA == null) dateA = (LocalDateTime) a.get("registrationDate");
                if (dateB == null) dateB = (LocalDateTime) b.get("registrationDate");
                return dateB.compareTo(dateA); // Descending order
            });
            
            return activities.size() > 20 ? activities.subList(0, 20) : activities;
            
        } catch (Exception e) {
            logger.error("Error fetching recent activities for company {}", companyId, e);
            // Return empty list instead of placeholder data
            return new ArrayList<>();
        }
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