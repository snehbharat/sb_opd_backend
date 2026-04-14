package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.BillDTO;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.BillService;
import com.sbpl.OPD.service.InvoicePdfService;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.RbacUtil;
import com.sbpl.OPD.utils.ReceiptFormatter;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * REST controller for billing operations.
 * <p>
 * Handles bill creation, retrieval, payment processing,
 * and billing status updates.
 * <p>
 * Exposes APIs for financial tracking
 * and patient billing history.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/bills")
public class BillController {

    @Autowired
    private BillService billService;

    @Autowired
    private RbacUtil rbacUtil;

    @Autowired
    private InvoicePdfService invoicePdfService;

    @GetMapping("/all")
    public ResponseEntity<?> getAllBills(@RequestParam(required = false) Integer pageNo,
                                         @RequestParam(required = false) Integer pageSize,
                                         @RequestParam(required = false) Long branchId,
                                         @RequestParam(required = false) String startDate,
                                         @RequestParam(required = false) String endDate) {
        return billService.getAllBills(pageNo, pageSize, branchId, startDate, endDate);
    }

    @GetMapping("/all/stats")
    public ResponseEntity<?> getAllBillsStats(@RequestParam(required = false) Long branchId,
                                              @RequestParam(required = false) String startDate,
                                              @RequestParam(required = false) String endDate) {
        return billService.getAllBillsStats(branchId, startDate, endDate);
    }

