package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.BillDTO;
import com.sbpl.OPD.enums.BillStatus;
import org.springframework.http.ResponseEntity;

public interface BillService {

    ResponseEntity<?> getBillsByPatientIdWithPackageDetails(Long patientId, Integer pageNo, Integer pageSize);

    /**
     * Get all bills with pagination
     */
    ResponseEntity<?> getAllBills(Integer pageNo, Integer pageSize, Long branchId, String startDate, String endDate);

    ResponseEntity<?> getAllBillsStats(Long branchId, String startDate, String endDate);

    /**
     * Get bill by ID
     */
    ResponseEntity<?> getBillById(Long id);

    /**
     * Create a new bill
     */
    ResponseEntity<?> createBill(BillDTO billDTO);

    ResponseEntity<?> createBillNew(BillDTO billDTO);

    /**
     * Update an existing bill
     */
    ResponseEntity<?> updateBill(Long id, BillDTO billDTO);

    /**
     * Delete a bill
     */
    ResponseEntity<?> deleteBill(Long id);

    ResponseEntity<?> getStaffBillingStats(String dateFilter, String startDate, String endDate);

    /**
     * Get bills by patient ID
     */
    ResponseEntity<?> getBillsByPatientId(Long patientId, Integer pageNo, Integer pageSize);

    /**
     * Get bills by billing staff ID (current logged-in user)
     */
    ResponseEntity<?> getBillsByBillingStaffId(Integer pageNo, Integer pageSize);

    /**
     * Get bills by specific staff ID (admin access)
     */
    ResponseEntity<?> getBillsByStaffId(Long staffId, Integer pageNo, Integer pageSize);

    /**
     * Get bills by status
     */
    ResponseEntity<?> getBillsByStatus(BillStatus status, Integer pageNo, Integer pageSize);

    /**
     * Get bills by appointment ID
     */
    ResponseEntity<?> getBillsByAppointmentId(Long appointmentId, Integer pageNo, Integer pageSize);

    /**
     * Update bill status
     */
    ResponseEntity<?> updateBillStatus(Long id, BillStatus status);

    /**
     * Process payment for a bill
     */
    ResponseEntity<?> processPayment(Long id, Double amount, String paymentType);

    /**
     * Get billing summary for a specific staff member
     */
    ResponseEntity<?> getStaffBillingSummary(Long staffId);

    ResponseEntity<?> getBillingSummaryStats(String dateFilter, Long branchId,String startDate, String endDate);

    /**
     * Get billing overview for all staff members with date filtering
     */
    ResponseEntity<?> getStaffBillingOverviewByDateRange(String dateFilter, String startDate, String endDate, Long branchId);

    /**
     * Get detailed billing report for a specific staff member
     */
    ResponseEntity<?> getStaffBillingReport(Long staffId, Integer pageNo, Integer pageSize);

    /**
     * Get detailed billing report for a specific staff member with date filtering
     */
    ResponseEntity<?> getStaffBillingReportWithDateFilter(Long staffId, Integer pageNo, Integer pageSize, String dateFilter, String startDate, String endDate);

    /**
     * Get collection rate for a specific staff member
     */
    ResponseEntity<?> getStaffCollectionRate(Long staffId);

    /**
     * Get top performing staff based on collection rate
     */
    ResponseEntity<?> getTopPerformingStaff(Integer limit);

    /**
     * Get pending bills for a specific staff member
     */
    ResponseEntity<?> getStaffPendingBills(Long staffId, Integer pageNo, Integer pageSize);


    ResponseEntity<?> getStaffBillingOverview(Long branchId);

    /**
     * Generate a formatted receipt for a specific bill
     */
    ResponseEntity<?> generateReceipt(Long billId);

    /**
     * Get billing summary for a specific staff member by date range
     */
    ResponseEntity<?> getStaffBillingSummaryByDateRange(Long staffId, String dateFilter, String startDate, String endDate);

    public ResponseEntity<?> getAllPaymentType();

    public ResponseEntity<?> getPaymentTypeStats(String startDate, String endDate, Long branchId);


}