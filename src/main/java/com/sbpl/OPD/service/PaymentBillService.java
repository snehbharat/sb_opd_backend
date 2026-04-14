package com.sbpl.OPD.service;

import com.sbpl.OPD.enums.PaymentType;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;

public interface PaymentBillService {

  public ResponseEntity<?> paymentBill(Long billId, BigDecimal totalPayment,
                                       BigDecimal paidAmount, PaymentType paymentType);

  public ResponseEntity<?> getAllBillByOriginalBillId(Long billId);

  public ResponseEntity<?> getAllPatientBills(Long patientId);

}
