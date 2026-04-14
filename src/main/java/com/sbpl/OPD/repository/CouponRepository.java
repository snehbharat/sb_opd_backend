package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.CouponDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CouponRepository extends JpaRepository<CouponDetails, Long> {

  Optional<CouponDetails> findByIdAndActiveTrue(Long id);

  Optional<CouponDetails> findByCouponCodeIgnoreCaseAndBranchIdAndActiveTrue(
      String couponCode,
      Long branchId
  );

  @Query("""
        SELECT c FROM CouponDetails c
        WHERE (:branchId IS NULL OR c.branchId = :branchId)
        AND (:active IS NULL OR c.active = :active)
        AND (:couponCode IS NULL OR LOWER(c.couponCode) LIKE LOWER(CONCAT('%', :couponCode, '%')))
    """)
  List<CouponDetails> searchCoupons(
      Long branchId,
      Boolean active,
      String couponCode
  );
}
