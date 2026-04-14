package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.enums.PaymentType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * DTO for detailed staff billing report.
 * Shows individual bill details for staff-wise tracking with comprehensive financial metrics.
 *
 * @author HMS Team
 */
@Getter
@Setter
public class StaffBillingReportDTO {
    // Bill Information
    private Long billId;
    private String billNumber;
    private Long patientId;
    private String patientName;
    private String patientPhone;
    private String patientEmail;
    private String patientUhid;
    private BillStatus status;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private LocalDateTime createdAt;
    private LocalDateTime paymentDate;
    private PaymentType paymentType;
    private String notes;
    private String couponCode;
    private BigDecimal couponAmount;
    
    // Staff Information
    private Long billingStaffId;
    private String billingStaffName;
    private String billingStaffRole;
    private String billingStaffEmail;
    private String billingStaffPhone;
    private String billingStaffDepartment;
    
    // Company/Branch Information
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
    
    // Appointment Information (if applicable)
    private Long appointmentId;
    private String appointmentType;
    private LocalDateTime appointmentDate;
    private LocalDateTime appointmentStartTime;
    private LocalDateTime appointmentEndTime;
    
    // Doctor Information (if available)
    private Long doctorId;
    private String doctorName;
    private String doctorSpecialization;
    private String doctorQualification;
    
    // Bill Items Information
    private List<Map<String, Object>> billItems;
    
    // Comprehensive Financial Metrics (Analytics Section)
    private Long paidBillsCountForStaff;           // Number of paid bills
    private Long partiallyPaidBillsCountForStaff;  // Number of partially paid bills
    private Long cancelledBillsCountForStaff;      // Number of cancelled bills
    private Double collectionRateForStaff;         // Collection percentage for this staff
    private BigDecimal averageBillAmountForStaff;  // Average bill amount for this staff
    private BigDecimal dailyCollectionAvgForStaff; // Average daily collection for this staff
    private List<PaymentTypeBreakdown> paymentTypeBreakdownForStaff; // Payment type distribution
}