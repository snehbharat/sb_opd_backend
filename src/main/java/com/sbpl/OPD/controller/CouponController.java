package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.coupon.CouponCreateDTO;
import com.sbpl.OPD.service.CouponService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/coupons")
public class CouponController {

  @Autowired
  private CouponService couponService;

  @PostMapping("/create")
  public ResponseEntity<?> createCoupon(@RequestBody CouponCreateDTO dto) {
    return couponService.createCoupon(dto);
  }

  @GetMapping("/get/all")
  public ResponseEntity<?> getAllCoupons() {
    return couponService.getAllCoupons();
  }

  @GetMapping("/{id}")
  public ResponseEntity<?> getCouponById(@PathVariable Long id) {
    return couponService.getCouponById(id);
  }

  @GetMapping("/search")
  public ResponseEntity<?> searchCoupons(
      @RequestParam(required = false) Long branchId,
      @RequestParam(required = false) Boolean active,
      @RequestParam(required = false) String couponCode
  ) {
    return couponService.searchCoupons(branchId, active, couponCode);
  }
}
