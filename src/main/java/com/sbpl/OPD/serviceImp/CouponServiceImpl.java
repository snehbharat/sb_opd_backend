package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.coupon.CouponCreateDTO;
import com.sbpl.OPD.dto.coupon.CouponResponseDTO;
import com.sbpl.OPD.model.CouponDetails;
import com.sbpl.OPD.repository.CouponRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.CouponService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CouponServiceImpl implements CouponService {

  @Autowired
  private CouponRepository couponRepository;

  @Autowired
  private BaseResponse baseResponse;

  private static final Logger logger = LoggerFactory.getLogger(CouponServiceImpl.class);


  @Override
  public ResponseEntity<?> createCoupon(CouponCreateDTO dto) {
    try {
      CouponDetails coupon = new CouponDetails();
      coupon.setBranchId(dto.getBranchId());
      coupon.setCouponCode(dto.getCouponCode().toUpperCase());
      coupon.setDescription(dto.getDescription());
      coupon.setAmount(dto.getAmount());
      coupon.setMinimumAmount(dto.getMinimumAmount());
      coupon.setMaximumAmount(dto.getMaximumAmount());
      coupon.setMinimumDiscountAmount(dto.getMinimumDiscountAmount());
      coupon.setDiscountType(dto.getDiscountType());
      coupon.setActive(dto.getActive() != null ? dto.getActive() : true);

      couponRepository.save(coupon);

      return baseResponse.successResponse("Coupon created successfully");

    } catch (Exception e) {
      logger.error("Error creating coupon", e);
      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to create coupon"
      );
    }
  }

  @Override
  public ResponseEntity<?> getAllCoupons() {
    List<CouponResponseDTO> list = couponRepository.findAll()
        .stream()
        .map(this::mapToResponseDTO)
        .toList();

    return baseResponse.successResponse("Coupons fetched successfully", list);
  }

  @Override
  public ResponseEntity<?> getCouponById(Long couponId) {
    try {
      CouponDetails coupon = couponRepository.findByIdAndActiveTrue(couponId)
          .orElseThrow(() -> new IllegalArgumentException("Coupon not found"));

      return baseResponse.successResponse(
          "Coupon fetched successfully",
          mapToResponseDTO(coupon)
      );

    } catch (IllegalArgumentException e) {
      return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
    }
  }

  @Override
  public ResponseEntity<?> searchCoupons(Long branchId, Boolean active, String couponCode) {
    List<CouponResponseDTO> list = couponRepository
        .searchCoupons(branchId, active, couponCode)
        .stream()
        .map(this::mapToResponseDTO)
        .toList();

    return baseResponse.successResponse("Coupons fetched successfully", list);
  }

  private CouponResponseDTO mapToResponseDTO(CouponDetails coupon) {
    CouponResponseDTO dto = new CouponResponseDTO();
    dto.setId(coupon.getId());
    dto.setBranchId(coupon.getBranchId());
    dto.setCouponCode(coupon.getCouponCode());
    dto.setDescription(coupon.getDescription());
    dto.setAmount(coupon.getAmount());
    dto.setMinimumAmount(coupon.getMinimumAmount());
    dto.setMaximumAmount(coupon.getMaximumAmount());
    dto.setMinimumDiscountAmount(coupon.getMinimumDiscountAmount());
    dto.setDiscountType(coupon.getDiscountType());
    dto.setActive(coupon.getActive());
    return dto;
  }

}
