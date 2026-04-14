package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.BillDTO;
import com.sbpl.OPD.dto.BillItemDTO;
import com.sbpl.OPD.dto.BillingOverviewSummaryDTO;
import com.sbpl.OPD.dto.StaffBillingReportDTO;
import com.sbpl.OPD.dto.StaffBillingStatsDTO;
import com.sbpl.OPD.dto.StaffBillingSummaryDTO;
import com.sbpl.OPD.dto.branch.PaymentTypeDTO;
import com.sbpl.OPD.dto.repository.BillStatsProjection;
import com.sbpl.OPD.dto.repository.DateRange;
import com.sbpl.OPD.dto.treatment.pkg.PackageUsageInfoDTO;
import com.sbpl.OPD.dto.treatment.pkg.PatientBillWithPackageDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.enums.PaymentType;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Bill;
import com.sbpl.OPD.model.BillItem;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.model.PatientPackageUsage;
import com.sbpl.OPD.model.PaymentBill;
import com.sbpl.OPD.model.TreatmentPackage;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BillItemRepository;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.PatientPackageUsageRepository;
import com.sbpl.OPD.repository.PaymentBillRepository;
import com.sbpl.OPD.repository.TreatmentPackageRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.BillNumberService;
import com.sbpl.OPD.service.BillService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.ErrorUtils;
import com.sbpl.OPD.utils.RbacUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.Period;
import java.time.ZoneId;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Implementation of billing service logic.
 * <p>
 * Handles bill calculation, item aggregation,
 * payment processing, and balance updates.
 * <p>
 * Ensures billing consistency and financial accuracy.
 *
 * @author Rahul Kumar
 */


@Service
public class BillServiceImpl implements BillService {


    private static final Logger logger = LoggerFactory.getLogger(BillServiceImpl.class);

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private BillItemRepository billItemRepository;

    @Autowired
    private PaymentBillRepository paymentBillRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private RbacUtil rbacUtil;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;
    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private BillNumberService billNumberService;

    @Autowired
    private PatientPackageUsageRepository patientPackageUsageRepository;

    @Autowired
    private TreatmentPackageRepository treatmentPackageRepository;

    @Override
    public ResponseEntity<?> createBill(BillDTO billDTO) {

        logger.info("Create bill request received [patientId={}, billingStaffId={}]",
            billDTO.getPatientId(), billDTO.getBillingStaffId());

        Long userLoggedInId = DbUtill.getLoggedInUserId();


        try {

            if (billDTO.getPatientId() == null || userLoggedInId == null) {
                ErrorUtils.throwValidation("PatientId and BillingStaffId are required");
            }

            Customer patient = customerRepository.findById(billDTO.getPatientId())
                .orElseThrow();

            User billingStaff = userRepository.findById(userLoggedInId)
                .orElseThrow();

            Bill bill = new Bill();
            bill.setPatient(patient);
            bill.setBillingStaff(billingStaff);
            BigDecimal previousDue = patient.getTotalDueAmount() != null
                ? patient.getTotalDueAmount()
                : BigDecimal.ZERO;

            BigDecimal paid = billDTO.getPaidAmount() != null
                ? billDTO.getPaidAmount()
                : BigDecimal.ZERO;

            BigDecimal total = billDTO.getTotalAmount();

            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                return baseResponse.errorResponse(
                    HttpStatus.BAD_REQUEST,
                    "Total amount must be greater than zero"
                );
            }

            Appointment appointment = null;

            if (billDTO.getAppointmentId() != null) {
                appointment = appointmentRepository
                    .findById(billDTO.getAppointmentId())
                    .orElse(null);
                if (appointment == null) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Appointment not found"
                    );
                }