    @GetMapping("/billing/summary/stats")
    public ResponseEntity<?> getBillingSummary(
            @RequestParam String dateFilter,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {

        return billService.getBillingSummaryStats(dateFilter, branchId,startDate,endDate);
    }

    @GetMapping("/stats/for-staff")
    public ResponseEntity<?> getStaffBillingStats(
            @RequestParam String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate
    ) {

        return billService.getStaffBillingStats(
                dateFilter,
                startDate,
                endDate
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getBillById(@PathVariable @Valid Long id) {

        return billService.getBillById(id);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createBill(@RequestBody @Valid BillDTO billDTO) {

        return billService.createBill(billDTO);
    }

    @PostMapping("/new/create")
    public ResponseEntity<?> createBillNew(@RequestBody @Valid BillDTO billDTO) {
        return billService.createBillNew(billDTO);
    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateBill(@PathVariable @Valid Long id, @RequestBody BillDTO billDTO) {

        return billService.updateBill(id, billDTO);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteBill(@PathVariable Long id) {
        if (!rbacUtil.hasAnyRole(UserRole.BILLING_STAFF,
                UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to delete bill");
        }
        billService.deleteBill(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/patient/{patientId}")
    public ResponseEntity<?> getBillsByPatientId(@PathVariable Long patientId,
                                                 @RequestParam(required = false) Integer pageNo,
                                                 @RequestParam(required = false) Integer pageSize) {
        return billService.getBillsByPatientId(patientId, pageNo, pageSize);
    }

    @GetMapping("/me/staff")
    public ResponseEntity<?> getBillsByBillingStaffId(@RequestParam(required = false) Integer pageNo,
                                                      @RequestParam(required = false) Integer pageSize) {
        return billService.getBillsByBillingStaffId(pageNo, pageSize);
    }

    @GetMapping("/staff/{staffId}")
    public ResponseEntity<?> getBillsByStaffId(@PathVariable Long staffId,
                                               @RequestParam(required = false) Integer pageNo,
                                               @RequestParam(required = false) Integer pageSize) {
        return billService.getBillsByStaffId(staffId, pageNo, pageSize);
    }

    @GetMapping("/by-status")
    public ResponseEntity<?> getBillsByStatus(@RequestParam BillStatus status,
                                              @RequestParam(required = false) Integer pageNo,
                                              @RequestParam(required = false) Integer pageSize) {

        return billService.getBillsByStatus(status, pageNo, pageSize);
    }

    @GetMapping("/appointment/{appointmentId}")
    public ResponseEntity<?> getBillsByAppointmentId(@PathVariable Long appointmentId,
                                                     @RequestParam(required = false) Integer pageNo,
                                                     @RequestParam(required = false) Integer pageSize) {
        return billService.getBillsByAppointmentId(appointmentId, pageNo, pageSize);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateBillStatus(@PathVariable Long id, @RequestParam BillStatus status) {
        return billService.updateBillStatus(id, status);
    }

    @PutMapping("/{id}/payment")
    public ResponseEntity<?> processPayment(@PathVariable Long id,
                                            @RequestParam Double amount,
                                            @RequestParam String paymentType) {

        return billService.processPayment(id, amount, paymentType);
    }


    /**
     * Cancel my bill - Only accessible to billing staff with BILLING_CANCEL permission
     * and only for bills they created
     */
    @PutMapping("/{id}/cancel")
    public ResponseEntity<?> cancelMyBill(@PathVariable Long id) {
        return billService.updateBillStatus(id, BillStatus.CANCELLED);
    }


    /**
     * Get billing summary for a specific staff member
     * GET /api/v1/bills/staff/{staffId}/summary
     */
    @GetMapping("/staff/{staffId}/summary")
    public ResponseEntity<?> getStaffBillingSummary(@PathVariable Long staffId) {
        // Only admins and branch managers can view other staff summaries
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing summary");
        }
        return billService.getStaffBillingSummary(staffId);
    }

    /**
     * Get billing summary for a specific staff member by date range
     * GET /api/v1/bills/staff/{staffId}/summary/filter
     */
    @GetMapping("/staff/{staffId}/summary/filter")
    public ResponseEntity<?> getStaffBillingSummaryByDateRange(
            @PathVariable Long staffId,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // Only admins and branch managers can view other staff summaries
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing summary");
        }
        return billService.getStaffBillingSummaryByDateRange(staffId, dateFilter, startDate, endDate);
    }

    /**
     * Get billing overview for all staff members with date filtering
     * GET /api/v1/bills/staff/overview/filter
     */
    @GetMapping("/staff/overview/filter")
    public ResponseEntity<?> getStaffBillingOverviewByDateRange(
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(required = false) Long branchId) {
        // Only admins and branch managers can view staff billing overview
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing overview");
        }
        return billService.getStaffBillingOverviewByDateRange(dateFilter, startDate, endDate, branchId);
    }

    /**
     * Get detailed billing report for a specific staff member
     * GET /api/v1/bills/staff/{staffId}/report
     */
    @GetMapping("/staff/{staffId}/report")
    public ResponseEntity<?> getStaffBillingReport(
            @PathVariable Long staffId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        // Only admins and branch managers can view other staff reports
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing report");
        }
        return billService.getStaffBillingReport(staffId, pageNo, pageSize);
    }

    /**
     * Get detailed billing report for a specific staff member with date filtering
     * GET /api/v1/bills/staff/{staffId}/report/full
     */
    @GetMapping("/staff/{staffId}/report/full")
    public ResponseEntity<?> getFullStaffBillingReport(
            @PathVariable Long staffId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // Only admins and branch managers can view other staff reports
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing report");
        }
        return billService.getStaffBillingReportWithDateFilter(staffId, pageNo, pageSize, dateFilter, startDate, endDate);
    }

    /**
     * Get comprehensive billing report for a specific staff member with all analytics
     * GET /api/v1/bills/staff/{staffId}/report/analytics
     */
    @GetMapping("/staff/{staffId}/report/analytics")
    public ResponseEntity<?> getStaffComprehensiveReport(
            @PathVariable Long staffId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        // Only admins and branch managers can view other staff reports
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing report");
        }
        return billService.getStaffBillingReport(staffId, pageNo, pageSize);
    }

    /**
     * Get billing overview for all staff members
     * GET /api/v1/bills/staff/overview
     */
    @GetMapping("/staff/overview")
    public ResponseEntity<?> getStaffBillingOverview(
            @RequestParam(required = false) Long branchId) {
        // Only admins and branch managers can view staff billing overview
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing overview");
        }
        return billService.getStaffBillingOverview(branchId);
    }

    /**
     * Get detailed billing report for a specific staff member with date filtering
     * GET /api/v1/bills/staff/{staffId}/report/filter
     */
    @GetMapping("/staff/{staffId}/report/filter")
    public ResponseEntity<?> getStaffBillingReportWithDateFilter(
            @PathVariable Long staffId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        // Only admins and branch managers can view other staff reports
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff billing report");
        }
        return billService.getStaffBillingReportWithDateFilter(staffId, pageNo, pageSize, dateFilter, startDate, endDate);
    }

    /**
     * Get collection rate for a specific staff member
     * GET /api/v1/bills/staff/{staffId}/collection-rate
     */
    @GetMapping("/staff/{staffId}/collection-rate")
    public ResponseEntity<?> getStaffCollectionRate(@PathVariable Long staffId) {
        // Only admins and branch managers can view other staff collection rates
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff collection rate");
        }
        return billService.getStaffCollectionRate(staffId);
    }

    /**
     * Get top performing staff based on collection rate
     * GET /api/v1/bills/top-performing-staff
     */
    @GetMapping("/top-performing-staff")
    public ResponseEntity<?> getTopPerformingStaff(
            @RequestParam(required = false) Integer limit) {
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view top performing staff");
        }
        return billService.getTopPerformingStaff(limit);
    }

