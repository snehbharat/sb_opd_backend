package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.company.request.CompanyProfileRequestDto;
import com.sbpl.OPD.dto.company.request.CompanyProfileUpdateDto;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.CompanyProfileService;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for Company Profile management.
 *
 * Provides APIs for creating, updating,
 * retrieving, and deleting healthcare company profiles.
 *
 * One company (Admin) can manage multiple clinics,
 * branches, departments, and doctors.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/company-profiles")
public class CompanyProfileController {

    @Autowired
    private CompanyProfileService companyProfileService;

    @Autowired
    private RbacUtil rbacUtil;

    @GetMapping
    public ResponseEntity<?> getAllCompanies(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to view companies");
        }
        return companyProfileService.getAllCompanies(pageNo, pageSize);
    }

    @PostMapping(value = "/create", consumes = "multipart/form-data")
    public ResponseEntity<?> createCompany(
            @Valid @ModelAttribute CompanyProfileRequestDto requestDto) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to create company");
        }
        return companyProfileService.createCompany(requestDto);
    }

    @GetMapping("/me")
    public ResponseEntity<?> getClinicDetails() {
        Long clinicId = DbUtill.getLoggedInCompanyId();
        return companyProfileService.getCompanyById(clinicId);
    }


    @GetMapping("/{id}")
    public ResponseEntity<?> getCompanyById(
            @PathVariable @NotNull Long id) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to view company");
        }
        return companyProfileService.getCompanyById(id);
    }

    @PutMapping(value = "/update/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> updateCompany(
            @PathVariable @NotNull Long id,
            @Valid @ModelAttribute CompanyProfileUpdateDto requestDto) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN, UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN_MANAGER,UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to update company");
        }
        return companyProfileService.updateCompany(id, requestDto);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteCompany(
            @PathVariable @NotNull Long id) {
        if (!rbacUtil.hasRole(UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient permissions to delete company");
        }
        return companyProfileService.deleteCompany(id);
    }
}