                if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Appointment must be COMPLETED before bill creation. Current status: " + appointment.getStatus()
                    );
                }

                bill.setAppointment(appointment);
            }

            PaymentType paymentType = null;
            if (billDTO.getPaymentType() != null && !billDTO.getPaymentType().isBlank()) {
                try {
                    paymentType = PaymentType.valueOf(billDTO.getPaymentType().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid payment type"
                    );
                }
            }

            if (paid.compareTo(total) == 0) {
                // PAID
                if (paymentType == null) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Payment type is required for paid bills"
                    );
                }
                bill.setStatus(BillStatus.PAID);
                bill.setPaymentType(paymentType);

            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                // PARTIALLY PAID
                if (paymentType == null) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Payment type is required for partially paid bills"
                    );
                }
                bill.setStatus(BillStatus.PARTIALLY_PAID);
                bill.setPaymentType(paymentType);

            } else {
                // PENDING
                bill.setStatus(BillStatus.PENDING);
                bill.setPaymentType(null); // optional, keeps DB clean
            }
            bill.setTotalAmount(total); // includes previous due
            bill.setPaidAmount(paid);
            bill.setBalanceAmount(total.subtract(paid));
            bill.setPreviousDue(previousDue);
            bill.setCreatedAt(new Date());
            bill.setNotes(billDTO.getNotes());

            // Generate bill number
            String billNumber;
            Long billCompanyId = billingStaff.getCompany() != null ? billingStaff.getCompany().getId() : null;
            Long billBranchId;
            if (rbacUtil.isAdmin()) {
                if (appointment != null) {
                    billBranchId = Optional.ofNullable(appointment.getBranch()).map(Branch::getId).orElse(null);
                } else {
                    if (billDTO.getBranchId() != null) {
                        billBranchId = billDTO.getBranchId();
                    } else {
                        return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Staff Admin must provide either an appointment or a branch ID."
                        );
                    }
                }
            } else {
                billBranchId = Optional.ofNullable(billingStaff.getBranch()).map(Branch::getId).orElse(null);
            }


            if (billCompanyId != null || billBranchId != null) {
                billNumber = billNumberService.generateBillNumber(billCompanyId, billBranchId);
            } else {
                billNumber = billNumberService.generateBillNumberForStaff(userLoggedInId);
            }

            bill.setBillNumber(billNumber);

            // Set company and branch information from billing staff
            if (billingStaff.getCompany() != null) {
                bill.setCompany(billingStaff.getCompany());
            }

            assert billBranchId != null;
            branchRepository.findById(billBranchId).ifPresent(bill::setBranch);

            if (billDTO.getCouponCode() != null) {
                bill.setCouponCode(billDTO.getCouponCode());
                bill.setCouponAmount(billDTO.getCouponAmount());
            }

            Bill savedBill = billRepository.save(bill);

            PaymentBill paymentBill = new PaymentBill();
            paymentBill.setOriginalBillId(savedBill.getId());
            paymentBill.setTotalAmount(savedBill.getTotalAmount());
            paymentBill.setPaidAmount(savedBill.getPaidAmount());
            paymentBill.setPaymentType(savedBill.getPaymentType());
            paymentBillRepository.save(paymentBill);

            saveBillItems(savedBill, billDTO.getBillItems());

            this.updatePatientBillAmount(patient, savedBill);

            logger.info("Bill created successfully [billId={}]", savedBill.getId());

            return baseResponse.successResponse(
                "Bill created successfully"
            );

        } catch (Exception e) {
            logger.error("Error while creating bill", e);
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to create bill. Please try again later."
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> createBillNew(BillDTO billDTO) {

        logger.info("Create bill request received [patientId={}, billingStaffId={}]",
                billDTO.getPatientId(), billDTO.getBillingStaffId());

        Long userLoggedInId = DbUtill.getLoggedInUserId();


        try {

            if (billDTO.getPatientId() == null || userLoggedInId == null) {
                ErrorUtils.throwValidation("PatientId and BillingStaffId are required");
            }

            Customer patient = customerRepository.findById(billDTO.getPatientId())
                    .orElseThrow();

            User billingStaff = userRepository.findById(userLoggedInId)
                    .orElseThrow();

            Bill bill = new Bill();
            bill.setPatient(patient);
            bill.setBillingStaff(billingStaff);
            BigDecimal previousDue = patient.getTotalDueAmount() != null
                    ? patient.getTotalDueAmount()
                    : BigDecimal.ZERO;

            BigDecimal paid = billDTO.getPaidAmount() != null
                    ? billDTO.getPaidAmount()
                    : BigDecimal.ZERO;

            BigDecimal total = billDTO.getTotalAmount();

            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Total amount must be greater than zero"
                );
            }

            Appointment appointment = null;

            if (billDTO.getAppointmentId() != null) {
                appointment = appointmentRepository
                        .findById(billDTO.getAppointmentId())
                        .orElse(null);
                if (appointment == null) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Appointment not found"
                    );
                }

                if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Appointment must be COMPLETED before bill creation. Current status: " + appointment.getStatus()
                    );
                }

                bill.setAppointment(appointment);
            }

            Long treatmentPackageIdFromBillItems;
            if (billDTO.getBillItems() != null && !billDTO.getBillItems().isEmpty()) {
                treatmentPackageIdFromBillItems = billDTO.getBillItems().get(0).getTreatmentPackageId();
            } else {
                treatmentPackageIdFromBillItems = null;
            }

            if (treatmentPackageIdFromBillItems != null) {
                logger.info("Checking package usage for patient {} and package {}",
                        billDTO.getPatientId(), treatmentPackageIdFromBillItems);

                Optional<PatientPackageUsage> packageUsageOpt = patientPackageUsageRepository
                        .findActiveUsageByPatientAndPackage(billDTO.getPatientId(), treatmentPackageIdFromBillItems);

                PatientPackageUsage usage;

                if (packageUsageOpt.isPresent()) {
                    usage = packageUsageOpt.get();

                    logger.info("Found existing active package usage: sessionsUsed={}, sessionsRemaining={}, completed={}",
                            usage.getSessionsUsed(), usage.getSessionsRemaining(), usage.getCompleted());

                    if (usage.getCompleted()) {
                        return baseResponse.errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "This treatment package has been fully utilized. All sessions completed."
                        );
                    }

                    if (usage.getSessionsRemaining() <= 0) {
                        return baseResponse.errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "No remaining sessions in this package. Package completed."
                        );
                    }

                    usage.setSessionsUsed(usage.getSessionsUsed() + 1);
                    usage.setSessionsRemaining(usage.getSessionsRemaining() - 1);
                    usage.setLastSessionDate(new Date());

                    if (usage.getSessionsRemaining() <= 0) {
                        usage.setCompleted(true);
                        usage.setActive(false);
                        logger.info("Package completed for patient {} - all sessions used", billDTO.getPatientId());
                    }

                    logger.info("Using existing package [patientId={}, packageId={}, sessionsUsed={}, sessionsRemaining={}]",
                            billDTO.getPatientId(), treatmentPackageIdFromBillItems,
                            usage.getSessionsUsed(), usage.getSessionsRemaining());

                } else {
                    logger.info("No existing package usage found. Creating new package usage for patient {} and package {}",
                            billDTO.getPatientId(), treatmentPackageIdFromBillItems);

                    TreatmentPackage treatmentPackage = treatmentPackageRepository.findById(treatmentPackageIdFromBillItems)
                            .orElseThrow(() -> new IllegalArgumentException("Treatment package not found with ID: " + treatmentPackageIdFromBillItems));

                    usage = new PatientPackageUsage();
                    usage.setPatient(patient);
                    usage.setTreatmentPackage(treatmentPackage);
                    usage.setTreatment(treatmentPackage.getTreatment());
                    usage.setTotalSessions(treatmentPackage.getSessions());
                    usage.setSessionsUsed(1);
                    usage.setSessionsRemaining(treatmentPackage.getSessions() - 1);
                    usage.setPackagePricePaid(treatmentPackage.getTotalPrice());
                    usage.setPurchaseDate(new Date());
                    usage.setLastSessionDate(new Date());
                    usage.setBranchId(treatmentPackage.getBranchId());
                    usage.setClinicId(treatmentPackage.getClinicId());
                    usage.setActive(true);
                    usage.setCompleted(false);

                    if (usage.getSessionsRemaining() <= 0) {
                        usage.setCompleted(true);
                        usage.setActive(false);
                        logger.info("Package completed immediately - single session package");
                    }

                    logger.info("Created new package usage [patientId={}, packageId={}, totalSessions={}, sessionsRemaining={}]",
                            billDTO.getPatientId(), treatmentPackageIdFromBillItems,
                            usage.getTotalSessions(), usage.getSessionsRemaining());
                }

                patientPackageUsageRepository.save(usage);
            }
            PaymentType paymentType = null;
            if (billDTO.getPaymentType() != null && !billDTO.getPaymentType().isBlank()) {
                try {
                    paymentType = PaymentType.valueOf(billDTO.getPaymentType().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Invalid payment type"
                    );
                }
            }

            if (paid.compareTo(total) == 0) {
                // PAID
                if (paymentType == null) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Payment type is required for paid bills"
                    );
                }
                bill.setStatus(BillStatus.PAID);
                bill.setPaymentType(paymentType);

            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                // PARTIALLY PAID
                if (paymentType == null) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Payment type is required for partially paid bills"
                    );
                }
                bill.setStatus(BillStatus.PARTIALLY_PAID);
                bill.setPaymentType(paymentType);

            } else {
                // PENDING
                bill.setStatus(BillStatus.PENDING);
                bill.setPaymentType(null); // optional, keeps DB clean
            }
            bill.setTotalAmount(total); // includes previous due
            bill.setPaidAmount(paid);
            bill.setBalanceAmount(total.subtract(paid));
            bill.setPreviousDue(previousDue);
            bill.setCreatedAt(new Date());
            bill.setNotes(billDTO.getNotes());

            // Generate bill number
            String billNumber;
            Long billCompanyId = billingStaff.getCompany() != null ? billingStaff.getCompany().getId() : null;
            Long billBranchId;
            if (rbacUtil.isAdmin()) {
                if (appointment != null) {
                    billBranchId = Optional.ofNullable(appointment.getBranch()).map(Branch::getId).orElse(null);
                } else {
                    if (billDTO.getBranchId() != null) {
                        billBranchId = billDTO.getBranchId();
                    } else {
                        return baseResponse.errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "Staff Admin must provide either an appointment or a branch ID."
                        );
                    }
                }
            } else {
                billBranchId = Optional.ofNullable(billingStaff.getBranch()).map(Branch::getId).orElse(null);
            }


            if (billCompanyId != null || billBranchId != null) {
                billNumber = billNumberService.generateBillNumber(billCompanyId, billBranchId);
            } else {
                billNumber = billNumberService.generateBillNumberForStaff(userLoggedInId);
            }

            bill.setBillNumber(billNumber);

            // Set company and branch information from billing staff
            if (billingStaff.getCompany() != null) {
                bill.setCompany(billingStaff.getCompany());
            }

            assert billBranchId != null;
            branchRepository.findById(billBranchId).ifPresent(bill::setBranch);

            if (billDTO.getCouponCode() != null) {
                bill.setCouponCode(billDTO.getCouponCode());
                bill.setCouponAmount(billDTO.getCouponAmount());
            }

            Bill savedBill = billRepository.save(bill);

            PaymentBill paymentBill = new PaymentBill();
            paymentBill.setOriginalBillId(savedBill.getId());
            paymentBill.setTotalAmount(savedBill.getTotalAmount());
            paymentBill.setPaidAmount(savedBill.getPaidAmount());
            paymentBill.setPaymentType(savedBill.getPaymentType());
            paymentBillRepository.save(paymentBill);

            saveBillItems(savedBill, billDTO.getBillItems());

            this.updatePatientBillAmount(patient, savedBill);

            logger.info("Bill created successfully [billId={}]", savedBill.getId());

            return baseResponse.successResponse(
                    "Bill created successfully"
            );

        } catch (Exception e) {
            logger.error("Error while creating bill", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create bill. Please try again later."
            );
        }
    }

    /**
     * Update The Patient Bill Amount.
     *
     * @param patient   @{@link Customer}
     * @param savedBill @{@link Bill}
     */
    private void updatePatientBillAmount(Customer patient, Bill savedBill) {

        // If patient totals are empty, load from DB
        if (patient.getTotalBillAmount() == null || patient.getTotalBillAmount().compareTo(BigDecimal.ZERO) == 0) {
            Object[] result = billRepository.getBillingSummaryByPatientId(patient.getId());

            if (result != null && result.length >= 3) {
                patient.setTotalBillAmount(result[0] != null ? new BigDecimal(result[0].toString()) : BigDecimal.ZERO);
                patient.setTotalPaidAmount(result[1] != null ? new BigDecimal(result[1].toString()) : BigDecimal.ZERO);
                patient.setTotalDueAmount(result[2] != null ? new BigDecimal(result[2].toString()) : BigDecimal.ZERO);
            }
        }

        // Current totals
        BigDecimal currentTotalBill = patient.getTotalBillAmount() != null ? patient.getTotalBillAmount() : BigDecimal.ZERO;
        BigDecimal currentTotalPaid = patient.getTotalPaidAmount() != null ? patient.getTotalPaidAmount() : BigDecimal.ZERO;
        BigDecimal previousDue = patient.getTotalDueAmount() != null ? patient.getTotalDueAmount() : BigDecimal.ZERO;

        // Bill values
        BigDecimal newBillAmount = savedBill.getTotalAmount() != null ? savedBill.getTotalAmount() : BigDecimal.ZERO;
        BigDecimal amountPaidNow = savedBill.getPaidAmount() != null ? savedBill.getPaidAmount() : BigDecimal.ZERO;

        // Remove previous due from bill total to get actual new service amount
        BigDecimal newServiceAmount = newBillAmount.subtract(previousDue);

        if (newServiceAmount.compareTo(BigDecimal.ZERO) < 0) {
            newServiceAmount = BigDecimal.ZERO;
        }

        // Updated totals
        BigDecimal updatedTotalBill = currentTotalBill.add(newServiceAmount);
        BigDecimal updatedTotalPaid = currentTotalPaid.add(amountPaidNow);
        BigDecimal updatedTotalDue = updatedTotalBill.subtract(updatedTotalPaid);

        // Update patient
        patient.setTotalBillAmount(updatedTotalBill);
        patient.setTotalPaidAmount(updatedTotalPaid);
        patient.setTotalDueAmount(updatedTotalDue);

        customerRepository.save(patient);
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateBill(Long billId, BillDTO billDTO) {

        logger.info("Full update bill request received [billId={}]", billId);

        Long userLoggedInId = DbUtill.getLoggedInUserId();

        try {
            if (billId == null || userLoggedInId == null) {
                ErrorUtils.throwValidation("BillId and Logged-in User are required");
            }

            if (billDTO.getPatientId() == null) {
                ErrorUtils.throwValidation("PatientId is required");
            }

            if (billDTO.getTotalAmount() == null) {
                ErrorUtils.throwValidation("Total amount is required");
            }

            // -------------------- FETCH BILL --------------------
            Bill bill = getBillOrThrow(billId);

            if (bill.getStatus() == BillStatus.PAID) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Paid bill cannot be modified"
                );
            }

            // -------------------- PATIENT --------------------
            Customer patient = customerRepository
                    .findById(billDTO.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            bill.setPatient(patient);

            // -------------------- BILLING STAFF --------------------
            User billingStaff = userRepository
                    .findById(userLoggedInId)
                    .orElseThrow(() -> new IllegalArgumentException("Billing staff not found"));

            bill.setBillingStaff(billingStaff);

            if (billingStaff.getCompany() != null) {
                bill.setCompany(billingStaff.getCompany());
            }

            if (billingStaff.getBranch() != null) {
                bill.setBranch(billingStaff.getBranch());
            }

            // -------------------- AMOUNT LOGIC --------------------
            BigDecimal total = billDTO.getTotalAmount();
            BigDecimal paid = billDTO.getPaidAmount() != null
                    ? billDTO.getPaidAmount()
                    : BigDecimal.ZERO;

            if (total.compareTo(BigDecimal.ZERO) <= 0) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Total amount must be greater than zero"
                );
            }

            if (paid.compareTo(BigDecimal.ZERO) < 0) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Paid amount cannot be negative"
                );
            }

            if (paid.compareTo(total) > 0) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Paid amount cannot be greater than total amount"
                );
            }

            BigDecimal balance = total.subtract(paid);

            bill.setTotalAmount(total);
            bill.setPaidAmount(paid);
            bill.setBalanceAmount(balance);

            // -------------------- PAYMENT TYPE (STRING → ENUM) --------------------
            PaymentType paymentType = null;
            if (billDTO.getPaymentType() != null && !billDTO.getPaymentType().isBlank()) {
                try {
                    paymentType = PaymentType.valueOf(billDTO.getPaymentType().toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Invalid payment type"
                    );
                }
            }

            // -------------------- STATUS + PAYMENT VALIDATION --------------------
            if (paid.compareTo(total) == 0) {
                // PAID
                if (paymentType == null) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Payment type is required for paid bills"
                    );
                }
                bill.setStatus(BillStatus.PAID);
                bill.setPaymentType(paymentType);

            } else if (paid.compareTo(BigDecimal.ZERO) > 0) {
                // PARTIALLY PAID
                if (paymentType == null) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Payment type is required for partially paid bills"
                    );
                }
                bill.setStatus(BillStatus.PARTIALLY_PAID);
                bill.setPaymentType(paymentType);

            } else {
                // PENDING
                bill.setStatus(BillStatus.PENDING);
                bill.setPaymentType(null);
            }

            // -------------------- APPOINTMENT --------------------
            if (billDTO.getAppointmentId() != null) {

                Appointment appointment = appointmentRepository
                        .findById(billDTO.getAppointmentId())
                        .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

                if (appointment.getStatus() != AppointmentStatus.COMPLETED) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Appointment must be COMPLETED before bill update. Current status: "
                                    + appointment.getStatus()
                    );
                }

                bill.setAppointment(appointment);

            } else {
                bill.setAppointment(null);
            }

            // -------------------- OTHER FIELDS --------------------
            bill.setNotes(billDTO.getNotes());
            bill.setUpdatedAt(new Date());
            if (billDTO.getCouponCode() != null) {
                bill.setCouponCode(billDTO.getCouponCode());
                bill.setCouponAmount(billDTO.getCouponAmount());
            }

            // -------------------- SAVE BILL --------------------
            Bill updatedBill = billRepository.save(bill);

            // -------------------- BILL ITEMS --------------------
            billItemRepository.deleteByBillId(billId);

            if (billDTO.getBillItems() != null && !billDTO.getBillItems().isEmpty()) {
                saveBillItems(updatedBill, billDTO.getBillItems());
            }

            this.updatePatientBillAmount(patient, updatedBill);

            logger.info("Bill fully updated successfully [billId={}]", billId);

            return baseResponse.successResponse("Bill updated successfully");

        } catch (IllegalArgumentException e) {

            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {

            logger.error("Error while updating bill", e);

            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update bill. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getBillById(Long billId) {
        try {
            return baseResponse.successResponse(
                    "Bill fetched successfully",
                    convertToDTO(getBillOrThrow(billId))
            );
        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getBillsByPatientIdWithPackageDetails(Long patientId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching bills with package details for patient {}", patientId);

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

            Page<Bill> billPage = billRepository.findByPatientId(patientId, pageRequest);

            if (billPage.isEmpty()) {
                return baseResponse.successResponse("No bills found for this patient", new ArrayList<>());
            }

            List<PatientBillWithPackageDTO> result = billPage.getContent().stream()
                    .map(this::mapToPatientBillWithPackageDTO)
                    .collect(Collectors.toList());


            return baseResponse.successResponse("Bills with package details fetched successfully",
                    DbUtill.buildPaginatedResponse(billPage,result));

        } catch (Exception e) {
            logger.error("Error fetching bills with package details for patient {}", patientId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch bills with package details"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllBills(Integer pageNo, Integer pageSize, Long branchId,
                                         String startDate, String endDate) {
        User currentUser = DbUtill.getCurrentUser();

        logger.info(
                "Request received to fetch all bills [pageNo={}, pageSize={}, branchId={}]",
                pageNo, pageSize, branchId
        );

        try {

            Long[] dateRangeInMilli = new Long[2];

            if (startDate != null && !startDate.isBlank() &&
                    endDate != null && !endDate.isBlank()) {
                if (DateUtils.isValidDate(startDate) && DateUtils.isValidDate(endDate)) {
                    dateRangeInMilli = DateUtils.getStartAndEndDateInMilli(startDate, endDate);
                } else {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                            "Date must be in format yyyy-MM-dd");
                }
            } else {
                dateRangeInMilli = DateUtils.getStartAndEndDateInMilli(startDate, endDate);
            }

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            Page<Bill> page;

            // Role-based access control for bill fetching
            if (rbacUtil.isSuperAdmin()) {
                // SUPER_ADMIN and SUPER_ADMIN_MANAGER see all bills across all companies
                page = billRepository.findAllBillsByDateRangeExcludingStatus(
                        BillStatus.DELETED,
                        dateRangeInMilli[0],
                        dateRangeInMilli[1],
                        pageRequest
                );
                logger.info("SUPER_ADMIN accessing all bills system-wide");

            } else if (rbacUtil.isAdmin()) {
                // SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's bills");
                    }
                    page = billRepository.findBillsByBranchAndDateRangeExcludingStatus(
                            branchId,
                            BillStatus.DELETED,
                            dateRangeInMilli[0],
                            dateRangeInMilli[1],
                            pageRequest
                    );
                    logger.info("SAAS_ADMIN accessing bills for branch ID: {}", branchId);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    page = billRepository.findBillsByCompanyAndDateRangeExcludingStatus(
                            companyId,
                            BillStatus.DELETED,
                            dateRangeInMilli[0],
                            dateRangeInMilli[1],
                            pageRequest);
                    logger.info("SAAS_ADMIN accessing bills for company ID: {}", companyId);
                }

            } else {

                if (currentUser.getBranch() != null) {
                    page = billRepository.findBillsByBranchAndDateRangeExcludingStatus(
                            currentUser.getBranch().getId(),
                            BillStatus.DELETED,
                            dateRangeInMilli[0],
                            dateRangeInMilli[1],
                            pageRequest
                    );
                    logger.info("{} accessing bills for their branch ID: {}", currentUser.getRole(), currentUser.getBranch().getId());
                } else {
                    logger.warn("{} user has no branch assignment", currentUser.getRole());
                    page = Page.empty();
                }
            }

            List<BillDTO> dtoList = page.getContent()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} bills on page {}",
                    page.getNumberOfElements(),
                    page.getNumber()
            );

            String successMessage;
            if (rbacUtil.isSuperAdmin()) {
                successMessage = "All bills fetched successfully (system-wide access)";
            } else if (rbacUtil.isAdmin()) {
                if (branchId != null) {
                    successMessage = "Branch bills fetched successfully";
                } else {
                    successMessage = "Company bills fetched successfully";
                }
            } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                successMessage = "Branch bills fetched successfully";
            } else {
                successMessage = "Branch bills fetched successfully (role-restricted access)";
            }

            return baseResponse.successResponse(
                    successMessage,
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error("Error while fetching bills", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch bills at the moment. Please try again later."
            );
        }
    }

    /**
     * Fetch billing statistics within a date range.
     *
     * <p>This API returns aggregated billing information used for the billing dashboard:
     * <ul>
     *     <li>Total number of bills</li>
     *     <li>Total billed amount</li>
     *     <li>Total collected amount</li>
     *     <li>Total balance due</li>
     * </ul>
     *
     * <p>Access Control:</p>
     * <ul>
     *     <li><b>SUPER_ADMIN</b> – Can view statistics across the entire system.</li>
     *     <li><b>SAAS_ADMIN</b> – Can view company-level stats or branch-level stats.</li>
     *     <li><b>Branch Users</b> – Can only view statistics for their assigned branch.</li>
     * </ul>
     *
     * <p>Date Filtering:</p>
     * <ul>
     *     <li>If {@code startDate} and {@code endDate} are provided, stats are filtered by that range.</li>
     *     <li>If not provided, the current day's range (IST) will be used.</li>
     * </ul>
     *
     * <p>Deleted bills are automatically excluded.</p>
     *
     * @param branchId  optional branch ID for filtering branch-level statistics
     * @param startDate start date in {@code yyyy-MM-dd} format
     * @param endDate   end date in {@code yyyy-MM-dd} format
     * @return aggregated billing statistics
     */
    @Override
    public ResponseEntity<?> getAllBillsStats(Long branchId, String startDate, String endDate) {

        User currentUser = DbUtill.getCurrentUser();

        logger.info(
                "Request received to fetch bill statistics [branchId={}, startDate={}, endDate={}]",
                branchId, startDate, endDate
        );

        try {

            Long[] dateRangeInMilli;

            if (startDate != null && !startDate.isBlank() &&
                    endDate != null && !endDate.isBlank()) {

                if (!DateUtils.isValidDate(startDate) || !DateUtils.isValidDate(endDate)) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Date must be in format yyyy-MM-dd"
                    );
                }
            }

            dateRangeInMilli = DateUtils.getStartAndEndDateInMilli(startDate, endDate);

            BillStatsProjection stats;

            /*
             * ===============================
             * SUPER ADMIN ACCESS
             * ===============================
             */
            if (rbacUtil.isSuperAdmin()) {

                stats = billRepository.getSystemBillStats(
                        BillStatus.DELETED,
                        dateRangeInMilli[0],
                        dateRangeInMilli[1]
                );

                logger.info("SUPER_ADMIN accessing system-wide billing statistics");

            }
            /*
             * ===============================
             * SAAS ADMIN ACCESS
             * ===============================
             */
            else if (rbacUtil.isAdmin()) {

                if (branchId != null) {

                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Branch not found with id: " + branchId));

                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(
                                HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's bills"
                        );
                    }

                    stats = billRepository.getBranchBillStats(
                            branchId,
                            BillStatus.DELETED,
                            dateRangeInMilli[0],
                            dateRangeInMilli[1]
                    );

                    logger.info("SAAS_ADMIN accessing billing stats for branch ID: {}", branchId);

                } else {

                    Long companyId = DbUtill.getLoggedInCompanyId();

                    stats = billRepository.getCompanyBillStats(
                            companyId,
                            BillStatus.DELETED,
                            dateRangeInMilli[0],
                            dateRangeInMilli[1]
                    );

                    logger.info("SAAS_ADMIN accessing billing stats for company ID: {}", companyId);
                }

            }
            /*
             * ===============================
             * BRANCH USER ACCESS
             * ===============================
             */
            else {

                if (currentUser.getBranch() == null) {

                    logger.warn("{} user has no branch assignment", currentUser.getRole());

                    return baseResponse.errorResponse(
                            HttpStatus.FORBIDDEN,
                            "User is not assigned to any branch"
                    );
                }

                stats = billRepository.getBranchBillStats(
                        currentUser.getBranch().getId(),
                        BillStatus.DELETED,
                        dateRangeInMilli[0],
                        dateRangeInMilli[1]
                );

                logger.info("{} accessing billing stats for branch ID: {}",
                        currentUser.getRole(),
                        currentUser.getBranch().getId());
            }

            Map<String, Object> response = new HashMap<>();

            response.put("totalBills", stats.getTotalBills());
            response.put("totalAmount", stats.getTotalAmount());
            response.put("collectedAmount", stats.getCollectedAmount());
            response.put("dueAmountToday", stats.getDueAmountToday());
            response.put("balanceAmount", stats.getBalanceAmount());

            logger.info("Billing statistics fetched successfully");

            return baseResponse.successResponse(
                    "Billing statistics fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {

            logger.warn("Validation failed | {}", e.getMessage());

            return baseResponse.errorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {

            logger.error("Error while fetching billing statistics", e);

            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch billing statistics at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> getStaffBillingStats(String dateFilter, String startDate, String endDate) {

        Long staffId = DbUtill.getLoggedInUserId();
        logger.info("Request received to fetch billing stats for staffId={} with date filter [dateFilter={}, startDate={}, endDate={}]",
                staffId, dateFilter, startDate, endDate);

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            LocalDateTime startDateTime;
            LocalDateTime endDateTime;

            if (dateFilter == null || dateFilter.equalsIgnoreCase("today")) {
                LocalDateTime[] range = DateUtils.getBusinessDayRange();
                startDateTime = range[0];
                endDateTime = range[1];
            } else {
                switch (dateFilter.toLowerCase()) {
                    case "weekly":
                    case "week":
                        LocalDateTime[] weekRange = DateUtils.getBusinessWeekRange();
                        startDateTime = weekRange[0];
                        endDateTime = weekRange[1];
                        break;
                    case "monthly":
                    case "month":
                        LocalDate today = DateUtils.getBusinessLocalDate();
                        LocalDate firstDay = today.withDayOfMonth(1);
                        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());
                        startDateTime = DateUtils.getStartOfBusinessDay(firstDay);
                        endDateTime = DateUtils.getEndOfBusinessDay(lastDay);
                        break;
                    case "custom":
                    case "range":
                        if (startDate == null || endDate == null) {
                            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Start date and end date are required for custom range");
                        }
                        if (!DateUtils.isValidDate(startDate) || !DateUtils.isValidDate(endDate)) {
                            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Date must be in format yyyy-MM-dd");
                        }
                        LocalDateTime[] customRange = DateUtils.getStartAndEndDateTime(startDate, endDate);
                        startDateTime = customRange[0];
                        endDateTime = customRange[1];
                        break;
                    default:
                        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid date filter. Allowed values: today, weekly, monthly, custom");
                }
            }

            Date startDateUtil = Date.from(startDateTime.atZone(DateUtils.getBusinessZone()).toInstant());
            Date endDateUtil = Date.from(endDateTime.atZone(DateUtils.getBusinessZone()).toInstant());

            // Fetch stats from repository
            Long totalBillsCount = billRepository.countByBillingStaffIdAndCreatedAtBetween(staffId, startDateUtil, endDateUtil);
            BigDecimal totalAmountBilled = billRepository.calculateTotalRevenueByStaffAndDateRange(staffId, startDateUtil, endDateUtil);
            BigDecimal totalAmountCollected = billRepository.calculatePaymentsByStaffAndDateRange(staffId, startDateUtil, endDateUtil);
            BigDecimal totalAmountPending = billRepository.calculatePendingAmountByStaffAndDateRange(staffId, startDateUtil, endDateUtil);

            // Build DTO
            StaffBillingStatsDTO stats = new StaffBillingStatsDTO();
            stats.setStaffId(staff.getId());
            stats.setStaffName(staff.getFirstName() + " " + staff.getLastName());
            stats.setTotalBillsCount(totalBillsCount);
            stats.setTotalAmountBilled(totalAmountBilled != null ? totalAmountBilled : BigDecimal.ZERO);
            stats.setTotalAmountCollected(totalAmountCollected != null ? totalAmountCollected : BigDecimal.ZERO);
            stats.setTotalAmountPending(totalAmountPending != null ? totalAmountPending : BigDecimal.ZERO);

            return baseResponse.successResponse("Staff billing stats fetched successfully", stats);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching billing stats for staffId={}", staffId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Unable to fetch billing stats at the moment.");
        }
    }


    @Override
    public ResponseEntity<?> getBillsByPatientId(Long patientId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Request received to fetch bills by patientId={} [pageNo={}, pageSize={}]",
                patientId, pageNo, pageSize
        );

        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            Page<Bill> page =
                    billRepository.findByPatientId(patientId, pageRequest);

            List<BillDTO> dtoList = page.getContent()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} bills for patientId={} on page {}",
                    page.getNumberOfElements(), patientId, page.getNumber()
            );

            return baseResponse.successResponse(
                    "Bills fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching bills by patientId={}", patientId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch bills at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> getBillsByBillingStaffId(Integer pageNo, Integer pageSize) {

        Long getLoggedUserId = DbUtill.getLoggedInUserId();
        logger.info(
                "Request received to fetch bills by billingStaffId={} [pageNo={}, pageSize={}]",
                getLoggedUserId, pageNo, pageSize
        );

        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            Page<Bill> page =
                    billRepository.findByBillingStaffId(getLoggedUserId, pageRequest);

            List<BillDTO> dtoList = page.getContent()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} bills for billingStaffId={} on page {}",
                    page.getNumberOfElements(), getLoggedUserId, page.getNumber()
            );

            return baseResponse.successResponse(
                    "Bills fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching bills by billingStaffId={}", getLoggedUserId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch bills at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getBillsByStaffId(Long staffId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Request received to fetch bills by specific staffId={} [pageNo={}, pageSize={}]",
                staffId, pageNo, pageSize
        );

        try {
            // Validate staff ID
            if (staffId == null) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Staff ID cannot be null"
                );
            }

            // Check if staff exists
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            Page<Bill> page =
                    billRepository.findByBillingStaffId(staffId, pageRequest);

            List<BillDTO> dtoList = page.getContent()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} bills for staffId={} on page {}",
                    page.getNumberOfElements(), staffId, page.getNumber()
            );

            return baseResponse.successResponse(
                    "Bills fetched successfully for staff: " + staff.getFirstName() + " " + staff.getLastName(),
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching bills by staffId={}", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch bills at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> getBillsByStatus(
            BillStatus status, Integer pageNo, Integer pageSize) {

        Long loggedInUserId = DbUtill.getLoggedInUserId();

        logger.info(
                "Request received to fetch bills by status={} [pageNo={}, pageSize={}]",
                status, pageNo, pageSize
        );

        try {

            if (status == null) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bill status cannot be null"
                );
            }

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            boolean isAdmin = rbacUtil.hasAnyRole(
                    UserRole.SAAS_ADMIN,
                    UserRole.SUPER_ADMIN,
                    UserRole.BRANCH_MANAGER,
                    UserRole.SAAS_ADMIN_MANAGER,
                    UserRole.SUPER_ADMIN_MANAGER
            );

            Page<Bill> page;

            // 🔥 MAIN LOGIC
            if (isAdmin) {
                // Admin → fetch all
                page = billRepository.findByStatus(status, pageRequest);
            } else {
                // Billing Staff → fetch only own bills
                page = billRepository.findByStatusAndBillingStaffId(
                        status,
                        loggedInUserId,
                        pageRequest
                );
            }

            List<BillDTO> dtoList = page.getContent()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} bills with status={} on page {}",
                    page.getNumberOfElements(), status, page.getNumber()
            );

            return baseResponse.successResponse(
                    "Bills fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching bills by status={}", status, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch bills at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> getBillsByAppointmentId(
            Long appointmentId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Request received to fetch bills by appointmentId={} [pageNo={}, pageSize={}]",
                appointmentId, pageNo, pageSize
        );

        try {

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            Page<Bill> page =
                    billRepository.findByAppointmentId(appointmentId, pageRequest);

            List<BillDTO> dtoList = page.getContent()
                    .stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} bills for appointmentId={} on page {}",
                    page.getNumberOfElements(), appointmentId, page.getNumber()
            );

            return baseResponse.successResponse(
                    "Bills fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching bills by appointmentId={}", appointmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch bills at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> deleteBill(Long billId) {

        logger.info("Request received to delete bill [billId={}]", billId);

        try {

            Bill bill = getBillOrThrow(billId);

            //            billItemRepository.findByBillId(billId)
            //                    .forEach(billItemRepository::delete);
            //
            //            billRepository.delete(bill);

            bill.setStatus(BillStatus.DELETED);
            billRepository.save(bill);

            logger.info("Bill deleted successfully [billId={}]", billId);

            return baseResponse.successResponse(
                    "Bill deleted successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while deleting bill [billId={}]", billId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete bill"
            );
        }
    }


    @Override
    public ResponseEntity<?> updateBillStatus(Long billId, BillStatus status) {

        logger.info(
                "Request received to update bill status [billId={}, status={}]",
                billId, status
        );

        try {
            if (status == null) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Bill status cannot be null"
                );
            }

            Bill bill = getBillOrThrow(billId);

            bill.setStatus(status);
            bill.setUpdatedAt(new Date());

            Bill updatedBill = billRepository.save(bill);
            convertToDTO(updatedBill);

            logger.info(
                    "Bill status updated successfully [billId={}, newStatus={}]",
                    billId, status
            );

            return baseResponse.successResponse(
                    "Bill status updated successfully"
            );

        } catch (IllegalArgumentException e) {
            logger.warn(
                    "Bill update failed | billId={} | reason={}",
                    billId, e.getMessage()
            );
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error(
                    "Unexpected error while updating bill status [billId={}]",
                    billId, e
            );
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update bill status at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> processPayment(Long billId, Double amountPaid, String paymentTypeStr) {

        logger.info("Processing payment [billId={}, amount={}, paymentType={}]", billId, amountPaid, paymentTypeStr);

        try {
            Bill bill = getBillOrThrow(billId);

            BigDecimal payment = BigDecimal.valueOf(amountPaid);

            if (payment.compareTo(BigDecimal.ZERO) <= 0) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Payment amount must be greater than zero"
                );
            }

            if (payment.compareTo(bill.getBalanceAmount()) > 0) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Payment amount cannot exceed remaining balance"
                );
            }

            // Validate payment type
            PaymentType paymentType = null;
            if (paymentTypeStr != null && !paymentTypeStr.isBlank()) {
                try {
                    paymentType = PaymentType.valueOf(paymentTypeStr.toUpperCase());
                } catch (IllegalArgumentException ex) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Invalid payment type: " + paymentTypeStr
                    );
                }
            } else {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Payment type is required"
                );
            }
            BigDecimal paidAmount =
                    bill.getPaidAmount().add(payment);

            BigDecimal balance =
                    bill.getTotalAmount().subtract(paidAmount);

            bill.setPaidAmount(paidAmount);
            bill.setBalanceAmount(balance);
            bill.setPaymentType(paymentType);
            bill.setPaymentDate(LocalDateTime.now(DateUtils.getBusinessZone()));
            bill.setUpdatedAt(new Date());

            if (balance.compareTo(BigDecimal.ZERO) <= 0) {
                bill.setStatus(BillStatus.PAID);
                bill.setPaymentDate(LocalDateTime.now(DateUtils.getBusinessZone()));
            } else {
                bill.setStatus(BillStatus.PARTIALLY_PAID);
            }

            billRepository.save(bill);

            return baseResponse.successResponse(
                    String.format("Payment of %s processed successfully via %s", payment, paymentType.name())
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while processing payment", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to process payment"
            );
        }
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


    private Bill getBillOrThrow(Long billId) {
        if (billId == null) {
            ErrorUtils.throwValidation("billId cannot be null");
        }

        return billRepository.findById(billId)
                .orElseThrow();
    }


    private void saveBillItems(Bill bill, List<BillItemDTO> items) {
        if (items == null || items.isEmpty()) return;

        items.forEach(itemDTO -> {
            BillItem item = new BillItem();
            item.setBill(bill);
            item.setItemName(itemDTO.getItemName());
            item.setTreatmentPackageId(itemDTO.getTreatmentPackageId());
            item.setItemDescription(itemDTO.getItemDescription());
            item.setQuantity(itemDTO.getQuantity());
            item.setUnitPrice(itemDTO.getUnitPrice());
            item.setTotalPrice(itemDTO.getTotalPrice());
            billItemRepository.save(item);
        });
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

    @Override
    public ResponseEntity<?> getStaffBillingSummary(Long staffId) {
        logger.info("Request received to fetch billing summary for staffId={}", staffId);

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            // Get main billing statistics
            Object[] stats = billRepository.getStaffBillingStatistics(staffId);

            // Get current month statistics
            LocalDateTime startOfMonth = DateUtils.getBusinessLocalDate().withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = DateUtils.getBusinessLocalDate().withDayOfMonth(DateUtils.getBusinessLocalDate().lengthOfMonth()).atTime(23, 59, 59);
            Object[] monthlyStats = billRepository.getStaffMonthlyStatistics(staffId, startOfMonth, endOfMonth);

            // Create summary DTO
            StaffBillingSummaryDTO summary = new StaffBillingSummaryDTO();

            // Staff Information
            summary.setStaffId(staff.getId());
            summary.setStaffName(staff.getFirstName() + " " + staff.getLastName());
            summary.setStaffRole(staff.getRole().name());
            if (staff.getBranch() != null) {
                summary.setStaffDepartment(staff.getBranch().getBranchName());
            }

            // Company/Branch Information
            if (staff.getCompany() != null) {
                summary.setCompanyId(staff.getCompany().getId());
                summary.setCompanyName(staff.getCompany().getCompanyName());
            }
            if (staff.getBranch() != null) {
                summary.setBranchId(staff.getBranch().getId());
                summary.setBranchName(staff.getBranch().getBranchName());
            }

            // Billing Statistics
            if (stats != null && stats.length > 0) {
                summary.setTotalBillsCreated(((Number) stats[0]).longValue());
                summary.setTotalAmountBilled((stats.length > 1 && stats[1] != null) ? convertToBigDecimal(stats[1]) : BigDecimal.ZERO);
                summary.setTotalAmountCollected((stats.length > 2 && stats[2] != null) ? convertToBigDecimal(stats[2]) : BigDecimal.ZERO);
                summary.setTotalAmountPending((stats.length > 3 && stats[3] != null) ? convertToBigDecimal(stats[3]) : BigDecimal.ZERO);
                summary.setTotalPaidBills((stats.length > 4 && stats[4] != null) ? ((Number) stats[4]).longValue() : 0L);
                summary.setTotalPendingBills((stats.length > 5 && stats[5] != null) ? ((Number) stats[5]).longValue() : 0L);
                summary.setTotalPartiallyPaidBills((stats.length > 6 && stats[6] != null) ? ((Number) stats[6]).longValue() : 0L);
                summary.setTotalCancelledBills((stats.length > 7 && stats[7] != null) ? ((Number) stats[7]).longValue() : 0L);

                // Calculate collection rate
                if (summary.getTotalAmountBilled().compareTo(BigDecimal.ZERO) > 0) {
                    double collectionRate = (summary.getTotalAmountCollected().doubleValue() /
                            summary.getTotalAmountBilled().doubleValue()) * 100;
                    summary.setCollectionRate(collectionRate);
                } else {
                    summary.setCollectionRate(0.0);
                }

                // Calculate averages
                if (summary.getTotalBillsCreated() > 0) {
                    summary.setAverageBillAmount(
                            summary.getTotalAmountBilled().divide(
                                    new BigDecimal(summary.getTotalBillsCreated()),
                                    RoundingMode.HALF_UP
                            )
                    );
                    summary.setAverageCollectionPerBill(
                            summary.getTotalAmountCollected().divide(
                                    new BigDecimal(summary.getTotalBillsCreated()),
                                    RoundingMode.HALF_UP
                            )
                    );
                }
            }

            // Monthly Statistics
            if (monthlyStats != null && monthlyStats.length > 0) {
                summary.setBillsThisMonth(((Number) monthlyStats[0]).longValue());
                summary.setAmountBilledThisMonth((monthlyStats.length > 1 && monthlyStats[1] != null) ? convertToBigDecimal(monthlyStats[1]) : BigDecimal.ZERO);
                summary.setAmountCollectedThisMonth((monthlyStats.length > 2 && monthlyStats[2] != null) ? convertToBigDecimal(monthlyStats[2]) : BigDecimal.ZERO);
            } else {
                // Set defaults when no monthly data is available
                summary.setBillsThisMonth(0L);
                summary.setAmountBilledThisMonth(BigDecimal.ZERO);
                summary.setAmountCollectedThisMonth(BigDecimal.ZERO);
            }

            // Calculate averages
            if (summary.getTotalBillsCreated() != null && summary.getTotalBillsCreated() > 0) {
                if (summary.getTotalAmountBilled() != null && summary.getTotalAmountBilled().compareTo(BigDecimal.ZERO) > 0) {
                    summary.setAverageBillAmount(
                            summary.getTotalAmountBilled().divide(
                                    new BigDecimal(summary.getTotalBillsCreated()),
                                    RoundingMode.HALF_UP
                            )
                    );
                } else {
                    summary.setAverageBillAmount(BigDecimal.ZERO);
                }

                if (summary.getTotalAmountCollected() != null && summary.getTotalAmountCollected().compareTo(BigDecimal.ZERO) > 0) {
                    summary.setAverageCollectionPerBill(
                            summary.getTotalAmountCollected().divide(
                                    new BigDecimal(summary.getTotalBillsCreated()),
                                    RoundingMode.HALF_UP
                            )
                    );
                } else {
                    summary.setAverageCollectionPerBill(BigDecimal.ZERO);
                }
            } else {
                // Set defaults when no bills exist
                summary.setAverageBillAmount(BigDecimal.ZERO);
                summary.setAverageCollectionPerBill(BigDecimal.ZERO);
            }

            logger.info("Billing summary fetched successfully for staffId={}", staffId);
            return baseResponse.successResponse("Billing summary fetched successfully", summary);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching billing summary for staffId={}", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch billing summary at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getStaffBillingReport(Long staffId, Integer pageNo, Integer pageSize) {
        logger.info("Request received to fetch detailed billing report for staffId={} [pageNo={}, pageSize={}]",
                staffId, pageNo, pageSize);

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);

            // Get bills for the staff
            Page<Bill> page = billRepository.findByBillingStaffId(staffId, pageRequest);

            // Create a special DTO with analytics data
            List<StaffBillingReportDTO> reportDTOs = page.getContent().stream()
                    .map(bill -> convertToStaffBillingReportWithAnalyticsDTO(bill, staffId))
                    .collect(Collectors.toList());

            // Create response map with pagination info
            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, reportDTOs);

            // Add comprehensive summary analytics to the response
            Object[] summaryStats = billRepository.getComprehensiveBillingSummaryByStaff(staffId);
            if (summaryStats != null && summaryStats.length > 0 && summaryStats[0] != null) {
                Map<String, Object> summary = new HashMap<>();
                summary.put("totalBilledAmount", summaryStats.length > 0 && summaryStats[0] != null ? convertToBigDecimal(summaryStats[0]) : BigDecimal.ZERO);
                summary.put("totalCollectedAmount", summaryStats.length > 1 && summaryStats[1] != null ? convertToBigDecimal(summaryStats[1]) : BigDecimal.ZERO);
                summary.put("totalDueAmount", summaryStats.length > 2 && summaryStats[2] != null ? convertToBigDecimal(summaryStats[2]) : BigDecimal.ZERO);
                summary.put("totalBillsCount", summaryStats.length > 3 && summaryStats[3] != null ? ((Number) summaryStats[3]).longValue() : 0L);
                summary.put("paidBillsCount", summaryStats.length > 4 && summaryStats[4] != null ? ((Number) summaryStats[4]).longValue() : 0L);
                summary.put("pendingBillsCount", summaryStats.length > 5 && summaryStats[5] != null ? ((Number) summaryStats[5]).longValue() : 0L);
                summary.put("partiallyPaidBillsCount", summaryStats.length > 6 && summaryStats[6] != null ? ((Number) summaryStats[6]).longValue() : 0L);
                summary.put("cancelledBillsCount", summaryStats.length > 7 && summaryStats[7] != null ? ((Number) summaryStats[7]).longValue() : 0L);

                // Calculate collection rate
                BigDecimal totalBilled = summaryStats[0] != null ? convertToBigDecimal(summaryStats[0]) : BigDecimal.ZERO;
                BigDecimal totalCollected = summaryStats[1] != null ? convertToBigDecimal(summaryStats[1]) : BigDecimal.ZERO;
                if (totalBilled.compareTo(BigDecimal.ZERO) > 0) {
                    Double collectionRate = (totalCollected.doubleValue() / totalBilled.doubleValue()) * 100;
                    summary.put("collectionRate", collectionRate);
                } else {
                    summary.put("collectionRate", 0.0);
                }

                response.put("summary", summary);
            }

            // Aging analysis removed as per user request

            // Add payment type breakdown for this staff
            List<Object[]> paymentBreakdown = billRepository.getPaymentTypeBreakdownByStaff(staffId);
            if (!paymentBreakdown.isEmpty()) {
                List<Map<String, Object>> paymentBreakdownList = paymentBreakdown.stream()
                        .map(obj -> {
                            Map<String, Object> breakdown = new HashMap<>();
                            breakdown.put("paymentType", obj[0] != null ? obj[0].toString() : "UNKNOWN");
                            breakdown.put("amount", obj[1] != null ? convertToBigDecimal(obj[1]) : BigDecimal.ZERO);
                            breakdown.put("count", obj[2] != null ? ((Number) obj[2]).longValue() : 0L);
                            return breakdown;
                        })
                        .collect(Collectors.toList());
                response.put("paymentBreakdown", paymentBreakdownList);
            }

            logger.info("Fetched {} billing records for staffId={} on page {}",
                    page.getNumberOfElements(), staffId, page.getNumber());

            String message = "Detailed billing report fetched successfully for staff: " + staff.getFirstName() + " " + staff.getLastName();

            return baseResponse.successResponse(message, response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching detailed billing report for staffId={}", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch detailed billing report at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getStaffBillingReportWithDateFilter(Long staffId,
                                                                 Integer pageNo,
                                                                 Integer pageSize,
                                                                 String dateFilter, String startDate, String endDate) {
        logger.info("Request received to fetch billing report for staffId={} with date filter [dateFilter={}, startDate={}, endDate={}, pageNo={}, pageSize={}]",
                staffId, dateFilter, startDate, endDate, pageNo, pageSize);

        try {

            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<Bill> page;

            LocalDateTime startDateTime;
            LocalDateTime endDateTime;

            if (dateFilter == null || dateFilter.equalsIgnoreCase("today")) {

                LocalDateTime[] range = DateUtils.getBusinessDayRange();
                startDateTime = range[0];
                endDateTime = range[1];

            } else {

                switch (dateFilter.toLowerCase()) {

                    case "weekly":
                    case "week":

                        LocalDateTime[] weekRange = DateUtils.getBusinessWeekRange();
                        startDateTime = weekRange[0];
                        endDateTime = weekRange[1];
                        break;

                    case "monthly":
                    case "month":

                        Long[] monthMillis = DateUtils.getMonthlyRangeInMilli();
                        startDateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(monthMillis[0]),
                                DateUtils.getBusinessZone());

                        endDateTime = LocalDateTime.ofInstant(
                                Instant.ofEpochMilli(monthMillis[1]),
                                DateUtils.getBusinessZone());
                        break;

                    case "custom":
                    case "range":

                        if (startDate == null || endDate == null) {
                            return baseResponse.errorResponse(
                                    HttpStatus.BAD_REQUEST,
                                    "Start date and end date are required for custom range"
                            );
                        }

                        if (!DateUtils.isValidDate(startDate) || !DateUtils.isValidDate(endDate)) {
                            return baseResponse.errorResponse(
                                    HttpStatus.BAD_REQUEST,
                                    "Date must be in format yyyy-MM-dd"
                            );
                        }

                        LocalDateTime[] customRange =
                                DateUtils.getStartAndEndDateTime(startDate, endDate);

                        startDateTime = customRange[0];
                        endDateTime = customRange[1];
                        break;

                    default:
                        return baseResponse.errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid date filter. Allowed values: today, weekly, monthly, custom"
                        );
                }
            }

            page = billRepository.findBillsByStaffIdAndDateRange(
                    staffId,
                    startDateTime,
                    endDateTime,
                    pageRequest
            );


            List<StaffBillingReportDTO> reportDTOs = page.getContent().stream()
                    .map(bill -> convertToStaffBillingReportWithAnalyticsDTO(bill, staffId))
                    .collect(Collectors.toList());

            // Create response map with pagination info
            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, reportDTOs);

            // Add comprehensive summary analytics to the response based on the date filter
            Object[] summaryStats = calculateSummaryFromPageContent(page.getContent());
            if (summaryStats.length > 0) {

                Map<String, Object> summary = new HashMap<>();

                summary.put("totalBilledAmount",
                        summaryStats[0] != null ? convertToBigDecimal(summaryStats[0]) : BigDecimal.ZERO);

                summary.put("totalCollectedAmount",
                        summaryStats[1] != null ? convertToBigDecimal(summaryStats[1]) : BigDecimal.ZERO);

                summary.put("totalDueAmount",
                        summaryStats[2] != null ? convertToBigDecimal(summaryStats[2]) : BigDecimal.ZERO);

                summary.put("totalBillsCount",
                        summaryStats[3] != null ? ((Number) summaryStats[3]).longValue() : 0L);

                summary.put("paidBillsCount",
                        summaryStats[4] != null ? ((Number) summaryStats[4]).longValue() : 0L);

                summary.put("pendingBillsCount",
                        summaryStats[5] != null ? ((Number) summaryStats[5]).longValue() : 0L);

                summary.put("partiallyPaidBillsCount",
                        summaryStats[6] != null ? ((Number) summaryStats[6]).longValue() : 0L);

                summary.put("cancelledBillsCount",
                        summaryStats[7] != null ? ((Number) summaryStats[7]).longValue() : 0L);

                BigDecimal totalBilled =
                        summaryStats[0] != null ? convertToBigDecimal(summaryStats[0]) : BigDecimal.ZERO;

                BigDecimal totalCollected =
                        summaryStats[1] != null ? convertToBigDecimal(summaryStats[1]) : BigDecimal.ZERO;

                double collectionRate = totalBilled.compareTo(BigDecimal.ZERO) > 0
                        ? (totalCollected.doubleValue() / totalBilled.doubleValue()) * 100
                        : 0.0;

                summary.put("collectionRate", collectionRate);

                response.put("summary", summary);
            }

            List<Object[]> paymentBreakdown = billRepository.getPaymentTypeBreakdownByStaffAndDateRange(staffId,startDateTime,endDateTime);
            if (!paymentBreakdown.isEmpty()) {

                List<Map<String, Object>> paymentBreakdownList =
                        paymentBreakdown.stream().map(obj -> {

                            Map<String, Object> breakdown = new HashMap<>();

                            breakdown.put("paymentType",
                                    obj[0] != null ? obj[0].toString() : "UNKNOWN");

                            breakdown.put("amount",
                                    obj[1] != null ? convertToBigDecimal(obj[1]) : BigDecimal.ZERO);

                            breakdown.put("count",
                                    obj[2] != null ? ((Number) obj[2]).longValue() : 0L);

                            return breakdown;

                        }).toList();

                response.put("paymentBreakdown", paymentBreakdownList);
            }

            logger.info("Fetched {} billing records for staffId={} on page {}",
                    page.getNumberOfElements(), staffId, page.getNumber());

            String message = "Detailed billing report fetched successfully for staff: " + staff.getFirstName() + " " + staff.getLastName();
            if (dateFilter != null) {
                message += " (filtered by " + dateFilter;
                if (dateFilter.equals("custom") && startDate != null && endDate != null) {
                    message += ": " + startDate + " to " + endDate;
                }
                message += ")";
            }

            return baseResponse.successResponse(message, response);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching billing report for staffId={}", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch billing report at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getStaffCollectionRate(Long staffId) {
        logger.info("Request received to fetch collection rate for staffId={}", staffId);

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            Object[] stats = billRepository.getStaffBillingStatistics(staffId);

            if (stats == null || stats.length == 0) {
                return baseResponse.successResponse("No billing data found for staff",
                        Map.of("staffId", staffId, "collectionRate", 0.0, "totalBills", 0L));
            }

            BigDecimal totalAmountBilled = (stats.length > 1 && stats[1] != null) ? convertToBigDecimal(stats[1]) : BigDecimal.ZERO;
            BigDecimal totalAmountCollected = (stats.length > 2 && stats[2] != null) ? convertToBigDecimal(stats[2]) : BigDecimal.ZERO;
            Long totalBills = ((Number) stats[0]).longValue();

            double collectionRate = 0.0;
            if (totalAmountBilled.compareTo(BigDecimal.ZERO) > 0) {
                collectionRate = (totalAmountCollected.doubleValue() / totalAmountBilled.doubleValue()) * 100;
            }

            Map<String, Object> result = Map.of(
                    "staffId", staffId,
                    "staffName", staff.getFirstName() + " " + staff.getLastName(),
                    "totalBills", totalBills,
                    "totalAmountBilled", totalAmountBilled,
                    "totalAmountCollected", totalAmountCollected,
                    "collectionRate", collectionRate,
                    "amountPending", totalAmountBilled.subtract(totalAmountCollected)
            );

            logger.info("Collection rate fetched successfully for staffId={}: {}%", staffId, collectionRate);
            return baseResponse.successResponse("Collection rate fetched successfully", result);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching collection rate for staffId={}", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch collection rate at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getTopPerformingStaff(Integer limit) {
        logger.info("Request received to fetch top performing staff [limit={}]", limit);

        try {
            if (limit == null || limit <= 0) {
                limit = 10; // Default limit
            }

            List<Object[]> topStaffStats = billRepository.getTopPerformingStaffByCollectionRate();

            List<Map<String, Object>> topPerformers = topStaffStats.stream()
                    .limit(limit)
                    .map(this::convertToTopPerformerDTO)
                    .collect(Collectors.toList());

            logger.info("Fetched top {} performing staff members", topPerformers.size());
            return baseResponse.successResponse("Top performing staff fetched successfully", topPerformers);

        } catch (Exception e) {
            logger.error("Error while fetching top performing staff", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch top performing staff at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getStaffPendingBills(Long staffId, Integer pageNo, Integer pageSize) {
        logger.info("Request received to fetch pending bills for staffId={} [pageNo={}, pageSize={}]",
                staffId, pageNo, pageSize);

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<Bill> page = billRepository.findByBillingStaffIdAndStatus(staffId, BillStatus.PENDING, pageRequest);

            List<BillDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info("Fetched {} pending bills for staffId={} on page {}",
                    page.getNumberOfElements(), staffId, page.getNumber());

            return baseResponse.successResponse(
                    "Pending bills fetched successfully for staff: " + staff.getFirstName() + " " + staff.getLastName(),
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching pending bills for staffId={}", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch pending bills at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> getStaffBillingSummaryByDateRange(Long staffId, String dateFilter, String startDate, String endDate) {
        logger.info("Request received to fetch billing summary for staffId={} with date filter [dateFilter={}, startDate={}, endDate={}]",
                staffId, dateFilter, startDate, endDate);

        try {
            User staff = userRepository.findById(staffId)
                    .orElseThrow(() -> new IllegalArgumentException("Staff not found with ID: " + staffId));

            LocalDateTime startDateTime;
            LocalDateTime endDateTime;

            if (dateFilter == null || dateFilter.equalsIgnoreCase("today")) {
                LocalDateTime[] range = DateUtils.getBusinessDayRange();
                startDateTime = range[0];
                endDateTime = range[1];

            } else {

                switch (dateFilter.toLowerCase()) {

                    case "weekly":
                    case "week":

                        LocalDateTime[] weekRange = DateUtils.getBusinessWeekRange();
                        startDateTime = weekRange[0];
                        endDateTime = weekRange[1];
                        break;

                    case "monthly":
                    case "month":

                        LocalDate today = DateUtils.getBusinessLocalDate();
                        LocalDate firstDay = today.withDayOfMonth(1);
                        LocalDate lastDay = today.withDayOfMonth(today.lengthOfMonth());

                        startDateTime = DateUtils.getStartOfBusinessDay(firstDay);
                        endDateTime = DateUtils.getEndOfBusinessDay(lastDay);
                        break;

                    case "custom":
                    case "range":

                        if (startDate == null || endDate == null) {
                            return baseResponse.errorResponse(
                                    HttpStatus.BAD_REQUEST,
                                    "Start date and end date are required for custom range"
                            );
                        }

                        if (!DateUtils.isValidDate(startDate) || !DateUtils.isValidDate(endDate)) {
                            return baseResponse.errorResponse(
                                    HttpStatus.BAD_REQUEST,
                                    "Date must be in format yyyy-MM-dd"
                            );
                        }

                        LocalDateTime[] customRange =
                                DateUtils.getStartAndEndDateTime(startDate, endDate);

                        startDateTime = customRange[0];
                        endDateTime = customRange[1];
                        break;

                    default:
                        return baseResponse.errorResponse(
                                HttpStatus.BAD_REQUEST,
                                "Invalid date filter. Allowed values: today, weekly, monthly, custom"
                        );
                }
            }

            Date startDateUtil = Date.from(
                    startDateTime.atZone(DateUtils.getBusinessZone()).toInstant()
            );

            Date endDateUtil = Date.from(
                    endDateTime.atZone(DateUtils.getBusinessZone()).toInstant()
            );

            // Get billing statistics for the specified date range
            Long totalBills = billRepository.countByBillingStaffIdAndCreatedAtBetween(staffId, startDateUtil, endDateUtil);

            // Calculate total amount billed within the date range
            BigDecimal totalAmountBilled = billRepository.calculateTotalRevenueByStaffAndDateRange(staffId, startDateUtil, endDateUtil);

            // Calculate paid amount within the date range
            BigDecimal totalAmountCollected = billRepository.calculatePaymentsByStaffAndDateRange(staffId, startDateUtil, endDateUtil);

//            // Calculate pending amount (bills created in date range but not fully paid)
//            List<Bill> billsInRange = billRepository.findBillsByStaffIdAndDateRange(staffId, startDateTime,
//                    endDateTime,
//                    PageRequest.of(0, Integer.MAX_VALUE)).getContent();

            BigDecimal totalAmountPending =
                    billRepository.calculatePendingAmountByStaffAndDateRange(
                            staffId, startDateUtil, endDateUtil);

            // Count bills by status in the date range
            Long paidBills = billRepository.countByBillingStaffIdAndStatusAndCreatedAtBetween(staffId, BillStatus.PAID, startDateUtil, endDateUtil);
            Long pendingBills = billRepository.countByBillingStaffIdAndStatusAndCreatedAtBetween(staffId, BillStatus.PENDING, startDateUtil, endDateUtil);
            Long partiallyPaidBills = billRepository.countByBillingStaffIdAndStatusAndCreatedAtBetween(staffId, BillStatus.PARTIALLY_PAID, startDateUtil, endDateUtil);
            Long cancelledBills = billRepository.countByBillingStaffIdAndStatusAndCreatedAtBetween(staffId, BillStatus.CANCELLED, startDateUtil, endDateUtil);

            // Calculate collection rate
            double collectionRate = 0.0;
            if (totalAmountBilled != null && totalAmountBilled.compareTo(BigDecimal.ZERO) > 0 && totalAmountCollected != null) {
                collectionRate = (totalAmountCollected.doubleValue() / totalAmountBilled.doubleValue()) * 100;
            }

            // Calculate averages
            BigDecimal averageBillAmount = BigDecimal.ZERO;
            BigDecimal averageCollectionPerBill = BigDecimal.ZERO;

            if (totalBills > 0) {
                if (totalAmountBilled != null && totalAmountBilled.compareTo(BigDecimal.ZERO) > 0) {
                    averageBillAmount = totalAmountBilled.divide(BigDecimal.valueOf(totalBills), 2, RoundingMode.HALF_UP);
                }
                if (totalAmountCollected != null && totalAmountCollected.compareTo(BigDecimal.ZERO) > 0) {
                    averageCollectionPerBill = totalAmountCollected.divide(BigDecimal.valueOf(totalBills), 2, RoundingMode.HALF_UP);
                }
            }

            // Create summary DTO
            StaffBillingSummaryDTO summary = new StaffBillingSummaryDTO();

            // Staff Information
            summary.setStaffId(staff.getId());
            summary.setStaffName(staff.getFirstName() + " " + staff.getLastName());
            summary.setStaffRole(staff.getRole().name());
            if (staff.getBranch() != null) {
                summary.setStaffDepartment(staff.getBranch().getBranchName());
            }

            // Company/Branch Information
            if (staff.getCompany() != null) {
                summary.setCompanyId(staff.getCompany().getId());
                summary.setCompanyName(staff.getCompany().getCompanyName());
            }
            if (staff.getBranch() != null) {
                summary.setBranchId(staff.getBranch().getId());
                summary.setBranchName(staff.getBranch().getBranchName());
            }

            // Billing Statistics for the date range
            summary.setTotalBillsCreated(totalBills);
            summary.setTotalAmountBilled(totalAmountBilled != null ? totalAmountBilled : BigDecimal.ZERO);
            summary.setTotalAmountCollected(totalAmountCollected != null ? totalAmountCollected : BigDecimal.ZERO);
            summary.setTotalAmountPending(totalAmountPending);
            summary.setTotalPaidBills(paidBills);
            summary.setTotalPendingBills(pendingBills);
            summary.setTotalPartiallyPaidBills(partiallyPaidBills);
            summary.setTotalCancelledBills(cancelledBills);
            summary.setCollectionRate(collectionRate);
            summary.setAverageBillAmount(averageBillAmount);
            summary.setAverageCollectionPerBill(averageCollectionPerBill);

            // Date range information
            summary.setDateRangeStart(startDateTime);
            summary.setDateRangeEnd(endDateTime);
            summary.setDateRangeType(dateFilter == null ? "today" : dateFilter);

            // Monthly statistics for the period (set to same as overall for date range)
            summary.setBillsThisMonth(totalBills);
            summary.setAmountBilledThisMonth(totalAmountBilled != null ? totalAmountBilled : BigDecimal.ZERO);
            summary.setAmountCollectedThisMonth(totalAmountCollected != null ? totalAmountCollected : BigDecimal.ZERO);

            logger.info("Billing summary fetched successfully for staffId={} in date range {} to {}", staffId, startDateTime, endDateTime);
            return baseResponse.successResponse("Billing summary fetched successfully for " + dateFilter + " period", summary);

        } catch (IllegalArgumentException e) {
            logger.warn("Invalid staff ID or staff not found | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching billing summary for staffId={} with date filter", staffId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch billing summary at the moment. Please try again later."
            );
        }
    }

    // Helper methods for conversion
    private StaffBillingSummaryDTO convertToStaffBillingSummary(Object[] stats) {
        StaffBillingSummaryDTO summary = new StaffBillingSummaryDTO();

        Long staffId = ((Number) stats[0]).longValue();
        User staff = userRepository.findById(staffId).orElse(null);

        if (staff != null) {
            // Staff Information
            summary.setStaffId(staff.getId());
            summary.setStaffName(staff.getFirstName() + " " + staff.getLastName());
            summary.setStaffRole(staff.getRole().name());
            if (staff.getBranch() != null) {
                summary.setStaffDepartment(staff.getBranch().getBranchName());
            }

            // Company/Branch Information
            if (staff.getCompany() != null) {
                summary.setCompanyId(staff.getCompany().getId());
                summary.setCompanyName(staff.getCompany().getCompanyName());
            }
            if (staff.getBranch() != null) {
                summary.setBranchId(staff.getBranch().getId());
                summary.setBranchName(staff.getBranch().getBranchName());
            }
        }

        // Billing Statistics
        summary.setTotalBillsCreated(((Number) stats[1]).longValue());
        summary.setTotalAmountBilled(stats[2] != null ? convertToBigDecimal(stats[2]) : BigDecimal.ZERO);
        summary.setTotalAmountCollected(stats[3] != null ? convertToBigDecimal(stats[3]) : BigDecimal.ZERO);
        summary.setTotalAmountPending(stats[4] != null ? convertToBigDecimal(stats[4]) : BigDecimal.ZERO);
        summary.setTotalPaidBills(((Number) stats[5]).longValue());
        summary.setTotalPendingBills(((Number) stats[6]).longValue());
        summary.setTotalPartiallyPaidBills(((Number) stats[7]).longValue());
        summary.setTotalCancelledBills(((Number) stats[8]).longValue());

        // Calculate collection rate
        if (summary.getTotalAmountBilled() != null && summary.getTotalAmountBilled().compareTo(BigDecimal.ZERO) > 0) {
            double collectionRate = (summary.getTotalAmountCollected().doubleValue() /
                    summary.getTotalAmountBilled().doubleValue()) * 100;
            summary.setCollectionRate(collectionRate);
        } else {
            summary.setCollectionRate(0.0);
        }

        // Calculate averages
        if (summary.getTotalBillsCreated() != null && summary.getTotalBillsCreated() > 0) {
            if (summary.getTotalAmountBilled() != null && summary.getTotalAmountBilled().compareTo(BigDecimal.ZERO) > 0) {
                summary.setAverageBillAmount(
                        summary.getTotalAmountBilled().divide(
                                new BigDecimal(summary.getTotalBillsCreated()),
                                RoundingMode.HALF_UP
                        )
                );
            } else {
                summary.setAverageBillAmount(BigDecimal.ZERO);
            }

            if (summary.getTotalAmountCollected() != null && summary.getTotalAmountCollected().compareTo(BigDecimal.ZERO) > 0) {
                summary.setAverageCollectionPerBill(
                        summary.getTotalAmountCollected().divide(
                                new BigDecimal(summary.getTotalBillsCreated()),
                                RoundingMode.HALF_UP
                        )
                );
            } else {
                summary.setAverageCollectionPerBill(BigDecimal.ZERO);
            }
        } else {
            // Set defaults when no bills exist
            summary.setAverageBillAmount(BigDecimal.ZERO);
            summary.setAverageCollectionPerBill(BigDecimal.ZERO);
        }

        // Set monthly statistics to zero since they're not included in the all-staff query
        summary.setBillsThisMonth(0L);
        summary.setAmountBilledThisMonth(BigDecimal.ZERO);
        summary.setAmountCollectedThisMonth(BigDecimal.ZERO);

        return summary;
    }

    private StaffBillingReportDTO convertToStaffBillingReportDTO(Bill bill) {
        StaffBillingReportDTO dto = new StaffBillingReportDTO();

        ZoneId IST = ZoneId.of("Asia/Kolkata");

        // Bill Information
        dto.setBillId(bill.getId());
        dto.setBillNumber(bill.getBillNumber());
        dto.setPatientId(bill.getPatient().getId());
        dto.setPatientName(bill.getPatient().getFirstName() + " " + bill.getPatient().getLastName());
        dto.setPatientPhone(bill.getPatient().getPhoneNumber());
        dto.setPatientEmail(bill.getPatient().getEmail());
        dto.setPatientUhid(bill.getPatient().getUhid());
        dto.setStatus(bill.getStatus());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setPaidAmount(bill.getPaidAmount());
        dto.setBalanceAmount(bill.getBalanceAmount());
        dto.setCreatedAt(bill.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        if (bill.getPaymentDate() != null) {
            dto.setPaymentDate(bill.getPaymentDate().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        dto.setPaymentType(bill.getPaymentType());
        dto.setNotes(bill.getNotes());
        dto.setCouponCode(bill.getCouponCode());
        dto.setCouponAmount(bill.getCouponAmount());

        // Staff Information
        dto.setBillingStaffId(bill.getBillingStaff().getId());
        dto.setBillingStaffName(bill.getBillingStaff().getFirstName() + " " + bill.getBillingStaff().getLastName());
        dto.setBillingStaffRole(bill.getBillingStaff().getRole().name());
        dto.setBillingStaffEmail(bill.getBillingStaff().getEmail());
        dto.setBillingStaffPhone(bill.getBillingStaff().getPhoneNumber());
        if (bill.getBillingStaff().getBranch() != null) {
            dto.setBillingStaffDepartment(bill.getBillingStaff().getBranch().getBranchName());
        }

        // Company/Branch Information
        if (bill.getCompany() != null) {
            dto.setCompanyId(bill.getCompany().getId());
            dto.setCompanyName(bill.getCompany().getCompanyName());
        }
        if (bill.getBranch() != null) {
            dto.setBranchId(bill.getBranch().getId());
            dto.setBranchName(bill.getBranch().getBranchName());
        }

        // Appointment Information
        if (bill.getAppointment() != null) {
            dto.setAppointmentId(bill.getAppointment().getId());
            dto.setAppointmentType(bill.getAppointment().getStatus().name());
            dto.setAppointmentDate(bill.getAppointment().getScheduledTime());
            // Since Appointment doesn't have separate start/end time fields, we'll use scheduledTime and completedAt
            dto.setAppointmentStartTime(bill.getAppointment().getScheduledTime());
//            dto.setAppointmentEndTime(bill.getAppointment().getCompletedAt());
            if (bill.getAppointment().getCompletedAt() != null) {
                dto.setAppointmentEndTime(
                        bill.getAppointment()
                                .getCompletedAt()
                                .atZone(ZoneId.of("UTC"))
                                .withZoneSameInstant(IST)
                                .toLocalDateTime()
                );
            }

            // Doctor Information if available
            if (bill.getAppointment().getDoctor() != null) {
                dto.setDoctorId(bill.getAppointment().getDoctor().getId());
                dto.setDoctorName(bill.getAppointment().getDoctor().getDoctorName());
                dto.setDoctorSpecialization(bill.getAppointment().getDoctor().getSpecialization());
                dto.setDoctorQualification(bill.getAppointment().getDoctor().getQualification());
            }
        }

        // Bill Items Information
        List<BillItem> billItems = bill.getBillItems();
        if (billItems != null && !billItems.isEmpty()) {
            List<Map<String, Object>> items = billItems.stream().map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("itemName", item.getItemName());
                itemMap.put("itemDescription", item.getItemDescription());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());
                itemMap.put("totalPrice", item.getTotalPrice());
                return itemMap;
            }).collect(Collectors.toList());

            dto.setBillItems(items);
        }

        return dto;
    }

    private BigDecimal convertToBigDecimal(Object value) {
        if (value == null) {
            return BigDecimal.ZERO;
        }

        try {
            if (value instanceof BigDecimal) {
                return (BigDecimal) value;
            } else if (value instanceof Number) {
                return BigDecimal.valueOf(((Number) value).doubleValue());
            } else {
                // Handle string conversion more safely
                String strValue = value.toString().trim();
                if (strValue.isEmpty() || "null".equalsIgnoreCase(strValue)) {
                    return BigDecimal.ZERO;
                }
                // Handle scientific notation by converting through double first
                try {
                    Double doubleValue = Double.parseDouble(strValue);
                    return BigDecimal.valueOf(doubleValue);
                } catch (NumberFormatException ex) {
                    // If double parsing fails, try direct BigDecimal conversion
                    return new BigDecimal(strValue);
                }
            }
        } catch (NumberFormatException e) {
            logger.warn("Failed to convert value '{}' to BigDecimal, using ZERO", value, e);
            return BigDecimal.ZERO;
        }
    }

    private StaffBillingReportDTO convertToStaffBillingReportWithAnalyticsDTO(Bill bill, Long staffId) {
        StaffBillingReportDTO dto = convertToStaffBillingReportDTO(bill);

        // Add comprehensive analytics for the staff
        try {
            // Fetch staff analytics
            Object[] stats = billRepository.getComprehensiveBillingSummaryByStaff(staffId);

            if (stats != null) {

                dto.setPaidBillsCountForStaff(stats[4] != null ? ((Number) stats[4]).longValue() : 0L);
                dto.setPartiallyPaidBillsCountForStaff(stats[6] != null ? ((Number) stats[6]).longValue() : 0L);
                dto.setCancelledBillsCountForStaff(stats[7] != null ? ((Number) stats[7]).longValue() : 0L);

                BigDecimal totalBilled = stats[0] != null ? (BigDecimal) stats[0] : BigDecimal.ZERO;
                BigDecimal totalCollected = stats[1] != null ? (BigDecimal) stats[1] : BigDecimal.ZERO;

                if (totalBilled.compareTo(BigDecimal.ZERO) > 0) {
                    Double collectionRate =
                        totalCollected.divide(totalBilled, 4, RoundingMode.HALF_UP)
                            .multiply(BigDecimal.valueOf(100))
                            .doubleValue();

                    dto.setCollectionRateForStaff(collectionRate);
                } else {
                    dto.setCollectionRateForStaff(0.0);
                }

                Long totalBills = stats[3] != null ? ((Number) stats[3]).longValue() : 0L;

                if (totalBills > 0) {
                    dto.setAverageBillAmountForStaff(
                        totalBilled.divide(BigDecimal.valueOf(totalBills), 2, RoundingMode.HALF_UP)
                    );
                } else {
                    dto.setAverageBillAmountForStaff(BigDecimal.ZERO);
                }
            }
            // Top services analysis removed as per user request

            // High value patients analysis removed as per user request


        } catch (Exception e) {
            logger.error("Error calculating analytics for staff ID: " + staffId, e);
            // Set defaults if there's an error
            dto.setCollectionRateForStaff(0.0);
        }

        return dto;
    }

    private Map<String, Object> convertToTopPerformerDTO(Object[] stats) {
        Long staffId = ((Number) stats[0]).longValue();
        User staff = userRepository.findById(staffId).orElse(null);

        String staffName = "Unknown Staff";
        if (staff != null) {
            staffName = staff.getFirstName() + " " + staff.getLastName();
        }

        return Map.of(
                "staffId", staffId,
                "staffName", staffName,
                "totalBills", ((Number) stats[1]).longValue(),
                "totalAmountBilled", stats[2] != null ? convertToBigDecimal(stats[2]) : BigDecimal.ZERO,
                "totalAmountCollected", stats[3] != null ? convertToBigDecimal(stats[3]) : BigDecimal.ZERO,
                "collectionRate", ((Number) stats[4]).doubleValue()
        );
    }

    @Override
    public ResponseEntity<?> getBillingSummaryStats(String dateFilter, Long branchId, String startDate, String endDate) {

        User currentUser = DbUtill.getCurrentUser();

        logger.info(
                "Request received to fetch billing summary [dateFilter={}, branchId={}, startDate={}, endDate={}]",
                dateFilter, branchId, startDate, endDate
        );

        try {

            Long[] dateRangeInMilli;

            if ("today".equalsIgnoreCase(dateFilter)) {

                dateRangeInMilli = DateUtils.getTodayRangeInMilli();

            } else if ("weekly".equalsIgnoreCase(dateFilter)) {

                dateRangeInMilli = DateUtils.getWeeklyRangeInMilli();

            } else if ("monthly".equalsIgnoreCase(dateFilter)) {

                dateRangeInMilli = DateUtils.getMonthlyRangeInMilli();

            } else if ("custom".equalsIgnoreCase(dateFilter)) {

                if (startDate == null || startDate.isBlank() || endDate == null || endDate.isBlank()) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "startDate and endDate are required for custom filter"
                    );
                }

                if (!DateUtils.isValidDate(startDate) || !DateUtils.isValidDate(endDate)) {
                    return baseResponse.errorResponse(
                            HttpStatus.BAD_REQUEST,
                            "Date must be in format yyyy-MM-dd"
                    );
                }

                dateRangeInMilli = DateUtils.getStartAndEndDateInMilli(startDate, endDate);

            } else {

                dateRangeInMilli = DateUtils.getTodayRangeInMilli();
            }

            Long companyId = null;
            Long finalBranchId = null;

            if (rbacUtil.isSuperAdmin()) {

                finalBranchId = branchId;

                logger.info("SUPER_ADMIN accessing billing summary");

            } else if (rbacUtil.isAdmin()) {

                companyId = DbUtill.getLoggedInCompanyId();

                if (branchId != null) {

                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() ->
                                    new IllegalArgumentException("Branch not found with id: " + branchId));

                    if (!branch.getClinic().getId().equals(companyId)) {

                        return baseResponse.errorResponse(
                                HttpStatus.FORBIDDEN,
                                "Branch access denied"
                        );
                    }

                    finalBranchId = branchId;
                }

                logger.info("SAAS_ADMIN accessing billing summary");
            } else {

                if (currentUser.getBranch() == null) {

                    return baseResponse.errorResponse(
                            HttpStatus.FORBIDDEN,
                            "User is not assigned to any branch"
                    );
                }

                finalBranchId = currentUser.getBranch().getId();

                logger.info("{} accessing billing summary for branch {}",
                        currentUser.getRole(),
                        finalBranchId);
            }

            Object[] result = billRepository.getBillingSummary(
                    companyId,
                    finalBranchId,
                    dateRangeInMilli[0],
                    dateRangeInMilli[1]
            );

            if (result == null || result.length == 0) {
                BillingOverviewSummaryDTO dto = new BillingOverviewSummaryDTO();

                dto.setPaidBillCount(0L);
                dto.setPendingBillCount(0L);
                dto.setTotalCollectedAmount(0.0);
                dto.setPendingAmount(0.0);

                logger.info("Billing summary fetched successfully For emty Response");

                return baseResponse.successResponse(
                    "Billing summary fetched successfully",
                    dto
                );
            }

            Object[] data = (Object[]) result[0];

            BillingOverviewSummaryDTO dto = new BillingOverviewSummaryDTO();

            dto.setTotalCollectedAmount(((Number) data[0]).doubleValue());
            dto.setPendingAmount(((Number) data[1]).doubleValue());
            dto.setPaidBillCount(((Number) data[2]).longValue());
            dto.setPendingBillCount(((Number) data[3]).longValue());

            logger.info("Billing summary fetched successfully");

            return baseResponse.successResponse(
                    "Billing summary fetched successfully",
                    dto
            );

        } catch (IllegalArgumentException e) {

            logger.warn("Validation failed | {}", e.getMessage());

            return baseResponse.errorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );
        } catch (Exception e) {

            logger.error("Error while fetching billing summary", e);

            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch billing summary at the moment. Please try again later."
            );
        }
    }


    public ResponseEntity<?> getStaffBillingOverviewByDateRangeOld(String dateFilter,
                                                                String startDate,
                                                                String endDate,
                                                                Long branchId) {
        User currentUser = DbUtill.getCurrentUser();
        logger.info("Request received to fetch staff billing overview by date range [dateFilter={}, startDate={}, endDate={}, branchId={}]",
                dateFilter, startDate, endDate, branchId);

        try {
            // Use business timezone (IST) instead of server timezone
            LocalDate today = DateUtils.getBusinessLocalDate();

            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(LocalTime.MAX);

            LocalDate now = DateUtils.getBusinessLocalDate();

            LocalDateTime startOfMonth = now.withDayOfMonth(1).atStartOfDay();
            LocalDateTime endOfMonth = now.withDayOfMonth(now.lengthOfMonth()).atTime(LocalTime.MAX);

            List<Object[]> allStats;

            // Role-based access control for staff billing overview with date range
            if (rbacUtil.isSuperAdmin()) {
                // SUPER_ADMIN and SUPER_ADMIN_MANAGER see all stats system-wide
                if (dateFilter != null) {
                    switch (dateFilter.toLowerCase()) {
                        case "today":
                        case "daily":
                        case "day":
                            if (branchId != null) {
                                allStats = billRepository.getStaffBillingStatisticsForTodayByBranch(branchId, startOfDay, endOfDay);
                            } else {
                                allStats = billRepository.getStaffBillingStatisticsForToday(startOfDay, endOfDay);
                            }
                            break;
                        case "weekly":
                        case "week":
                            LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                            LocalDate weekEnd = weekStart.plusDays(6);
                            LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
                            LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);

                            if (branchId != null) {
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndBranch(branchId, weekStartDateTime, weekEndDateTime);
                            } else {
                                allStats = billRepository.getStaffBillingStatisticsByDateRange(weekStartDateTime, weekEndDateTime);
                            }
                            break;
                        case "monthly":
                        case "month":
                            if (branchId != null) {
                                allStats = billRepository.getStaffBillingStatisticsForCurrentMonthByBranch(branchId, startOfMonth, endOfMonth);
                            } else {
                                allStats = billRepository.getStaffBillingStatisticsForCurrentMonth(startOfMonth, endOfMonth);
                            }
                            break;
                        case "custom":
                        case "range":
                            if (startDate == null || endDate == null) {
                                return baseResponse.errorResponse(
                                        HttpStatus.BAD_REQUEST,
                                        "Start date and end date are required for custom date range"
                                );
                            }

                            LocalDateTime startDateTime;
                            LocalDateTime endDateTime;

                            try {
                                LocalDateTime[] dateTimes = DateUtils.getStartAndEndDateTime(startDate, endDate);
                                startDateTime = dateTimes[0];
                                endDateTime = dateTimes[1];
                            } catch (Exception e) {
                                return baseResponse.errorResponse(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid date format. Please use either date format (yyyy-MM-dd) or datetime format (yyyy-MM-ddTHH:mm:ss)"
                                );
                            }

                            if (branchId != null) {
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndBranch(branchId, startDateTime, endDateTime);
                            } else {
                                allStats = billRepository.getStaffBillingStatisticsByDateRange(startDateTime, endDateTime);
                            }
                            break;
                        default:
                            return baseResponse.errorResponse(
                                    HttpStatus.BAD_REQUEST,
                                    "Invalid date filter. Use 'today', 'daily', 'weekly', 'monthly', or 'custom'"
                            );
                    }
                } else {
                    // Default to all time if no date filter specified
                    if (branchId != null) {
                        allStats = billRepository.getAllStaffBillingStatisticsByBranch(branchId);
                    } else {
                        allStats = billRepository.getAllStaffBillingStatistics();
                    }
                }
                logger.info("SUPER_ADMIN accessing staff billing overview with date filter: {}, branchId: {}", dateFilter, branchId);

            } else if (rbacUtil.isAdmin()) {
                // SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's billing overview");
                    }
                    // Branch-wise filtering with date range
                    if (dateFilter != null) {
                        switch (dateFilter.toLowerCase()) {
                            case "today":
                            case "daily":
                            case "day":
                                allStats = billRepository.getStaffBillingStatisticsForTodayByBranch(branchId, startOfDay, endOfDay);
                                break;
                            case "weekly":
                            case "week":
                                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                                LocalDate weekEnd = weekStart.plusDays(6);
                                LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
                                LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndBranch(branchId, weekStartDateTime, weekEndDateTime);
                                break;
                            case "monthly":
                            case "month":
                                allStats = billRepository.getStaffBillingStatisticsForCurrentMonthByBranch(branchId, startOfMonth, endOfMonth);
                                break;
                            case "custom":
                            case "range":
                                if (startDate == null || endDate == null) {
                                    return baseResponse.errorResponse(
                                            HttpStatus.BAD_REQUEST,
                                            "Start date and end date are required for custom date range"
                                    );
                                }
                                LocalDateTime startDateTime;
                                LocalDateTime endDateTime;

                                try {
                                    LocalDateTime[] dateTimes = DateUtils.getStartAndEndDateTime(startDate, endDate);
                                    startDateTime = dateTimes[0];
                                    endDateTime = dateTimes[1];
                                } catch (Exception e) {
                                    return baseResponse.errorResponse(
                                            HttpStatus.BAD_REQUEST,
                                            "Invalid date format. Please use either date format (yyyy-MM-dd) or datetime format (yyyy-MM-ddTHH:mm:ss)"
                                    );
                                }

                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndBranch(branchId, startDateTime, endDateTime);
                                break;
                            default:
                                return baseResponse.errorResponse(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid date filter. Use 'today', 'daily', 'weekly', 'monthly', or 'custom'"
                                );
                        }
                    } else {
                        allStats = billRepository.getAllStaffBillingStatisticsByBranch(branchId);
                    }
                    logger.info("SAAS_ADMIN accessing staff billing overview for branch ID: {} with date filter: {}", branchId, dateFilter);
                } else {
                    // Company-wise filtering - need to filter company stats by date
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    if (dateFilter != null) {
                        switch (dateFilter.toLowerCase()) {
                            case "today":
                            case "daily":
                            case "day":
                                allStats = billRepository.getStaffBillingStatisticsForTodayByCompany(companyId, startOfDay, endOfDay);
                                break;
                            case "weekly":
                            case "week":
                                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                                LocalDate weekEnd = weekStart.plusDays(6);
                                LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
                                LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndCompany(companyId, weekStartDateTime, weekEndDateTime);
                                break;
                            case "monthly":
                            case "month":
                                allStats = billRepository.getStaffBillingStatisticsForCurrentMonthByCompany(companyId, startOfMonth, endOfMonth);
                                break;
                            case "custom":
                            case "range":
                                if (startDate == null || endDate == null) {
                                    return baseResponse.errorResponse(
                                            HttpStatus.BAD_REQUEST,
                                            "Start date and end date are required for custom date range"
                                    );
                                }
                                LocalDateTime startDateTime;
                                LocalDateTime endDateTime;

                                try {
                                    LocalDateTime[] dateTimes = DateUtils.getStartAndEndDateTime(startDate, endDate);
                                    startDateTime = dateTimes[0];
                                    endDateTime = dateTimes[1];
                                } catch (Exception e) {
                                    return baseResponse.errorResponse(
                                            HttpStatus.BAD_REQUEST,
                                            "Invalid date format. Please use either date format (yyyy-MM-dd) or datetime format (yyyy-MM-ddTHH:mm:ss)"
                                    );
                                }
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndCompany(companyId, startDateTime, endDateTime);
                                break;
                            default:
                                return baseResponse.errorResponse(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid date filter. Use 'today', 'daily', 'weekly', 'monthly', or 'custom'"
                                );
                        }
                    } else {
                        allStats = billRepository.getAllStaffBillingStatisticsByCompany(companyId);
                    }
                    logger.info("SAAS_ADMIN accessing staff billing overview for company ID: {} with date filter: {}", companyId, dateFilter);
                }

            }  else {
                // Other roles (DOCTOR, RECEPTIONIST, STAFF, BILLING_STAFF, PATIENT) - branch-wise only
                if (currentUser.getBranch() != null) {
                    Long branchIdCurrentUser = currentUser.getBranch().getId();
                    if (dateFilter != null) {
                        switch (dateFilter.toLowerCase()) {
                            case "today":
                            case "daily":
                            case "day":
                                allStats = billRepository.getStaffBillingStatisticsForTodayByBranch(branchIdCurrentUser, startOfDay, endOfDay);
                                break;
                            case "weekly":
                            case "week":
                                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                                LocalDate weekEnd = weekStart.plusDays(6);
                                LocalDateTime weekStartDateTime = weekStart.atStartOfDay();
                                LocalDateTime weekEndDateTime = weekEnd.atTime(23, 59, 59);
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndBranch(branchIdCurrentUser, weekStartDateTime, weekEndDateTime);
                                break;
                            case "monthly":
                            case "month":
                                allStats = billRepository.getStaffBillingStatisticsForCurrentMonthByBranch(branchIdCurrentUser, startOfMonth, endOfMonth);
                                break;
                            case "custom":
                            case "range":
                                if (startDate == null || endDate == null) {
                                    return baseResponse.errorResponse(
                                            HttpStatus.BAD_REQUEST,
                                            "Start date and end date are required for custom date range"
                                    );
                                }
                                LocalDateTime startDateTime;
                                LocalDateTime endDateTime;

                                try {
                                    LocalDateTime[] dateTimes = DateUtils.getStartAndEndDateTime(startDate, endDate);
                                    startDateTime = dateTimes[0];
                                    endDateTime = dateTimes[1];
                                } catch (Exception e) {
                                    return baseResponse.errorResponse(
                                            HttpStatus.BAD_REQUEST,
                                            "Invalid date format. Please use either date format (yyyy-MM-dd) or datetime format (yyyy-MM-ddTHH:mm:ss)"
                                    );
                                }
                                allStats = billRepository.getStaffBillingStatisticsByDateRangeAndBranch(branchIdCurrentUser, startDateTime, endDateTime);
                                break;
                            default:
                                return baseResponse.errorResponse(
                                        HttpStatus.BAD_REQUEST,
                                        "Invalid date filter. Use 'today', 'daily', 'weekly', 'monthly', or 'custom'"
                                );
                        }
                    } else {
                        allStats = billRepository.getAllStaffBillingStatisticsByBranch(branchIdCurrentUser);
                    }
                    logger.info("{} accessing staff billing overview for their branch ID: {} with date filter: {}", currentUser.getRole(), branchIdCurrentUser, dateFilter);
                } else {
                    logger.warn("{} user has no branch assignment", currentUser.getRole());
                    allStats = new ArrayList<>();
                }
            }

            List<Map<String, Object>> staffOverviews = allStats.stream()
                    .map(this::convertToStaffOverview)
                    .collect(Collectors.toList());

            logger.info("Fetched billing overview for {} staff members with date filter: {}, branchId: {}",
                    staffOverviews.size(), dateFilter, branchId);

            String message = "Staff billing overview fetched successfully";
            if (rbacUtil.isSuperAdmin()) {
                if (branchId != null) {
                    message += " for branch ID: " + branchId;
                } else {
                    message += " (system-wide access)";
                }
            } else if (rbacUtil.isAdmin()) {
                if (branchId != null) {
                    message += " for branch ID: " + branchId;
                } else {
                    message += " for company";
                }
            } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                message += " for branch";
            } else {
                message += " (role-restricted access)";
            }
            if (dateFilter != null) {
                message += " for " + dateFilter;
                if (dateFilter.equals("custom") && startDate != null && endDate != null) {
                    message += " (" + startDate + " to " + endDate + ")";
                }
            }

            return baseResponse.successResponse(message, staffOverviews);

        } catch (Exception e) {
            logger.error("Error while fetching staff billing overview by date range", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch staff billing overview at the moment. Please try again later."
            );
        }
    }
    @Override
    public ResponseEntity<?> getStaffBillingOverviewByDateRange(String dateFilter,
                                                                String startDate,
                                                                String endDate,
                                                                Long branchId) {

        User currentUser = DbUtill.getCurrentUser();
        logger.info("Fetching staff billing overview [filter={}, start={}, end={}, branchId={}]",
            dateFilter, startDate, endDate, branchId);

        try {

            DateRange range = resolveDateRange(dateFilter, startDate, endDate);

            logger.info(
                "Date filter '{}' resolved to range [start={}, end={}]",
                dateFilter,
                range.getStart(),
                range.getEnd()
            );

            List<Object[]> stats = fetchBillingStats(currentUser, branchId, range);

            List<Map<String, Object>> staffOverviews = stats.stream()
                .map(this::convertToStaffOverview)
                .collect(Collectors.toList());

            return baseResponse.successResponse(
                buildSuccessMessage(currentUser, branchId, dateFilter, startDate, endDate),
                staffOverviews
            );

        } catch (IllegalArgumentException e) {

            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {

            logger.error("Error fetching staff billing overview", e);

            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to fetch staff billing overview at the moment."
            );
        }
    }

    private DateRange resolveDateRange(String dateFilter, String startDate, String endDate) {

//        LocalDate today = DateUtils.getBusinessLocalDate();
        ZoneId indiaZone = ZoneId.of("Asia/Kolkata");
        ZoneId utcZone = ZoneId.of("UTC");
        LocalDate today = LocalDate.now(indiaZone);

        if (dateFilter == null) {
            return new DateRange(null, null);
        }

        switch (dateFilter.toLowerCase()) {

            case "today":
            case "daily":
            case "day":
            return new DateRange(
                    today.atStartOfDay(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime(),
                    today.atTime(LocalTime.MAX).atZone(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime()
            );

            case "weekly":
            case "week":

                LocalDate weekStart = today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
                LocalDate weekEnd = weekStart.plusDays(6);

                return new DateRange(
                        weekStart.atStartOfDay(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime(),
                        weekEnd.atTime(LocalTime.MAX).atZone(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime()
                );

            case "monthly":
            case "month":
                LocalDate startMonth = today.withDayOfMonth(1);
                LocalDate endMonth = today.withDayOfMonth(today.lengthOfMonth());
                return new DateRange(
                        startMonth.atStartOfDay(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime(),
                        endMonth.atTime(LocalTime.MAX).atZone(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime()
                );

            case "custom":
            case "range":

                if (startDate == null || endDate == null) {
                    throw new IllegalArgumentException(
                        "Start date and end date are required for custom date range"
                    );
                }

                try {
                    LocalDateTime[] dates = DateUtils.getStartAndEndDateTime(startDate, endDate);

                    return new DateRange(
                            dates[0].atZone(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime(),
                            dates[1].atZone(indiaZone).withZoneSameInstant(utcZone).toLocalDateTime()
                    );
                } catch (Exception e) {
                    throw new IllegalArgumentException(
                        "Invalid date format. Use yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss"
                    );
                }

            default:
                throw new IllegalArgumentException(
                    "Invalid date filter. Use today, weekly, monthly, or custom"
                );
        }
    }

    private List<Object[]> fetchBillingStats(User user, Long branchId, DateRange range) {

        if (rbacUtil.isSuperAdmin()) {
            return fetchSuperAdminStats(branchId, range);
        }

        if (rbacUtil.isAdmin()) {
            return fetchAdminStats(user, branchId, range);
        }

        return fetchBranchUserStats(user, range);
    }

    private List<Object[]> fetchSuperAdminStats(Long branchId, DateRange range) {

        if (branchId != null) {

            if (range.isAllTime())
                return billRepository.getAllStaffBillingStatisticsByBranch(branchId);

            return billRepository.getStaffBillingStatisticsByDateRangeAndBranch(
                branchId,
                range.getStart(),
                range.getEnd()
            );
        }

        if (range.isAllTime())
            return billRepository.getAllStaffBillingStatistics();

        return billRepository.getStaffBillingStatisticsByDateRange(
            range.getStart(),
            range.getEnd()
        );
    }

    private List<Object[]> fetchAdminStats(User user, Long branchId, DateRange range) {

        if (branchId != null) {

            Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() ->
                    new IllegalArgumentException("Branch not found: " + branchId));

            if (!isBranchAccessibleToUser(branch, user)) {
                throw new IllegalArgumentException(
                    "You don't have permission to access this branch"
                );
            }

            if (range.isAllTime())
                return billRepository.getAllStaffBillingStatisticsByBranch(branchId);

            return billRepository.getStaffBillingStatisticsByDateRangeAndBranch(
                branchId,
                range.getStart(),
                range.getEnd()
            );
        }

        Long companyId = DbUtill.getLoggedInCompanyId();

        if (range.isAllTime())
            return billRepository.getAllStaffBillingStatisticsByCompany(companyId);

        return billRepository.getStaffBillingStatisticsByDateRangeAndCompany(
            companyId,
            range.getStart(),
            range.getEnd()
        );
    }

    private List<Object[]> fetchBranchUserStats(User user, DateRange range) {

        if (user.getBranch() == null) {
            return new ArrayList<>();
        }

        Long branchId = user.getBranch().getId();

        if (range.isAllTime())
            return billRepository.getAllStaffBillingStatisticsByBranch(branchId);

        return billRepository.getStaffBillingStatisticsByDateRangeAndBranch(
            branchId,
            range.getStart(),
            range.getEnd()
        );
    }

    private String buildSuccessMessage(User currentUser,
                                       Long branchId,
                                       String dateFilter,
                                       String startDate,
                                       String endDate) {

        StringBuilder message = new StringBuilder("Staff billing overview fetched successfully");

        // Role based message
        if (rbacUtil.isSuperAdmin()) {

            if (branchId != null) {
                message.append(" for branch ID: ").append(branchId);
            } else {
                message.append(" (system-wide access)");
            }

        } else if (rbacUtil.isAdmin()) {

            if (branchId != null) {
                message.append(" for branch ID: ").append(branchId);
            } else {
                message.append(" for company");
            }

        } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {

            message.append(" for branch");

        } else {

            message.append(" (role-restricted access)");
        }

        // Date filter message
        if (dateFilter != null) {

            message.append(" for ").append(dateFilter);

            if ("custom".equalsIgnoreCase(dateFilter)
                && startDate != null
                && endDate != null) {

                message.append(" (")
                    .append(startDate)
                    .append(" to ")
                    .append(endDate)
                    .append(")");
            }
        }

        return message.toString();
    }

    @Override
    public ResponseEntity<?> getStaffBillingOverview(Long branchId) {
        User currentUser = DbUtill.getCurrentUser();
        logger.info("Request received to fetch staff billing overview[branchId={}]",
                branchId != null ? branchId : "all branches");

        try {
            List<Object[]> allStats;

            // Role-based access control for staff billing overview
            if (rbacUtil.isSuperAdmin()) {
                // SUPER_ADMIN and SUPER_ADMIN_MANAGER see all stats system-wide
                if (branchId != null) {
                    allStats = billRepository.getAllStaffBillingStatisticsByBranch(branchId);
                    logger.info("SUPER_ADMIN accessing staff billing overview for branch ID: {}", branchId);
                } else {
                    allStats = billRepository.getAllStaffBillingStatistics();
                    logger.info("SUPER_ADMIN accessing staff billing overview system-wide");
                }

            } else if (rbacUtil.isAdmin()) {
                // SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's billing overview");
                    }
                    allStats = billRepository.getAllStaffBillingStatisticsByBranch(branchId);
                    logger.info("SAAS_ADMIN accessing staff billing overview for branch ID: {}", branchId);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    allStats = billRepository.getAllStaffBillingStatisticsByCompany(companyId);
                    logger.info("SAAS_ADMIN accessing staff billing overview for company ID: {}", companyId);
                }

            } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                // BRANCH_MANAGER sees stats in their assigned branch only
                if (currentUser.getBranch() != null) {
                    allStats = billRepository.getAllStaffBillingStatisticsByBranch(currentUser.getBranch().getId());
                    logger.info("BRANCH_MANAGER accessing staff billing overview for branch ID: {}", currentUser.getBranch().getId());
                } else {
                    logger.warn("BRANCH_MANAGER user has no branch assignment");
                    allStats = new ArrayList<>();
                }

            } else {
                // Other roles (DOCTOR, RECEPTIONIST, STAFF, BILLING_STAFF, PATIENT) - branch-wise only
                if (currentUser.getBranch() != null) {
                    allStats = billRepository.getAllStaffBillingStatisticsByBranch(currentUser.getBranch().getId());
                    logger.info("{} accessing staff billing overview for their branch ID: {}", currentUser.getRole(), currentUser.getBranch().getId());
                } else {
                    logger.warn("{} user has no branch assignment", currentUser.getRole());
                    allStats = new ArrayList<>();
                }
            }

            List<Map<String, Object>> staffOverviews = allStats.stream()
                    .map(this::convertToStaffOverview)
                    .collect(Collectors.toList());

            logger.info("Fetched billing overview for {} staff members{}",
                    staffOverviews.size(),
                    branchId != null ? " for branch ID: " + branchId : " for all branches");

            String message = "Staff billing overview fetched successfully";
            if (rbacUtil.isSuperAdmin()) {
                if (branchId != null) {
                    message += " for branch ID: " + branchId + " (system-wide access)";
                } else {
                    message += " (system-wide access)";
                }
            } else if (rbacUtil.isAdmin()) {
                if (branchId != null) {
                    message += " for branch ID: " + branchId;
                } else {
                    message += " for company";
                }
            } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                message += " for branch";
            } else {
                message += " (role-restricted access)";
            }

            return baseResponse.successResponse(message, staffOverviews);

        } catch (Exception e) {
            logger.error("Error while fetching staff billing overview", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch staff billing overview at the moment. Please try again later."
            );
        }
    }

    private Map<String, Object> convertToStaffOverview(Object[] stats) {
        Long staffId = ((Number) stats[0]).longValue();
        User staff = userRepository.findById(staffId).orElse(null);

        String staffName = "Unknown Staff";
        if (staff != null) {
            staffName = staff.getFirstName() + " " + staff.getLastName();
        }

        Long totalBills = ((Number) stats[1]).longValue();
        BigDecimal totalAmountBilled = stats[2] != null ? convertToBigDecimal(stats[2]) : BigDecimal.ZERO;
        BigDecimal totalAmountCollected = stats[3] != null ? convertToBigDecimal(stats[3]) : BigDecimal.ZERO;
        BigDecimal totalAmountPending = stats[4] != null ? convertToBigDecimal(stats[4]) : BigDecimal.ZERO;
        Long totalPaidBills = ((Number) stats[5]).longValue();
        Long totalPendingBills = ((Number) stats[6]).longValue();
        Long totalPartiallyPaidBills = ((Number) stats[7]).longValue();
        Long totalCancelledBills = ((Number) stats[8]).longValue();


        double collectionRate = 0.0;
        if (totalAmountBilled.compareTo(BigDecimal.ZERO) > 0) {
            collectionRate = (totalAmountCollected.doubleValue() / totalAmountBilled.doubleValue()) * 100;
        }

        Map<String, Object> result = new HashMap<>();
        result.put("staffId", staffId);
        result.put("staffName", staffName);
        result.put("staffRole", staff != null ? staff.getRole().name() : "UNKNOWN");
        result.put("totalBillsCreated", totalBills);
        result.put("totalAmountBilled", totalAmountBilled);
        result.put("totalAmountCollected", totalAmountCollected);
        result.put("totalAmountPending", totalAmountPending);
        result.put("totalPaidBills", totalPaidBills);
        result.put("totalPendingBills", totalPendingBills);
        result.put("totalPartiallyPaidBills", totalPartiallyPaidBills);
        result.put("totalCancelledBills", totalCancelledBills);
        result.put("collectionRate", collectionRate);

        return result;
    }

    /**
     * Parse date string to LocalDateTime, handling both date-only and datetime formats
     *
     * @param dateStr Date string in either yyyy-MM-dd or yyyy-MM-ddTHH:mm:ss format
     * @return LocalDateTime object
     * @throws Exception if parsing fails
     */
    private LocalDateTime parseDateTime(String dateStr) throws Exception {
        try {
            // Try parsing as full datetime first
            return LocalDateTime.parse(dateStr);
        } catch (Exception e) {
            // If that fails, try parsing as date only and convert to start of day
            try {
                LocalDate date = LocalDate.parse(dateStr);
                return date.atStartOfDay();
            } catch (Exception ex) {
                // Try parsing as date only and convert to end of day (for end date)
                try {
                    LocalDate date = LocalDate.parse(dateStr);
                    return date.atTime(23, 59, 59);
                } catch (Exception ex2) {
                    throw new Exception("Unable to parse date: " + dateStr);
                }
            }
        }
    }

    @Override
    public ResponseEntity<?> generateReceipt(Long billId) {
        logger.info("Request received to generate receipt for billId={}", billId);

        try {
            Bill bill = getBillOrThrow(billId);

            // Use the bill entity directly instead of converting to DTO first
            Customer patient = bill.getPatient();
            User billingStaff = bill.getBillingStaff();
            Appointment appointment = bill.getAppointment();
            CompanyProfile companyProfile = bill.getCompany();
            Branch branch = bill.getBranch();

            List<BillItem> billItems = bill.getBillItems();
            if (billItems == null || billItems.isEmpty()) {
                // Fallback to repository method
                billItems = billItemRepository.findByBillId(billId);
                logger.info("Fetched {} bill items from repository for billId={}", billItems.size(), billId);
            } else {
                logger.info("Found {} bill items from entity relationship for billId={}", billItems.size(), billId);
            }

            if (billItems == null) {
                billItems = new ArrayList<>();
                logger.warn("No bill items found for billId={} after all attempts", billId);
            }

            // Create receipt data map
            Map<String, Object> receiptData = new HashMap<>();

            // Patient Information
            receiptData.put("patientName", patient.getFirstName() + " " + patient.getLastName());
            receiptData.put("patientAge", calculateAge(patient.getDateOfBirth()));
            receiptData.put("patientAddress", patient.getAddress() != null ? patient.getAddress() : "N/A");
            String patientContact = "";
            if (patient.getPhoneNumber() != null) {
                patientContact = patient.getPhoneNumber();
            }
            if (patient.getEmail() != null) {
                patientContact += (patientContact.isEmpty() ? "" : " / ") + patient.getEmail();
            }
            receiptData.put("patientContact", patientContact.isEmpty() ? "N/A" : patientContact);
            receiptData.put("patientGender", patient.getGender() != null ? patient.getGender() : "N/A");
            receiptData.put("patientUHID", patient.getUhid() != null ? patient.getUhid() : "N/A");

            // Doctor/Appointment Information (if available)
            if (appointment != null && appointment.getDoctor() != null) {
                Doctor doctor = appointment.getDoctor();
                receiptData.put("doctorName", doctor.getDoctorName());
                receiptData.put("doctorSpecialization", doctor.getQualification() != null ? doctor.getQualification() : "" +
                        (doctor.getSpecialization() != null ? ", " + doctor.getSpecialization() : ""));
                receiptData.put("doctorRegistrationNo", doctor.getRegistrationNumber() != null ? doctor.getRegistrationNumber() : "N/A");
                String doctorContact = "";
                if (doctor.getPhoneNumber() != null) {
                    doctorContact = doctor.getPhoneNumber();
                }
                if (doctor.getDoctorEmail() != null) {
                    doctorContact += (doctorContact.isEmpty() ? "" : " / ") + doctor.getDoctorEmail();
                }
                receiptData.put("doctorContact", doctorContact.isEmpty() ? "N/A" : doctorContact);
                receiptData.put("doctorDepartment", doctor.getDepartment() != null ? doctor.getDepartment() : "N/A");
                receiptData.put("doctorConsultationFee", doctor.getConsultationFee() != null ? doctor.getConsultationFee().toString() : "N/A");
            } else {
                // If no doctor in appointment, use department from patient
                if (patient.getDepartment() != null) {
                    receiptData.put("doctorName", patient.getDepartment().getDepartmentName());
                    receiptData.put("doctorSpecialization", "Department");
                } else {
                    receiptData.put("doctorName", "N/A");
                    receiptData.put("doctorSpecialization", "N/A");
                }
                receiptData.put("doctorRegistrationNo", "N/A");
                receiptData.put("doctorContact", "N/A");
                receiptData.put("doctorDepartment", "N/A");
                receiptData.put("doctorConsultationFee", "N/A");
            }

            // Company Information
            if (companyProfile != null) {
                receiptData.put("companyName", companyProfile.getCompanyName() != null ? companyProfile.getCompanyName() : "N/A");
                receiptData.put("companyGstin", companyProfile.getGstNumber() != null ? companyProfile.getGstNumber() : "N/A");
                receiptData.put("companyAddress", companyProfile.getAddressLine1() != null ? companyProfile.getAddressLine1() : "N/A");
                String companyContact = "";
                if (companyProfile.getPhoneNumber() != null) {
                    companyContact = companyProfile.getPhoneNumber();
                }
                if (companyProfile.getEmail() != null) {
                    companyContact += (companyContact.isEmpty() ? "" : " / ") + companyProfile.getEmail();
                }
                receiptData.put("companyContact", companyContact.isEmpty() ? "N/A" : companyContact);

                // Get state information
                String state = companyProfile.getState() != null ? companyProfile.getState() : "West Bengal";
                String stateCode = getStateCode(state);
                receiptData.put("stateCode", stateCode);
                receiptData.put("placeOfSupply", state);

                // Additional company info
                receiptData.put("companyPan", companyProfile.getPanNumber() != null ? companyProfile.getPanNumber() : "N/A");
                receiptData.put("companyRegistration", companyProfile.getRegistrationNumber() != null ? companyProfile.getRegistrationNumber() : "N/A");
                receiptData.put("companyWebsite", companyProfile.getWebsite() != null ? companyProfile.getWebsite() : "N/A");
            } else {
                receiptData.put("companyName", "N/A");
                receiptData.put("companyGstin", "N/A");
                receiptData.put("companyAddress", "N/A");
                receiptData.put("companyContact", "N/A");
                receiptData.put("stateCode", "19");
                receiptData.put("placeOfSupply", "West Bengal");
                receiptData.put("companyPan", "N/A");
                receiptData.put("companyRegistration", "N/A");
                receiptData.put("companyWebsite", "N/A");
            }

            // Branch Information
            if (branch != null) {
                receiptData.put("branchName", branch.getBranchName() != null ? branch.getBranchName() : "N/A");
                receiptData.put("branchAddress", branch.getAddress() != null ? branch.getAddress() : "N/A");
                String branchContact = "";
                if (branch.getPhoneNumber() != null) {
                    branchContact = branch.getPhoneNumber();
                }
                if (branch.getEmail() != null) {
                    branchContact += (branchContact.isEmpty() ? "" : " / ") + branch.getEmail();
                }
                receiptData.put("branchContact", branchContact.isEmpty() ? "N/A" : branchContact);
                receiptData.put("branchEstablishedDate", branch.getEstablishedDate() != null ? branch.getEstablishedDate() : "N/A");

                // Add company name if branch has clinic reference
                if (branch.getClinic() != null && branch.getClinic().getCompanyName() != null) {
                    receiptData.put("branchCompanyName", branch.getClinic().getCompanyName());
                } else {
                    receiptData.put("branchCompanyName", "N/A");
                }
            } else {
                receiptData.put("branchName", "N/A");
                receiptData.put("branchAddress", "N/A");
                receiptData.put("branchContact", "N/A");
                receiptData.put("branchEstablishedDate", "N/A");
                receiptData.put("branchCompanyName", "N/A");
            }

            // Bill Information
            receiptData.put("billId", bill.getId());
            receiptData.put("billNumber", bill.getBillNumber());
            receiptData.put("billDate", bill.getCreatedAt() != null ?
                    bill.getCreatedAt().toString() : "N/A");
            receiptData.put("paymentDate", bill.getPaymentDate() != null ?
                    bill.getPaymentDate().toString() : "N/A");
            receiptData.put("billStatus", bill.getStatus() != null ? bill.getStatus().name() : "N/A");
            receiptData.put("totalAmount", bill.getTotalAmount());
            receiptData.put("paidAmount", bill.getPaidAmount());
            receiptData.put("balanceAmount", bill.getBalanceAmount());
            receiptData.put("dueAmount", bill.getBalanceAmount()); // Explicit due amount field

            // Bill Items with detailed information
            List<Map<String, Object>> items = new ArrayList<>();
            BigDecimal totalDiscount = BigDecimal.ZERO;

            for (int i = 0; i < billItems.size(); i++) {
                BillItem item = billItems.get(i);
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("serialNumber", i + 1);
                itemMap.put("description", item.getItemName() != null ? item.getItemName() : "N/A");
                itemMap.put("itemDescription", item.getItemDescription() != null ? item.getItemDescription() : "N/A");
                itemMap.put("quantity", item.getQuantity() != null ? item.getQuantity() : 1);
                itemMap.put("unitPrice", item.getUnitPrice() != null ? item.getUnitPrice() : BigDecimal.ZERO);
                BigDecimal totalPrice = item.getTotalPrice() != null ? item.getTotalPrice() : BigDecimal.ZERO;
                itemMap.put("totalPrice", totalPrice);
                itemMap.put("fee", totalPrice); // For ReceiptFormatter compatibility

                // Calculate discount based on item type or position
                BigDecimal discount = BigDecimal.ZERO;
                if (item.getItemName() != null && item.getItemName().toLowerCase().contains("discount")) {
                    // If item name contains "discount", treat it as discount
                    discount = item.getTotalPrice() != null ? item.getTotalPrice().abs() : BigDecimal.ZERO;
                    if (item.getTotalPrice() != null && item.getTotalPrice().compareTo(BigDecimal.ZERO) > 0) {
                        discount = discount.negate(); // Make it negative for discount
                    }
                } else if (i > 0) {
                    // Apply 10% discount on items after first one as example
                    discount = item.getTotalPrice() != null ?
                            item.getTotalPrice().multiply(BigDecimal.valueOf(0.10)).setScale(2, RoundingMode.HALF_UP) :
                            BigDecimal.ZERO;
                    discount = discount.negate(); // Make it negative
                }

                itemMap.put("discount", discount);
                itemMap.put("amount", item.getTotalPrice() != null ? item.getTotalPrice().add(discount) : BigDecimal.ZERO);

                // Add tax fields for ReceiptFormatter compatibility (set to zero for healthcare)
                itemMap.put("igst", BigDecimal.ZERO);
                itemMap.put("cgst", BigDecimal.ZERO);
                itemMap.put("sgst", BigDecimal.ZERO);
                itemMap.put("refund", BigDecimal.ZERO);

                // Add HSN code for healthcare services
                itemMap.put("hsnCode", "9993"); // Healthcare services HSN code

                items.add(itemMap);
                totalDiscount = totalDiscount.add(discount);
            }

            // Update total amounts based on discounts
            receiptData.put("items", items);
            receiptData.put("totalItems", items.size());

            BigDecimal originalTotal = bill.getTotalAmount() != null ? bill.getTotalAmount().subtract(totalDiscount) : BigDecimal.ZERO;
            receiptData.put("originalTotal", originalTotal);
            receiptData.put("totalDiscountApplied", totalDiscount.abs());

            // Calculate totals for GST and other taxes
            BigDecimal totalIGST = BigDecimal.ZERO;
            BigDecimal totalCGST = BigDecimal.ZERO;
            BigDecimal totalSGST = BigDecimal.ZERO;
            BigDecimal totalDiscountCalculated = BigDecimal.ZERO;
            BigDecimal totalRefund = BigDecimal.ZERO;

            for (Map<String, Object> item : items) {
                // Healthcare services typically don't have GST, but keeping structure for flexibility
                totalIGST = totalIGST.add(BigDecimal.ZERO);
                totalCGST = totalCGST.add(BigDecimal.ZERO);
                totalSGST = totalSGST.add(BigDecimal.ZERO);
                totalDiscountCalculated = totalDiscountCalculated.add((BigDecimal) item.get("discount"));
                totalRefund = totalRefund.add(BigDecimal.ZERO);
            }

            receiptData.put("totalIGST", totalIGST);
            receiptData.put("totalCGST", totalCGST);
            receiptData.put("totalSGST", totalSGST);
            receiptData.put("totalDiscount", totalDiscountCalculated.abs());
            receiptData.put("totalRefund", totalRefund);

            // Additional billing staff information
            receiptData.put("billingStaffName", billingStaff.getFirstName() + " " + billingStaff.getLastName());
            receiptData.put("billingStaffRole", billingStaff.getRole() != null ? billingStaff.getRole().name() : "N/A");
            receiptData.put("billingStaffId", billingStaff.getId());

            // Payment method information (if available in future)
            receiptData.put("paymentMethod", "N/A");
            receiptData.put("transactionId", "N/A");

            logger.info("Receipt generated successfully for billId={} with {} items", billId, items.size());
            logger.debug("Receipt data keys: {}", receiptData.keySet());
            logger.debug("Sample item data: {}", items.isEmpty() ? "No items" : items.get(0));
            return baseResponse.successResponse("Receipt generated successfully", receiptData);

        } catch (IllegalArgumentException e) {
            logger.warn("Receipt generation failed | billId={} | reason={}", billId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Unexpected error while generating receipt for billId={}", billId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to generate receipt at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllPaymentType() {
        try {
            List<PaymentTypeDTO> paymentTypes = Arrays.stream(PaymentType.values())
                    .map(type -> new PaymentTypeDTO(
                            type.name(),
                            type.name().replace("_", " ")
                    ))
                    .sorted(Comparator.comparing(PaymentTypeDTO::getLabel))
                    .toList();

            return baseResponse.successResponse(
                    "Payment types fetched successfully",
                    paymentTypes
            );

        } catch (Exception e) {
            logger.error("Error while fetching payment types", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch payment types. Please try again later."
            );
        }
    }

    /**
     * Fetch payment statistics grouped by payment type within a given date range.
     *
     * <p>This API supports role-based access:</p>
     * <ul>
     *     <li><b>Super Admin</b> → Fetch stats for all companies.</li>
     *     <li><b>Admin</b> → Fetch stats for their company.</li>
     *     <li><b>Branch User</b> → Fetch stats for a specific branch.</li>
     * </ul>
     *
     * <p>The statistics include:</p>
     * <ul>
     *     <li>Total transactions per payment type</li>
     *     <li>Total paid amount per payment type</li>
     * </ul>
     *
     * @param startDate start date in string format
     * @param endDate end date in string format
     * @param branchId branch ID (optional for branch-level users)
     * @return ResponseEntity containing payment statistics
     */
    @Override
    public ResponseEntity<?> getPaymentTypeStats(String startDate, String endDate, Long branchId) {

        logger.info("Fetching payment type stats | startDate: {}, endDate: {}, branchId: {}",
            startDate, endDate, branchId);

        try {
            User currentUser = DbUtill.getCurrentUser();
            Long[] dateRange = DateUtils.getStartAndEndDateInMilli(startDate, endDate);

            Long startMs = dateRange[0];
            Long endMs = dateRange[1];

            // Super Admin → All companies
            if (rbacUtil.isSuperAdmin()) {
                logger.info("SuperAdmin requesting global payment stats");
                return baseResponse.successResponse(
                    "Payment stats fetched successfully",
                    billRepository.getPaymentStats(startMs, endMs)
                );
            }

            // Admin → Company level
            if (rbacUtil.isAdmin()) {
                // If branchId is provided → branch stats
                if (branchId != null) {

                    if (!branchRepository.existsById(branchId)) {
                        logger.warn("Invalid branchId received: {}", branchId);
                        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid branchId");
                    }

                    logger.info("Admin requesting branch payment stats | branchId: {}", branchId);

                    return baseResponse.successResponse(
                        "Branch payment stats fetched successfully",
                        billRepository.getBranchPaymentStats(branchId, startMs, endMs)
                    );
                }

                // If branchId NOT provided → company stats
                Long companyId = currentUser.getCompany().getId();

                logger.info("Admin requesting company payment stats | companyId: {}", companyId);

                return baseResponse.successResponse(
                    "Company payment stats fetched successfully",
                    billRepository.getCompanyPaymentStats(companyId, startMs, endMs)
                );
            } else if (branchId != null) {
                if (!branchRepository.existsById(branchId)) {
                    logger.warn("Invalid branchId received: {}", branchId);
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Invalid branchId");
                }
                return baseResponse.successResponse(
                    "Branch payment stats fetched successfully",
                    billRepository.getBranchPaymentStats(branchId, startMs, endMs)
                );
            } else {
                branchId = currentUser.getBranch().getId();
                logger.info("Branch level payment stats requested | branchId: {}", branchId);

                return baseResponse.successResponse(
                    "Branch payment stats fetched successfully",
                    billRepository.getBranchPaymentStats(branchId, startMs, endMs)
                );
            }

        } catch (Exception e) {
            logger.error("Error while fetching payment stats", e);
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to fetch payment statistics"
            );
        }
    }

    /**
     * Calculate age from date of birth
     * @param dateOfBirth Date of birth string
     * @return Age string in format "X Y Y M D D" (Years Months Days)
     */
    /**
     * Calculate summary statistics from page content for date-filtered results
     */
    private Object[] calculateSummaryFromPageContent(List<Bill> bills) {
        if (bills == null || bills.isEmpty()) {
            return new Object[]{BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0L, 0L, 0L, 0L, 0L};
        }

        BigDecimal totalBilled = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalDue = BigDecimal.ZERO;
        long totalBills = 0L;
        long paidBills = 0L;
        long pendingBills = 0L;
        long partiallyPaidBills = 0L;
        long cancelledBills = 0L;

        for (Bill bill : bills) {
            totalBilled = totalBilled.add(bill.getTotalAmount() != null ? bill.getTotalAmount() : BigDecimal.ZERO);
            totalCollected = totalCollected.add(bill.getPaidAmount() != null ? bill.getPaidAmount() : BigDecimal.ZERO);
            totalDue = totalDue.add(bill.getBalanceAmount() != null ? bill.getBalanceAmount() : BigDecimal.ZERO);
            totalBills++;

            switch (bill.getStatus()) {
                case PAID:
                    paidBills++;
                    break;
                case PENDING:
                    pendingBills++;
                    break;
                case PARTIALLY_PAID:
                    partiallyPaidBills++;
                    break;
                case CANCELLED:
                    cancelledBills++;
                    break;
            }
        }

        return new Object[]{totalBilled, totalCollected, totalDue, totalBills, paidBills, pendingBills, partiallyPaidBills, cancelledBills};
    }

    private String calculateAge(String dateOfBirth) {
        if (dateOfBirth == null || dateOfBirth.isEmpty()) {
            return "N/A";
        }

        try {
            // Parse date of birth - assuming format like YYYY-MM-DD
            LocalDate dob = LocalDate.parse(dateOfBirth);
            LocalDate currentDate = DateUtils.getBusinessLocalDate();

            long years = Period.between(dob, currentDate).getYears();
            long months = Period.between(dob, currentDate).getMonths();
            long days = Period.between(dob, currentDate).getDays();

            return years + "Y " + months + "M " + days + "D";
        } catch (Exception e) {
            logger.warn("Could not parse date of birth: {}", dateOfBirth);
            return "N/A";
        }
    }

    /**
     * Get state code for GST purposes
     *
     * @param state State name
     * @return State code
     */
    private String getStateCode(String state) {
        if (state == null) return "19"; // Default to West Bengal

        switch (state.toLowerCase()) {
            case "west bengal":
                return "19";
            case "maharashtra":
                return "27";
            case "karnataka":
                return "29";
            case "tamil nadu":
                return "33";
            case "delhi":
                return "07";
            case "gujarat":
                return "24";
            case "uttar pradesh":
                return "09";
            case "rajasthan":
                return "08";
            case "madhya pradesh":
                return "23";
            case "andhra pradesh":
                return "37";
            case "kerala":
                return "32";
            case "punjab":
                return "03";
            case "haryana":
                return "06";
            case "bihar":
                return "10";
            case "odisha":
                return "21";
            case "chhattisgarh":
                return "22";
            case "jharkhand":
                return "20";
            case "uttarakhand":
                return "05";
            case "himachal pradesh":
                return "02";
            case "assam":
                return "18";
            case "meghalaya":
                return "17";
            case "nagaland":
                return "13";
            case "manipur":
                return "14";
            case "mizoram":
                return "15";
            case "tripura":
                return "16";
            case "sikkim":
                return "11";
            case "arunachal pradesh":
                return "12";
            case "goa":
                return "30";
            case "telangana":
                return "36";
            case "jammu and kashmir":
                return "01";
            case "ladakh":
                return "01";
            default:
                return "19"; // Default to West Bengal
        }
    }

    /**
     * Check if a branch is accessible to the current user based on their role
     *
     * @param branch      The branch to check
     * @param currentUser The current user
     * @return true if accessible, false otherwise
     */
    private boolean isBranchAccessibleToUser(Branch branch, User currentUser) {
        // Super admins have access to all branches
        if (rbacUtil.isSuperAdmin()) {
            return true;
        }

        // SAAS_ADMIN and SAAS_ADMIN_MANAGER have access to branches in their company
        if (currentUser.getRole() == UserRole.SAAS_ADMIN ||
                currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
            if (currentUser.getCompany() != null && branch.getClinic() != null) {
                return currentUser.getCompany().getId().equals(branch.getClinic().getId());
            }
            return false;
        }

        // BRANCH_MANAGER and other roles can only access their assigned branch
        if (currentUser.getBranch() != null) {
            return currentUser.getBranch().getId().equals(branch.getId());
        }

        return false;
    }


    private PatientBillWithPackageDTO mapToPatientBillWithPackageDTO(Bill bill) {
        PatientBillWithPackageDTO dto = new PatientBillWithPackageDTO();
        dto.setBillId(bill.getId());
        dto.setBillNumber(bill.getBillNumber());
        dto.setCreatedAt(bill.getCreatedAt());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setPaidAmount(bill.getPaidAmount());
        dto.setBalanceAmount(bill.getBalanceAmount());
        dto.setStatus(bill.getStatus() != null ? bill.getStatus().name() : null);
        dto.setPaymentType(bill.getPaymentType() != null ? bill.getPaymentType().name() : null);

        List<PatientBillWithPackageDTO.PatientBillItemWithPackageDTO> itemDTOs = new ArrayList<>();
        if (bill.getBillItems() != null) {
            for (BillItem item : bill.getBillItems()) {
                PatientBillWithPackageDTO.PatientBillItemWithPackageDTO itemDTO =
                        new PatientBillWithPackageDTO.PatientBillItemWithPackageDTO();
                itemDTO.setItemId(item.getId());
                itemDTO.setItemName(item.getItemName());
                itemDTO.setItemDescription(item.getItemDescription());
                itemDTO.setQuantity(item.getQuantity());
                itemDTO.setUnitPrice(item.getUnitPrice());
                itemDTO.setTotalPrice(item.getTotalPrice());
                itemDTOs.add(itemDTO);
            }
        }
        dto.setBillItems(itemDTOs);

        List<PatientPackageUsage> activeUsages =
                patientPackageUsageRepository.findActiveUsagesByPatientId(bill.getPatient().getId());

        if (!activeUsages.isEmpty()) {
            PatientPackageUsage latestUsage = activeUsages.get(0);
            PackageUsageInfoDTO usageDTO =
                    new PackageUsageInfoDTO();
            usageDTO.setPackageUsageId(latestUsage.getId());
            usageDTO.setTreatmentPackageId(latestUsage.getTreatmentPackage().getId());
            usageDTO.setPackageName(latestUsage.getTreatmentPackage().getName());
            usageDTO.setTreatmentName(latestUsage.getTreatment().getName());
            usageDTO.setTotalSessions(latestUsage.getTotalSessions());
            usageDTO.setSessionsUsed(latestUsage.getSessionsUsed());
            usageDTO.setSessionsRemaining(latestUsage.getSessionsRemaining());
            usageDTO.setCompleted(latestUsage.getCompleted());
            usageDTO.setPurchaseDate(latestUsage.getPurchaseDate());
            usageDTO.setLastSessionDate(latestUsage.getLastSessionDate());
            dto.setPackageUsage(usageDTO);
        }

        return dto;
    }

}