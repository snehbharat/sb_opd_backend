package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * DTO for staff-wise billing summary and statistics.
 * Provides comprehensive billing analytics for individual staff members.
 *
 * @author HMS Team
 */
@Getter
@Setter
public class StaffBillingSummaryDTO {
    // Staff Information
    private Long staffId;
    private String staffName;
    private String staffRole;
    private String staffDepartment;
    
    // Company/Branch Information
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
    
    // Billing Statistics
    private Long totalBillsCreated;
    private BigDecimal totalAmountBilled;
    private BigDecimal totalAmountCollected;
    private BigDecimal totalAmountPending;
    private Long totalPaidBills;
    private Long totalPendingBills;
    private Long totalPartiallyPaidBills;
    private Long totalCancelledBills;
    
    // Collection Rate
    private Double collectionRate; // Percentage of collected amount vs total billed
    
    // Recent Activity
    private Long billsThisMonth;
    private BigDecimal amountBilledThisMonth;
    private BigDecimal amountCollectedThisMonth;
    
    // Average Values
    private BigDecimal averageBillAmount;
    private BigDecimal averageCollectionPerBill;
    
    // Date Range Information
    private java.time.LocalDateTime dateRangeStart;
    private java.time.LocalDateTime dateRangeEnd;
    private String dateRangeType;
}