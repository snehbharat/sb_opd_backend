package com.sbpl.OPD.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO for bill summary data.
 * Used for reporting and analytics purposes.
 */
@Data
public class BillSummaryDTO {
    private Long staffId;
    private String staffName;
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
    private Long totalBills;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal pendingAmount;
    private BigDecimal collectedAmount;
    private Double collectionRate;
    private Long pendingBillsCount;
    private Long pendingBills;
}