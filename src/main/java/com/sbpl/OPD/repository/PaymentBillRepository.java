package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.PaymentBill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PaymentBillRepository extends JpaRepository<PaymentBill, Long> {

  @Query("""
          SELECT p
          FROM PaymentBill p
          WHERE p.originalBillId = :billId
          ORDER BY p.createdAt DESC
      """)
  List<PaymentBill> findByOriginalBillId(Long billId);

}
