package com.sbpl.OPD.model;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents company profile information for the healthcare organization.
 * Stores organization details, contact information, and configuration.
 *
 * @author Rahul Kumar
 */
@Entity
@Table(name = "company_profiles",schema = "sb_opd")
@Getter
@Setter
public class CompanyProfile extends BaseEntity {

    @Column(name = "company_name", nullable = false)
    private String companyName;

    @Column(name = "company_code", unique = true)
    private String companyCode;

    @Column(name = "registration_number")
    private String registrationNumber;

    @Column(name = "gst_number")
    private String gstNumber;

    @Column(name = "pan_number")
    private String panNumber;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "alternate_phone")
    private String alternatePhone;

    @Column(name = "website")
    private String website;

    @Column(name = "address_line1")
    private String addressLine1;

    @Column(name = "address_line2")
    private String addressLine2;

    @Column(name = "city")
    private String city;

    @Column(name = "state")
    private String state;

    @Column(name = "country")
    private String country = "India";

    @Column(name = "pincode")
    private String pincode;

    @Column(name = "logo_url")
    private String logoUrl;

    @Column(name = "favicon_url")
    private String faviconUrl;

    @Column(name = "primary_color")
    private String primaryColor = "#1976D2";

    @Column(name = "secondary_color")
    private String secondaryColor = "#FFC107";

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "established_year")
    private Integer establishedYear;

    @Column(name = "about_company", length = 2000)
    private String aboutCompany;

    @Column(name = "mission_statement", length = 1000)
    private String missionStatement;

    @Column(name = "vision_statement", length = 1000)
    private String visionStatement;

    @Column(name = "contact_person_name")
    private String contactPersonName;

    @Column(name = "contact_person_designation")
    private String contactPersonDesignation;

    @Column(name = "contact_person_email")
    private String contactPersonEmail;

    @Column(name = "contact_person_phone")
    private String contactPersonPhone;

    @Column(name = "working_hours")
    private String workingHours;

    @Column(name = "holidays")
    private String holidays;

    @Column(name = "time_zone")
    private String timeZone = "Asia/Kolkata";

    @Column(name = "currency")
    private String currency = "INR";

    @Column(name = "language")
    private String language = "en";

    // Additional fields for CompanyProfileServiceImpl
    @Column(name = "company_url")
    private String companyUrl;

    @Column(name = "gstin_number")
    private String gstinNumber;

    @Column(name = "cin_number")
    private String cinNumber;

    @Column(name = "dl_no")
    private String dlNo;

    @Column(name = "registered_office")
    private String registeredOffice;

    @Column(name = "company_phone")
    private String companyPhone;

    @Column(name = "company_alternate_no")
    private String companyAlternateNo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    // Helper methods to match expected method names
    public String getCompanyLogoUrl() { return this.logoUrl; }
    public void setCompanyLogoUrl(String companyLogoUrl) { this.logoUrl = companyLogoUrl; }

    public String getAddress() { return this.addressLine1; }
    public void setAddress(String address) { this.addressLine1 = address; }

    public String getCompanyEmail() { return this.email; }
    public void setCompanyEmail(String companyEmail) { this.email = companyEmail; }

    public Boolean getActive() { return this.isActive; }
    public void setActive(Boolean active) { this.isActive = active; }
}