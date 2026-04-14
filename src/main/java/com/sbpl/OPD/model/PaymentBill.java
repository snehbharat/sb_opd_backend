package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.enums.PaymentType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Getter
@Setter
@Table(name = "payment_bill", schema = "sb_opd", indexes = {
    @Index(name = "idx_payment_bill_original_bill_id_created_ms", columnList = "original_bill_id, created_at_ms"),
    @Index(name = "idx_payment_billstatus_created_ms", columnList = "status, created_at_ms"),
    @Index(name = "idx_payment_bill_status_created", columnList = "status, created_at")})
public class PaymentBill extends BaseEntity {

  @Column(name = "original_bill_id", nullable = false)
  private Long originalBillId;

  @Column(name = "total_amount", nullable = false)
  private BigDecimal totalAmount;

  @Column(name = "paid_amount", nullable = false)
  private BigDecimal paidAmount;

  @Column(name = "payment_type")
  @Enumerated(EnumType.STRING)
  private PaymentType paymentType;

}
