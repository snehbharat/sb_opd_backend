package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.customer.request.CustomerRegistrationDto;
import com.sbpl.OPD.dto.customer.request.CustomerUpdateDto;
import com.sbpl.OPD.dto.customer.request.PatientCsvImportRequestDto;
import com.sbpl.OPD.enums.CustomerSearchType;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.CustomerService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer management operations with data isolation.
 * <p>
 * Implements hierarchical data access control based on user roles:
 * - SUPER_ADMIN: Access to all data
 * - ADMIN: Access to company-level data
 * - SAAS_ADMIN_MANAGER: Access to company-level data
 * - BRANCH_MANAGER: Access to their assigned branch data
 * - DOCTOR/STAFF: Access to their department data
 * - PATIENT: Access only to their own data
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/customers")
public class CustomerController {

    @Autowired
    private CustomerService customerService;

    @Autowired
    private RbacUtil rbacUtil;

//    @Autowired
//    private SecurityContextUtils securityContextUtils;
//
//    @Autowired
//    private DataIsolationService dataIsolationService;

    /**
     * Create a new customer.
     * Access: ADMIN, SUPER_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER, DOCTOR, STAFF
     */
    @PostMapping("/create")
    public ResponseEntity<?> createCustomer(@Valid @RequestBody CustomerRegistrationDto customerDTO) {

        return customerService.createCustomer(customerDTO);
    }

    /**
     * Update existing customer.
     * Access: ADMIN, SUPER_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER, DOCTOR, STAFF, or owner
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCustomer(
            @PathVariable @NotNull Long id,
            @Valid @RequestBody CustomerUpdateDto customerDTO) {

        return customerService.updateCustomer(id, customerDTO);
    }

    /**
     * Get customer by ID.
     * Access: All authenticated users with appropriate data access
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCustomerById(@PathVariable @NotNull Long id) {
        return customerService.getCustomerById(id);
    }

    /**
     * Get all customers with pagination.
     * Access: All authenticated users with appropriate data access
     * For SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
     */
    @GetMapping
    public ResponseEntity<?> getAllCustomers(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long branchId) {
        return customerService.getAllCustomersNew(pageNo, pageSize, branchId);
    }

    @GetMapping("/new")
    public ResponseEntity<?> getAllCustomersNew(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long branchId) {
        return customerService.getAllCustomersNew(pageNo, pageSize, branchId);
    }

    /**
     * Get customers by phone number.
     * Access: ADMIN, SUPER_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER, DOCTOR, STAFF
     */
    @GetMapping("/phone")
    public ResponseEntity<?> getCustomerByPhoneNumber(@RequestParam String phoneNumber) {

        return customerService.getCustomerByPhoneNumber(phoneNumber);
    }

    /**
     * Get customers by email.
     * Access: ADMIN, SUPER_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER, DOCTOR, STAFF
     */
    @GetMapping("/by-email")
    public ResponseEntity<?> getCustomerByEmail(@RequestParam String email) {
        return customerService.getCustomerByEmail(email);
    }

    /**
     * Delete a customer (soft delete).
     * Access: ADMIN, SUPER_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCustomer(@PathVariable @NotNull Long id) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN,
                UserRole.SUPER_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.SUPER_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Insufficient permissions to delete customer");
        }
        return customerService.deleteCustomer(id);
    }

    /**
     * Import patients from CSV file with department information.
     * Access: ADMIN, SUPER_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER, DOCTOR
     */
    @PostMapping(value = "/import-patients-csv", consumes = "multipart/form-data")
    public ResponseEntity<?> importPatientsFromCsv(@ModelAttribute PatientCsvImportRequestDto requestDto) {

        return customerService.importPatientsFromCsv(requestDto);
    }


    @GetMapping("/search")
    public ResponseEntity<?> searchCustomers(
            @RequestParam @Valid CustomerSearchType type,
            @RequestParam String keyword) {

        return customerService.search(type, keyword);
    }

    @GetMapping("/new/search")
    public ResponseEntity<?> searchCustomersNew(
            @RequestParam @Valid CustomerSearchType type,
            @RequestParam String keyword) {

        return customerService.searchNew(type, keyword);
    }

}