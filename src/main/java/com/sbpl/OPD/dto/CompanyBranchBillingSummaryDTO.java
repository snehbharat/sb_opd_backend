package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

/**
 * DTO for company/branch-wise billing summary.
 * Provides aggregated billing statistics by company and branch.
 *
 * @author HMS Team
 */
@Getter
@Setter
public class CompanyBranchBillingSummaryDTO {
    // Company Information
    private Long companyId;
    private String companyName;
    private String companyCode;
    
    // Branch Information
    private Long branchId;
    private String branchName;
    
    // Overall Statistics
    private Long totalStaff;
    private Long totalBills;
    private BigDecimal totalBilledAmount;
    private BigDecimal totalCollectedAmount;
    private BigDecimal totalPendingAmount;
    
    // Staff-wise Breakdown
    private List<StaffBillingSummaryDTO> staffSummaries;
    
    // Performance Metrics
    private Double overallCollectionRate;
    private Long totalPaidBills;
    private Long totalPendingBills;
    private BigDecimal averageBillPerStaff;
}