package com.sbpl.OPD.controller;

import com.sbpl.OPD.enums.PaymentType;
import com.sbpl.OPD.service.PaymentBillService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;


@RestController
public class PaymentBillController {

  @Autowired
  private PaymentBillService paymentBillService;

  @PostMapping("/create/payment")
  public ResponseEntity<?> paymentBill(@RequestParam Long billId,
                                       @RequestParam BigDecimal totalPayment,
                                       @RequestParam BigDecimal paidAmount,
                                       @RequestParam PaymentType paymentType) {
    return paymentBillService.paymentBill(billId, totalPayment, paidAmount, paymentType);
  }

  @GetMapping("/get/allbill/by-billId")
  public ResponseEntity<?> getAllBillByOriginalBillId(@RequestParam Long billId) {
    return paymentBillService.getAllBillByOriginalBillId(billId);
  }

  @GetMapping("/get/all-patient-bill")
  public ResponseEntity<?> getAllPatientBills(@RequestParam Long patientId) {
    return paymentBillService.getAllPatientBills(patientId);
  }


}
