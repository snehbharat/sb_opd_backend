package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.company.request.CompanyProfileRequestDto;
import com.sbpl.OPD.dto.company.request.CompanyProfileUpdateDto;
import org.springframework.http.ResponseEntity;

/**
 * Company Profile management service.
 *
 * Handles creation, retrieval, update, and deletion of
 * healthcare company profiles (Tenant / Organization).
 *
 * One company can have multiple clinics, branches,
 * departments, and doctors under it.
 *
 * @author rahul kumar
 */
public interface CompanyProfileService {

    /**
     * Get all company profiles with pagination.
     *
     * @param pageNo   page number (0-based)
     * @param pageSize number of records per page
     */
    ResponseEntity<?> getAllCompanies(Integer pageNo, Integer pageSize);

    /**
     * Get company profile by company ID.
     *
     * @param companyId company primary key
     */
    ResponseEntity<?> getCompanyById(Long companyId);

    /**
     * Get company profile by registered email.
     *
     * @param email company email
     */
    ResponseEntity<?> getCompanyByEmail(String email);

    /**
     * Get company profile by GSTIN number.
     *
     * @param gstin GSTIN (India)
     */
    ResponseEntity<?> getCompanyByGstin(String gstin);

    /**
     * Create a new company profile.
     *
     * Only ADMIN / SUPER_ADMIN users are allowed.
     *
     * @param dto company profile request DTO
     */
    ResponseEntity<?> createCompany(CompanyProfileRequestDto dto);

    /**
     * Update company profile details.
     *
     * @param companyId company primary key
     * @param dto       company profile update DTO
     */
    ResponseEntity<?> updateCompany(Long companyId, CompanyProfileUpdateDto dto);

    /**
     * Soft delete / deactivate company profile.
     *
     * @param companyId company primary key
     */
    ResponseEntity<?> deleteCompany(Long companyId);
}
