package com.sbpl.OPD.dto.coupon;

import com.sbpl.OPD.enums.DiscountType;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class CouponCreateDTO {
  private Long branchId;
  private String couponCode;
  private String description;
  private BigDecimal amount;
  private BigDecimal minimumAmount;
  private BigDecimal maximumAmount;
  private BigDecimal minimumDiscountAmount;
  private DiscountType discountType;
  private Boolean active;
}
