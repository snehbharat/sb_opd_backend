package com.sbpl.OPD.model;


import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.enums.PaymentType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 Represents a billing record in the Hospital Management System.
 *
 * This entity stores financial details related to a patient's visit,
 * including billing staff, appointment reference, bill status,
 * payment details, and itemized charges.
 *
 * The bill is linked to a patient and may optionally be linked
 * to an appointment. It supports partial and full payments.
 *
 * Database indexes are designed to optimize reporting,
 * payment tracking, and patient-wise billing queries.
 *
 * @author Rahul Kumar
 */
@Entity
@Getter
@Setter
@Table(name = "bills", schema = "sb_opd", indexes = {
    @Index(name = "idx_bill_patient_status_created", columnList = "patient_id, status, created_at"),
    @Index(name = "idx_bill_billing_staff_created", columnList = "billing_staff_id, created_at"),
    @Index(name = "idx_bill_appointment", columnList = "appointment_id"),
    @Index(name = "idx_bill_created_at", columnList = "created_at"),
    @Index(name = "idx_bill_payment_date_status", columnList = "payment_date, status"),
    @Index(name = "idx_bill_branch_status_created_ms", columnList = "branch_id, status, created_at_ms"),
    @Index(name = "idx_bill_company_status_created_ms", columnList = "company_id, status, created_at_ms"),
    @Index(name = "idx_bill_status_created_ms", columnList = "status, created_at_ms"),
    @Index(name = "idx_bill_status_created", columnList = "status, created_at")})
public class Bill extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Customer patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "billing_staff_id", nullable = false)
    private User billingStaff;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Enumerated(EnumType.STRING)
    private BillStatus status;

    @Column(name = "payment_type")
    @Enumerated(EnumType.STRING)
    private PaymentType paymentType;

    @OneToMany(mappedBy = "bill", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<BillItem> billItems;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal balanceAmount;

    private BigDecimal previousDue;

    @Column(name = "coupon_code")
    private String couponCode;

    @Column(name = "coupon_amount")
    private BigDecimal couponAmount;

    private LocalDateTime paymentDate;

    private String notes;

    private String billNumber;

    // Company and Branch information for billing tracking
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyProfile company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
}