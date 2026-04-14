package com.sbpl.OPD.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Represents a customer in the system.
 * <p>
 * This entity stores personal and contact information for customers.
 * It extends a base entity to inherit common audit fields such as
 * identifier, creation time, and last update time.
 * <p>
 * Database indexes are defined to optimize searches by phone number,
 * email, and customer name.
 *
 * @author Rahul Kumar
 */

@Entity
@Table(name = "customers",
        schema = "sb_opd",
        indexes = {
                @Index(name = "idx_customer_phone", columnList = "phone_number"),
                @Index(name = "idx_customer_email", columnList = "email"),
                @Index(name = "idx_customer_first_name", columnList = "firstName"),
                @Index(name = "idx_customer_last_name", columnList = "lastName")
        })
@JsonIgnoreProperties(ignoreUnknown = true, value = {"hibernateLazyInitializer", "handler"})
@Getter
@Setter
public class Customer extends BaseEntity {

    @Column(name = "prefix")
    private String prefix;

    @Column(name = "uhid", unique = true)
    private String uhid;

    @Column(name = "first_name", nullable = false)
    private String firstName;

    @Column(name = "last_name", nullable = false)
    private String lastName;

    @Column(name = "phone_number", nullable = false)
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "age")
    private Long age;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(length = 1000)
    private String address;

    @Column(name = "gender")
    private String gender;

    @Column(name = "total_paid_amount")
    private BigDecimal totalPaidAmount;

    @Column(name = "total_due_amount")
    private BigDecimal totalDueAmount;

    @Column(name = "total_bill_amount")
    private BigDecimal totalBillAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "department_id")
    private Department department;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyProfile company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;
}