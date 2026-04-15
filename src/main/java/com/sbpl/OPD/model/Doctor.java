package com.sbpl.OPD.model;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

/**
 * Represents a doctor in the Hospital Management System.
 *
 * This entity stores professional, registration, and operational
 * details of a doctor. Each doctor is linked to a user account
 * for authentication and authorization purposes.
 *
 * Database indexes are defined to support efficient lookups by
 * specialization, registration number, availability, and active status.
 *
 * @author Rahul Kumar
 */

@Entity
@Getter
@Setter
@Table(name = "doctors", schema = "sb_opd", indexes = {
        @Index(name = "idx_doctor_user_id", columnList = "user_id"),
        @Index(name = "idx_doctor_specialization", columnList = "specialization"),
        @Index(name = "idx_doctor_registration_no", columnList = "registration_number", unique = true),
        @Index(name = "idx_doctor_active", columnList = "is_active")

})
public class Doctor extends BaseEntity {

    @Column(name = "prefix")
    private String prefix;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "created_by", nullable = false)
    private Long createdBy;

    @Column(name = "doctor_name", nullable = false)
    private String doctorName;

    @Column(name = "doctor_email",nullable = false)
    private String doctorEmail;

    @Column(name = "phone_number",nullable = false)
    private String phoneNumber;

    @Column(name = "gender",nullable = false)
    private String gender;

    @Column(name = "date_of_birth")
    private String dateOfBirth;

    @Column(nullable = false)
    private String specialization;
    @Column(name = "sub_specialization")
    private String subSpecialization;

    @Column(name = "registration_number", nullable = false, unique = true)
    private String registrationNumber;   // Medical Council Reg No

    private Integer experienceYears;

    private String qualification;   // MBBS, MD, MS, DM etc.
    private String university;

    private String department;
    private String consultationRoom;
    private String shiftTiming;   // Morning / Evening / Night

    private Double consultationFee;
    private Boolean onlineConsultationAvailable = false;

    @Column(name = "doctor_sign_url")
    private String doctorSignUrl;


    @Column(name = "is_active")
    private Boolean isActive = true;

    private LocalDate joiningDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyProfile company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
        name = "doctor_expertise_mapping",
        schema = "sb_opd",
        joinColumns = @JoinColumn(name = "doctor_id"),
        inverseJoinColumns = @JoinColumn(name = "expertise_id")
    )
    private List<DoctorCoreExpertise> coreExpertiseList;
}