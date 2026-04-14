package com.sbpl.OPD.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class StaffBillingStatsDTO {
    private Long staffId;
    private String staffName;
    private Long totalBillsCount;
    private BigDecimal totalAmountBilled;
    private BigDecimal totalAmountCollected;
    private BigDecimal totalAmountPending;
}