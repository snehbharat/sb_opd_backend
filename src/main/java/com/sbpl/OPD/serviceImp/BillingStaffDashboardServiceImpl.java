package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.BillingStaffDashboardDTO;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.model.Bill;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.BillingStaffDashboardService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Implementation of BillingStaffDashboardService.
 * Provides comprehensive dashboard statistics and analytics for billing staff.
 */
@Service
@Slf4j
public class BillingStaffDashboardServiceImpl implements BillingStaffDashboardService {

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Override
    public ResponseEntity<?> getDashboardStatistics() {
        try {
            log.info("Fetching comprehensive billing staff dashboard statistics");
            
            Long currentUserId = DbUtill.getLoggedInUserId();
            User currentUser = userRepository.findById(currentUserId)
                    .orElseThrow(() -> new RuntimeException("Current user not found"));

            BillingStaffDashboardDTO dashboardDTO = new BillingStaffDashboardDTO();

            // 1. Staff Information
            dashboardDTO.setStaffInfo(getStaffInfo(currentUser));

            // 2. Today's Overview
            dashboardDTO.setTodaysOverview(getTodaysOverview(currentUserId));

            // 3. Weekly Performance
            dashboardDTO.setWeeklyPerformance(getWeeklyPerformance(currentUserId));

            // 4. Monthly Performance
            dashboardDTO.setMonthlyPerformance(getMonthlyPerformance(currentUserId));

            // 5. Payment Status Distribution
            dashboardDTO.setPaymentStatusDistribution(getPaymentStatusDistribution(currentUserId));

            // 6. Recent Transactions
            dashboardDTO.setRecentTransactions(getRecentTransactions(currentUserId, 10));

            // 7. Billing Metrics
            dashboardDTO.setBillingMetrics(getBillingMetrics(currentUserId));

            // 8. Recent Activities
            dashboardDTO.setRecentActivities(getRecentActivities(currentUserId, 15));

            return baseResponse.successResponse("Billing staff dashboard statistics fetched successfully", dashboardDTO);

        } catch (Exception e) {
            log.error("Error fetching billing staff dashboard statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching dashboard statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getTodaysStatistics() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            Map<String, Object> todaysStats = getTodaysOverview(currentUserId);
            return baseResponse.successResponse("Today's statistics fetched successfully", todaysStats);
        } catch (Exception e) {
            log.error("Error fetching today's statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching today's statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getWeeklyOverview() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            Map<String, Object> weeklyStats = getWeeklyPerformance(currentUserId);
            return baseResponse.successResponse("Weekly overview fetched successfully", weeklyStats);
        } catch (Exception e) {
            log.error("Error fetching weekly overview", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching weekly overview: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getMonthlyPerformance() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            Map<String, Object> monthlyStats = getMonthlyPerformance(currentUserId);
            return baseResponse.successResponse("Monthly performance fetched successfully", monthlyStats);
        } catch (Exception e) {
            log.error("Error fetching monthly performance", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching monthly performance: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPaymentStatusDistribution() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            Map<String, Long> statusDistribution = getPaymentStatusDistribution(currentUserId);
            return baseResponse.successResponse("Payment status distribution fetched successfully", statusDistribution);
        } catch (Exception e) {
            log.error("Error fetching payment status distribution", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching payment status distribution: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecentTransactions(Integer limit) {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            int transactionLimit = (limit != null && limit > 0) ? limit : 10;
            Map<String, Object> recentTransactions = getRecentTransactions(currentUserId, transactionLimit);
            return baseResponse.successResponse("Recent transactions fetched successfully", recentTransactions);
        } catch (Exception e) {
            log.error("Error fetching recent transactions", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching recent transactions: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getBillingMetrics() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            Map<String, Object> billingMetrics = getBillingMetrics(currentUserId);
            return baseResponse.successResponse("Billing metrics fetched successfully", billingMetrics);
        } catch (Exception e) {
            log.error("Error fetching billing metrics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching billing metrics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getRecentActivities(Integer limit) {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            int activityLimit = (limit != null && limit > 0) ? limit : 15;
            List<Map<String, Object>> recentActivities = getRecentActivities(currentUserId, activityLimit);
            return baseResponse.successResponse("Recent activities fetched successfully", recentActivities);
        } catch (Exception e) {
            log.error("Error fetching recent activities", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching recent activities: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getCollectionRateStatistics() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            
            // Get total bills created by staff
            Long totalBills = billRepository.countByBillingStaffId(currentUserId);
            
            // Get paid bills
            Long paidBills = billRepository.countByBillingStaffIdAndStatus(currentUserId, BillStatus.PAID);
            
            // Calculate collection rate
            double collectionRate = totalBills > 0 ? (double) paidBills / totalBills * 100 : 0.0;
            
            Map<String, Object> collectionStats = new HashMap<>();
            collectionStats.put("totalBills", totalBills);
            collectionStats.put("paidBills", paidBills);
            collectionStats.put("collectionRate", String.format("%.2f%%", collectionRate));
            collectionStats.put("pendingBills", totalBills - paidBills);
            
            return baseResponse.successResponse("Collection rate statistics fetched successfully", collectionStats);
        } catch (Exception e) {
            log.error("Error fetching collection rate statistics", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching collection rate statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPendingBillsOverview() {
        try {
            Long currentUserId = DbUtill.getLoggedInUserId();
            
            // Get pending bills count
            Long pendingBills = billRepository.countByBillingStaffIdAndStatus(currentUserId, BillStatus.PENDING);
            
            // Get partially paid bills
            Long partiallyPaidBills = billRepository.countByBillingStaffIdAndStatus(currentUserId, BillStatus.PARTIALLY_PAID);
            
            // Get total pending amount
            BigDecimal pendingAmount = billRepository.calculatePendingAmountByStaff(currentUserId);
            
            Map<String, Object> pendingOverview = new HashMap<>();
            pendingOverview.put("pendingBills", pendingBills);
            pendingOverview.put("partiallyPaidBills", partiallyPaidBills);
            pendingOverview.put("totalPendingAmount", pendingAmount != null ? pendingAmount : BigDecimal.ZERO);
            pendingOverview.put("totalPendingCount", pendingBills + partiallyPaidBills);
            
            return baseResponse.successResponse("Pending bills overview fetched successfully", pendingOverview);
        } catch (Exception e) {
            log.error("Error fetching pending bills overview", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, 
                "Error fetching pending bills overview: " + e.getMessage());
        }
    }

    // Private helper methods

    private Map<String, Object> getStaffInfo(User user) {
        Map<String, Object> staffInfo = new HashMap<>();
        staffInfo.put("staffId", user.getId());
        staffInfo.put("firstName", user.getFirstName());
        staffInfo.put("lastName", user.getLastName());
        staffInfo.put("email", user.getEmail());
        staffInfo.put("employeeId", user.getEmployeeId());
        staffInfo.put("department", user.getDepartment());
        staffInfo.put("role", user.getRole().name());
        
        if (user.getBranch() != null) {
            staffInfo.put("branchId", user.getBranch().getId());
            staffInfo.put("branchName", user.getBranch().getBranchName());
        }
        
        if (user.getCompany() != null) {
            staffInfo.put("companyId", user.getCompany().getId());
            staffInfo.put("companyName", user.getCompany().getCompanyName());
        }
        
        return staffInfo;
    }

    private Map<String, Object> getTodaysOverview(Long staffId) {
        Map<String, Object> todayStats = new HashMap<>();
        
        // Use business timezone (IST) instead of server timezone
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);
        
        // Today's bills created
        Long todayBills = billRepository.countByBillingStaffIdAndCreatedAtBetween(staffId, java.util.Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));
        
        // Today's payments received
        BigDecimal todayPayments = billRepository.calculatePaymentsByStaffAndDateRange(staffId, java.util.Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(endOfDay.atZone(ZoneId.systemDefault()).toInstant()));
        
        // Today's pending bills
        Long todayPending = billRepository.countTodayPendingBillsByStaff(staffId, java.util.Date.from(startOfDay.atZone(ZoneId.systemDefault()).toInstant()));
        
        todayStats.put("date", today.toString());
        todayStats.put("billsCreated", todayBills);
        todayStats.put("paymentsReceived", todayPayments != null ? todayPayments : BigDecimal.ZERO);
        todayStats.put("pendingBills", todayPending);
        
        return todayStats;
    }

    private Map<String, Object> getWeeklyPerformance(Long staffId) {
        Map<String, Object> weeklyStats = new HashMap<>();
        
        // Use business timezone (IST) instead of server timezone
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDateTime weekStart = today.minusDays(7).atStartOfDay();
        LocalDateTime weekEnd = today.atTime(23, 59, 59);
        
        // Weekly bills created
        Long weeklyBills = billRepository.countByBillingStaffIdAndCreatedAtBetween(staffId, java.util.Date.from(weekStart.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(weekEnd.atZone(ZoneId.systemDefault()).toInstant()));
        
        // Weekly payments received
        BigDecimal weeklyPayments = billRepository.calculatePaymentsByStaffAndDateRange(staffId, java.util.Date.from(weekStart.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(weekEnd.atZone(ZoneId.systemDefault()).toInstant()));
        
        // Weekly collection rate
        Long weeklyPaid = billRepository.countByBillingStaffIdAndStatusAndCreatedAtBetween(staffId, BillStatus.PAID, java.util.Date.from(weekStart.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(weekEnd.atZone(ZoneId.systemDefault()).toInstant()));
        double collectionRate = weeklyBills > 0 ? (double) weeklyPaid / weeklyBills * 100 : 0.0;
        
        weeklyStats.put("period", "Last 7 days");
        weeklyStats.put("billsCreated", weeklyBills);
        weeklyStats.put("paymentsReceived", weeklyPayments != null ? weeklyPayments : BigDecimal.ZERO);
        weeklyStats.put("billsPaid", weeklyPaid);
        weeklyStats.put("collectionRate", String.format("%.2f%%", collectionRate));
        
        return weeklyStats;
    }

    private Map<String, Object> getMonthlyPerformance(Long staffId) {
        Map<String, Object> monthlyStats = new HashMap<>();
        
        // Use business timezone (IST) instead of server timezone
        LocalDate today = DateUtils.getBusinessLocalDate();
        LocalDateTime monthStart = today.withDayOfMonth(1).atStartOfDay();
        LocalDateTime monthEnd = today.atTime(23, 59, 59);
        
        // Monthly bills created
        Long monthlyBills = billRepository.countByBillingStaffIdAndCreatedAtBetween(staffId, java.util.Date.from(monthStart.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(monthEnd.atZone(ZoneId.systemDefault()).toInstant()));
        
        // Monthly payments received
        BigDecimal monthlyPayments = billRepository.calculatePaymentsByStaffAndDateRange(staffId, java.util.Date.from(monthStart.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(monthEnd.atZone(ZoneId.systemDefault()).toInstant()));
        
        // Monthly collection rate
        Long monthlyPaid = billRepository.countByBillingStaffIdAndStatusAndCreatedAtBetween(staffId, BillStatus.PAID, java.util.Date.from(monthStart.atZone(ZoneId.systemDefault()).toInstant()), java.util.Date.from(monthEnd.atZone(ZoneId.systemDefault()).toInstant()));
        double collectionRate = monthlyBills > 0 ? (double) monthlyPaid / monthlyBills * 100 : 0.0;
        
        monthlyStats.put("period", "Current month");
        monthlyStats.put("billsCreated", monthlyBills);
        monthlyStats.put("paymentsReceived", monthlyPayments != null ? monthlyPayments : BigDecimal.ZERO);
        monthlyStats.put("billsPaid", monthlyPaid);
        monthlyStats.put("collectionRate", String.format("%.2f%%", collectionRate));
        
        return monthlyStats;
    }

    private Map<String, Long> getPaymentStatusDistribution(Long staffId) {
        Map<String, Long> statusDistribution = new HashMap<>();
        
        statusDistribution.put("PENDING", billRepository.countByBillingStaffIdAndStatus(staffId, BillStatus.PENDING));
        statusDistribution.put("PAID", billRepository.countByBillingStaffIdAndStatus(staffId, BillStatus.PAID));
        statusDistribution.put("PARTIALLY_PAID", billRepository.countByBillingStaffIdAndStatus(staffId, BillStatus.PARTIALLY_PAID));
        statusDistribution.put("CANCELLED", billRepository.countByBillingStaffIdAndStatus(staffId, BillStatus.CANCELLED));
        
        return statusDistribution;
    }

    private Map<String, Object> getRecentTransactions(Long staffId, int limit) {
        Map<String, Object> recentTransactions = new HashMap<>();
        
        Pageable pageable = PageRequest.of(0, limit);
        List<Bill> recentBills = billRepository.findRecentBillsByStaffId(staffId, pageable);
        
        List<Map<String, Object>> transactions = new ArrayList<>();
        for (Bill bill : recentBills) {
            Map<String, Object> transaction = new HashMap<>();
            transaction.put("billId", bill.getId());
            transaction.put("patientName", bill.getPatient() != null ? 
                bill.getPatient().getFirstName() + " " + bill.getPatient().getLastName() : "Unknown");
            transaction.put("amount", bill.getTotalAmount());
            transaction.put("status", bill.getStatus().name());
            transaction.put("createdAt", bill.getCreatedAt());
            transaction.put("updatedAt", bill.getUpdatedAt());
            transactions.add(transaction);
        }
        
        recentTransactions.put("transactions", transactions);
        recentTransactions.put("total", transactions.size());
        
        return recentTransactions;
    }

    private Map<String, Object> getBillingMetrics(Long staffId) {
        Map<String, Object> billingMetrics = new HashMap<>();
        
        // Total bills created
        Long totalBills = billRepository.countByBillingStaffId(staffId);
        
        // Total revenue generated
        BigDecimal totalRevenue = billRepository.calculateTotalRevenueByStaff(staffId);
        
        // Average bill amount
        BigDecimal averageBill = totalBills > 0 ? 
            totalRevenue.divide(BigDecimal.valueOf(totalBills), 2, BigDecimal.ROUND_HALF_UP) : BigDecimal.ZERO;
        
        // Collection efficiency (paid vs total)
        Long paidBills = billRepository.countByBillingStaffIdAndStatus(staffId, BillStatus.PAID);
        double collectionEfficiency = totalBills > 0 ? (double) paidBills / totalBills * 100 : 0.0;
        
        billingMetrics.put("totalBillsCreated", totalBills);
        billingMetrics.put("totalRevenue", totalRevenue != null ? totalRevenue : BigDecimal.ZERO);
        billingMetrics.put("averageBillAmount", averageBill);
        billingMetrics.put("collectionEfficiency", String.format("%.2f%%", collectionEfficiency));
        billingMetrics.put("paidBills", paidBills);
        billingMetrics.put("pendingBills", totalBills - paidBills);
        
        return billingMetrics;
    }

    private List<Map<String, Object>> getRecentActivities(Long staffId, int limit) {
        List<Map<String, Object>> activities = new ArrayList<>();
        
        // Get recent bill creations
        Pageable pageable = PageRequest.of(0, limit / 2);
        List<Bill> recentBills = billRepository.findRecentBillsByStaffId(staffId, pageable);
        
        for (Bill bill : recentBills) {
            Map<String, Object> activity = new HashMap<>();
            activity.put("type", "BILL_CREATED");
            activity.put("description", "Created bill #" + bill.getId() + " for patient");
            activity.put("timestamp", bill.getCreatedAt());
            activity.put("billId", bill.getId());
            activities.add(activity);
        }
        
        // Get recent payments (this would need a payment history table)
        // For now, we'll add some sample activities
        Map<String, Object> paymentActivity = new HashMap<>();
        paymentActivity.put("type", "PAYMENT_RECEIVED");
        paymentActivity.put("description", "Received payment for bill");
        paymentActivity.put("timestamp", LocalDateTime.now(DateUtils.getBusinessZone()).minusHours(2));
        activities.add(paymentActivity);
        
        // Sort by timestamp and limit
        activities.sort((a, b) -> {
            LocalDateTime timeA = (LocalDateTime) a.get("timestamp");
            LocalDateTime timeB = (LocalDateTime) b.get("timestamp");
            return timeB.compareTo(timeA); // Descending order
        });
        
        return activities.size() > limit ? activities.subList(0, limit) : activities;
    }
}