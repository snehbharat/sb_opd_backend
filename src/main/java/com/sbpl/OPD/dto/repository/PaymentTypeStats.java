package com.sbpl.OPD.dto.repository;

import com.sbpl.OPD.enums.PaymentType;

import java.math.BigDecimal;

public interface PaymentTypeStats {

  PaymentType getPaymentType();

  Long getTotalTransactions();

  BigDecimal getTotalPaidAmount();
}
