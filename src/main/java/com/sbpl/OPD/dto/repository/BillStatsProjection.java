package com.sbpl.OPD.dto.repository;

import java.math.BigDecimal;

/**
 * Bill Stats Projection Dto.
 *
 * @author Kousik Manik
 */
public interface BillStatsProjection {

  Long getTotalBills();

  BigDecimal getTotalAmount();

  BigDecimal getCollectedAmount();

  BigDecimal getBalanceAmount();

  BigDecimal getDueAmountToday();
}
