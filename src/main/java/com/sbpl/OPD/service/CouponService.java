package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.coupon.CouponCreateDTO;
import org.springframework.http.ResponseEntity;

public interface CouponService {

  public ResponseEntity<?> createCoupon(CouponCreateDTO dto);

  public ResponseEntity<?> getAllCoupons();

  public ResponseEntity<?> getCouponById(Long couponId);

  public ResponseEntity<?> searchCoupons(Long branchId, Boolean active, String couponCode);
}
