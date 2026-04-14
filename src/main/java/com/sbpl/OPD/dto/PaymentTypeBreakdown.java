package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

// Supporting DTOs for analytics
@Getter
@Setter
public class PaymentTypeBreakdown {
    private String paymentType;
    private BigDecimal amount;
    private Long count;
    
    public PaymentTypeBreakdown(String paymentType, BigDecimal amount, Long count) {
        this.paymentType = paymentType;
        this.amount = amount;
        this.count = count;
    }
}