    /**
     * Get pending bills for a specific staff member
     * GET /api/v1/bills/staff/{staffId}/pending-bills
     */
    @GetMapping("/staff/{staffId}/pending-bills")
    public ResponseEntity<?> getStaffPendingBills(
            @PathVariable Long staffId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        // Only admins and branch managers can view other staff pending bills
        if (!rbacUtil.hasAnyRole(UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to view staff pending bills");
        }
        return billService.getStaffPendingBills(staffId, pageNo, pageSize);
    }

    /**
     * My Billing Analytics Endpoints (for logged-in billing staff)
     */

    /**
     * Get my billing summary
     * GET /api/v1/bills/my/summary
     */
    @GetMapping("/my/summary")
    public ResponseEntity<?> getMyBillingSummary() {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return billService.getStaffBillingSummary(currentUserId);
    }

    /**
     * Get my detailed billing report
     * GET /api/v1/bills/my/report
     */
    @GetMapping("/my/report")
    public ResponseEntity<?> getMyBillingReport(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return billService.getStaffBillingReport(currentUserId, pageNo, pageSize);
    }

    /**
     * Get my collection rate
     * GET /api/v1/bills/my/collection-rate
     */
    @GetMapping("/my/collection-rate")
    public ResponseEntity<?> getMyCollectionRate() {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return billService.getStaffCollectionRate(currentUserId);
    }

    /**
     * Get my pending bills
     * GET /api/v1/bills/my/pending-bills
     */
    @GetMapping("/my/pending-bills")
    public ResponseEntity<?> getMyPendingBills(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return billService.getStaffPendingBills(currentUserId, pageNo, pageSize);
    }


    @GetMapping("/payment-types")
    public ResponseEntity<?> getPaymentTypes() {
        return billService.getAllPaymentType();
    }


    /**
     * Generate a formatted receipt for a specific bill
     * GET /api/v1/bills/{id}/receipt
     */
    @GetMapping("/{id}/receipt")
    public ResponseEntity<?> generateReceipt(@PathVariable Long id) {
        // Ensure user has permission to access receipt (RECEPTIONIST, BILLING_STAFF, and admin roles)
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST, UserRole.BILLING_STAFF,
                UserRole.BRANCH_MANAGER, UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role to access receipt");
        }
        return billService.generateReceipt(id);
    }

    @GetMapping("/my/summary/filter")
    public ResponseEntity<?> getMyBillingSummaryByDateRange(
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return billService.getStaffBillingSummaryByDateRange(currentUserId, dateFilter, startDate, endDate);
    }

    @GetMapping("/my/report/filter")
    public ResponseEntity<?> getStaffBillingReportWithDateFilter(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) String dateFilter,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return billService.getStaffBillingReportWithDateFilter(currentUserId, pageNo, pageSize, dateFilter, startDate, endDate);
    }

    /**
     * Generate a formatted receipt for a specific bill
     * GET /api/v1/bills/{id}/formatted-receipt
     */
    @GetMapping("/{id}/formatted-receipt")
    public ResponseEntity<String> generateFormattedReceipt(@PathVariable Long id) {
        ResponseEntity<?> response = billService.generateReceipt(id);

        if (response.getStatusCode().is2xxSuccessful()) {
            Object responseBody = response.getBody();
            if (responseBody instanceof com.sbpl.OPD.response.ResponseDto) {
                com.sbpl.OPD.response.ResponseDto responseDto = (com.sbpl.OPD.response.ResponseDto) responseBody;
                Object data = responseDto.getData();
                if (data instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> receiptData = (Map<String, Object>) data;
                    String formattedReceipt = ReceiptFormatter.formatReceipt(receiptData);
                    return ResponseEntity.ok(formattedReceipt);
                }
            }
            return ResponseEntity.ok("Receipt data format not recognized");
        }

        return ResponseEntity.status(response.getStatusCode()).body("Error generating receipt: " + response.getStatusCode());
    }

    /**
     * Upload invoice PDF to S3 bucket and send email to customer
     * POST /api/v1/bills/upload-invoice-pdf
     *
     * @param file MultipartFile containing the invoice PDF
     * @param billNo Bill number for reference
     * @return ResponseEntity with upload result and S3 URL
     */
    @PostMapping("/upload-invoice-pdf")
    public ResponseEntity<?> uploadInvoicePdf(
            @RequestParam("file") MultipartFile file,
            @RequestParam("billNo") String billNo) {
        
        return invoicePdfService.uploadInvoicePdf(file, billNo);
    }

    @GetMapping("/payment-type/stats")
    public ResponseEntity<?> getPaymentTypeStats(@RequestParam(required = false) String startDate,
                                                 @RequestParam(required = false) String endDate,
                                                 @RequestParam(required = false) Long branchId) {
        return billService.getPaymentTypeStats(startDate, endDate, branchId);
    }


    @GetMapping("/patient/{patientId}/with-packages")
    public ResponseEntity<?> getBillsByPatientIdWithPackages(@PathVariable Long patientId,
                                                             @RequestParam Integer pageNo,
                                                             @RequestParam Integer pageSize) {
        return billService.getBillsByPatientIdWithPackageDetails(patientId, pageNo, pageSize);
    }


}