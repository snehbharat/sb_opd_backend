package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.dto.BillDTO;
import com.sbpl.OPD.dto.BillItemDTO;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.enums.PaymentType;
import com.sbpl.OPD.model.Bill;
import com.sbpl.OPD.model.BillItem;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.PaymentBill;
import com.sbpl.OPD.repository.BillItemRepository;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.PaymentBillRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.PaymentBillService;
import com.sbpl.OPD.utils.DbUtill;
import jakarta.transaction.Transactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class PaymentBillServiceImpl implements PaymentBillService {

  @Autowired
  private PaymentBillRepository paymentBillRepository;

  @Autowired
  private BillRepository billRepository;

  @Autowired
  private CustomerRepository customerRepository;

  @Autowired
  private BaseResponse baseResponse;

  @Autowired
  private BillItemRepository billItemRepository;

  private static final Logger logger = LoggerFactory.getLogger(PaymentBillServiceImpl.class);

  @Override
  @Transactional
  public ResponseEntity<?> paymentBill(
      Long billId,
      BigDecimal totalPayment,
      BigDecimal paidAmount,
      PaymentType paymentType
  ) {
    try {

      if (billId == null) {
        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Bill ID is required"
        );
      }

      Bill bill = billRepository.findById(billId)
          .orElseThrow(() ->
              new RuntimeException("Bill not found")
          );

      BigDecimal billTotal = bill.getTotalAmount();

      if (totalPayment == null || paidAmount == null) {
        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Payment amounts are required"
        );
      }

      if (paidAmount.compareTo(totalPayment) != 0) {
        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Paid amount must equal total payment"
        );
      }

      if (totalPayment.compareTo(billTotal) > 0) {
        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Payment exceeds bill total"
        );
      }

      BigDecimal newPaid =
          bill.getPaidAmount().add(paidAmount);

      BigDecimal newBalance =
          billTotal.subtract(newPaid);

      bill.setPaidAmount(newPaid);
      bill.setBalanceAmount(newBalance);

      if (newBalance.compareTo(BigDecimal.ZERO) == 0) {

        bill.setStatus(BillStatus.PAID);

      } else {

        bill.setStatus(BillStatus.PARTIALLY_PAID);

      }

      bill = billRepository.save(bill);

      PaymentBill paymentBill = new PaymentBill();
      paymentBill.setOriginalBillId(billId);
      paymentBill.setTotalAmount(totalPayment);
      paymentBill.setPaidAmount(paidAmount);
      paymentBill.setPaymentType(paymentType);
      paymentBillRepository.save(paymentBill);

      this.updatePatientBillAmount(bill.getPatient(), bill);

      return baseResponse.successResponse(
          "Payment recorded successfully"
      );

    } catch (Exception e) {

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to process payment"
      );
    }
  }

  private void updatePatientBillAmount(
      Customer patient,
      Bill savedBill
  ) {

    if (patient == null || savedBill == null) {
      return;
    }

    BigDecimal currentTotalBill =
        Optional.ofNullable(patient.getTotalBillAmount())
            .orElse(BigDecimal.ZERO);

    BigDecimal currentTotalPaid =
        Optional.ofNullable(patient.getTotalPaidAmount())
            .orElse(BigDecimal.ZERO);

    BigDecimal previousDue =
        Optional.ofNullable(patient.getTotalDueAmount())
            .orElse(BigDecimal.ZERO);

    BigDecimal billTotal =
        Optional.ofNullable(savedBill.getTotalAmount())
            .orElse(BigDecimal.ZERO);

    BigDecimal paidNow =
        Optional.ofNullable(savedBill.getPaidAmount())
            .orElse(BigDecimal.ZERO);

    /*
        total = previousDue + newService
        so we remove previous due
    */

    BigDecimal newServiceAmount =
        billTotal.subtract(previousDue);

    if (newServiceAmount.compareTo(BigDecimal.ZERO) < 0) {

      newServiceAmount = BigDecimal.ZERO;

    }

    BigDecimal updatedTotalBill =
        currentTotalBill.add(newServiceAmount);

    BigDecimal updatedTotalPaid =
        currentTotalPaid.add(paidNow);

    BigDecimal updatedTotalDue =
        updatedTotalBill.subtract(updatedTotalPaid);

    patient.setTotalBillAmount(updatedTotalBill);
    patient.setTotalPaidAmount(updatedTotalPaid);
    patient.setTotalDueAmount(updatedTotalDue);

    customerRepository.save(patient);
  }

  @Override
  public ResponseEntity<?> getAllBillByOriginalBillId(Long billId) {

    logger.info("Fetching payment bills for originalBillId={}", billId);

    try {

      if (billId == null) {

        logger.warn("Invalid request: originalBillId is null");

        return baseResponse.errorResponse(
            HttpStatus.BAD_REQUEST,
            "Original bill ID is required"
        );
      }

      List<PaymentBill> paymentBillList =
          paymentBillRepository.findByOriginalBillId(billId);

      if (paymentBillList == null || paymentBillList.isEmpty()) {

        logger.info(
            "No payment records found for originalBillId={}",
            billId
        );

        return baseResponse.successResponse(
            "No payment records found",
            Collections.emptyList()
        );
      }

      logger.info(
          "Successfully fetched {} payment records for originalBillId={}",
          paymentBillList.size(),
          billId
      );

      return baseResponse.successResponse(
          "Payment records fetched successfully",
          paymentBillList
      );

    } catch (Exception e) {

      logger.error(
          "Error while fetching payment bills for originalBillId={}",
          billId,
          e
      );

      return baseResponse.errorResponse(
          HttpStatus.INTERNAL_SERVER_ERROR,
          "Failed to fetch payment records"
      );
    }
  }

  @Override
  public ResponseEntity<?> getAllPatientBills(Long patientId) {

    long startTime = System.currentTimeMillis();

    try {
      if (patientId == null) {
        logger.warn("getAllPatientBills called with null patientId");
        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Patient ID is required");
      }

      logger.info("Fetching bills for patientId={}", patientId);

      Pageable pageable = DbUtill.buildPageRequestWithoutSort(0, 20);

      Page<Bill> billPage = billRepository.findByPatientIdOrderByCreatedAtMsDesc(patientId, pageable);

      if (billPage.isEmpty()) {
        logger.info("No bills found for patientId={}", patientId);
      }

      // Convert to DTO (optimized stream)
      List<BillDTO> dtoList = billPage.getContent()
          .stream()
          .map(this::convertToDTO)
          .toList();

      Map<String, Object> response = DbUtill.buildPaginatedResponse(billPage, dtoList);

      long timeTaken = System.currentTimeMillis() - startTime;
      logger.info("Fetched {} bills for patientId={} in {} ms",
          dtoList.size(), patientId, timeTaken);

      return baseResponse.successResponse("Bills fetched successfully", response);

    } catch (Exception e) {
      logger.error("Error fetching bills for patientId={}", patientId, e);
      return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to fetch bills");
    }
  }

  private BillItemDTO convertItemToDTO(BillItem item) {
    BillItemDTO dto = new BillItemDTO();
    dto.setId(item.getId());
    dto.setBillId(item.getBill().getId());
    dto.setItemName(item.getItemName());
    dto.setItemDescription(item.getItemDescription());
    dto.setTreatmentPackageId(item.getTreatmentPackageId());
    dto.setQuantity(item.getQuantity());
    dto.setUnitPrice(item.getUnitPrice());
    dto.setTotalPrice(item.getTotalPrice());
    return dto;
  }

  private BillDTO convertToDTO(Bill bill) {
    BillDTO dto = new BillDTO();
    dto.setId(bill.getId());
    dto.setPatientId(bill.getPatient().getId());
    dto.setPatientBillAmount(bill.getPatient().getTotalBillAmount());
    dto.setPatientPaidAmount(bill.getPatient().getTotalPaidAmount());
    dto.setPatientDueAmount(bill.getPatient().getTotalDueAmount());
    dto.setBillingStaffId(bill.getBillingStaff().getId());
    if (bill.getAppointment() != null) {
      dto.setAppointmentId(bill.getAppointment().getId());
    }
    dto.setStatus(bill.getStatus());
    dto.setTotalAmount(bill.getTotalAmount());
    dto.setPaidAmount(bill.getPaidAmount());
    dto.setBalanceAmount(bill.getBalanceAmount());
    dto.setPreviousDueAmount(bill.getPreviousDue());
    dto.setCreatedAt(bill.getCreatedAt());
    dto.setUpdatedAt(bill.getUpdatedAt());
    dto.setPaymentDate(bill.getPaymentDate());
    dto.setPaymentType(
        bill.getPaymentType() != null
            ? bill.getPaymentType().name()
            : null
    );
    dto.setNotes(bill.getNotes());
    dto.setBillNumber(bill.getBillNumber());

    // Set names for display
    dto.setPatientName(bill.getPatient().getFirstName() + " " + bill.getPatient().getLastName());
    dto.setBillingStaffName(bill.getBillingStaff().getFirstName() + " " + bill.getBillingStaff().getLastName());

    // Set company and branch information
    if (bill.getCompany() != null) {
      dto.setCompanyId(bill.getCompany().getId());
      dto.setCompanyName(bill.getCompany().getCompanyName());
    }
    if (bill.getBranch() != null) {
      dto.setBranchId(bill.getBranch().getId());
      dto.setBranchName(bill.getBranch().getBranchName());
    }

    dto.setCouponCode(bill.getCouponCode());
    dto.setCouponAmount(bill.getCouponAmount());

    // Get bill items
    List<BillItemDTO> itemDTOs =
        billItemRepository.findByBillId(bill.getId())
            .stream()
            .map(this::convertItemToDTO)
            .toList();
    dto.setBillItems(itemDTOs);

    return dto;
  }

}
