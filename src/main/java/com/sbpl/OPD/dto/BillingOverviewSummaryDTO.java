package com.sbpl.OPD.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class BillingOverviewSummaryDTO {

    private Long paidBillCount;
    private Long pendingBillCount;
    private Double totalCollectedAmount;
    private Double pendingAmount;

}