package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.enums.DiscountType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;


@Getter
@Setter
@Entity
@Table(
    name = "coupon_details",
    schema = "sb_opd",
    uniqueConstraints = {
        @UniqueConstraint(columnNames = {"coupon_code", "branch_id"})
    },
    indexes = {
        @Index(name = "idx_coupon_code", columnList = "coupon_code"),
        @Index(name = "idx_coupon_branch", columnList = "branch_id"),
        @Index(name = "idx_coupon_active", columnList = "is_active"),
        @Index(name = "idx_coupon_branch_active", columnList = "branch_id,is_active")
    }
)
public class CouponDetails extends BaseEntity {

  @Column(name = "branch_id", nullable = false)
  private Long branchId;

  @Column(name = "coupon_code", nullable = false, length = 50)
  private String couponCode;

  @Column(name = "description", length = 255)
  private String description;

  // Discount amount or percentage
  @Column(name = "amount", precision = 10, scale = 2, nullable = false)
  private BigDecimal amount;

  @Column(name = "minimum_amount", precision = 10, scale = 2)
  private BigDecimal minimumAmount;

  @Column(name = "maximum_amount", precision = 10, scale = 2)
  private BigDecimal maximumAmount;

  @Column(name = "minimum_discount_amount", precision = 10, scale = 2)
  private BigDecimal minimumDiscountAmount;

  @Enumerated(EnumType.STRING)
  @Column(name = "discount_type", nullable = false)
  private DiscountType discountType;

  @Column(name = "is_active", nullable = false)
  private Boolean active = true;
}