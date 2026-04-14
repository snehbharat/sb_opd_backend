package com.sbpl.OPD.repository;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.model.CompanyProfile;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for CompanyProfile entity operations.
 * Provides CRUD operations for company profile management.
 *
 * @author HMS Team
 */
@Repository
public interface CompanyProfileRepository extends JpaRepository<CompanyProfile, Long> {

    Page<CompanyProfile> findAll(Pageable pageable);

    /**
     * Find active company profile
     */
    Optional<CompanyProfile> findByIsActiveTrue();

    /**
     * Find company profile by company code
     */
    Optional<CompanyProfile> findByCompanyCode(String companyCode);

    /**
     * Find company profile by email
     */
    Optional<CompanyProfile> findByEmail(String email);

    /**
     * Check if company code already exists
     */
    boolean existsByCompanyCode(String companyCode);


    /**
     * Find company profile by registration number
     */
    Optional<CompanyProfile> findByRegistrationNumber(String registrationNumber);


    /**
     * Check if GSTIN number already exists
     */
    boolean existsByGstinNumber(String gstinNumber);

    /**
     * Find company profile by GSTIN number
     */
    Optional<CompanyProfile> findByGstinNumber(String gstinNumber);

    /**
     * Find company profile by company email
     */
//    Optional<CompanyProfile> findByCompanyEmail(String companyEmail);

    boolean existsByEmail(@Email @NotBlank String companyEmail);

    /**
     * Find company profile by user
     */
    Optional<CompanyProfile> findByUser(User user);
}