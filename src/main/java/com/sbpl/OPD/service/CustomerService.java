package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.customer.request.CustomerRegistrationDto;
import com.sbpl.OPD.dto.customer.request.CustomerUpdateDto;
import com.sbpl.OPD.dto.customer.request.PatientCsvImportRequestDto;
import com.sbpl.OPD.enums.CustomerSearchType;
import org.springframework.http.ResponseEntity;

/**
 * This is a customer management service interface.
 * Handles customer creation, retrieval, update, and deletion
 * for the Pharmacy Management System.
 *
 * @author rahul kumar
 */
public interface CustomerService {

    ResponseEntity<?> getAllCustomers(Integer pageNo, Integer pageSize, Long branchId);

    ResponseEntity<?> getAllCustomersNew(Integer pageNo, Integer pageSize, Long branchId);

    ResponseEntity<?> getCustomerById(Long id);

    ResponseEntity<?> getCustomerByPhoneNumber(String phoneNumber);

    ResponseEntity<?> getCustomerByEmail(String email);

    ResponseEntity<?> createCustomer(CustomerRegistrationDto dto);

    ResponseEntity<?> updateCustomer(Long id, CustomerUpdateDto dto);

    ResponseEntity<?> deleteCustomer(Long id);

    ResponseEntity<?> importPatientsFromCsv(PatientCsvImportRequestDto requestDto);

    ResponseEntity<?> search(
            CustomerSearchType type,
            String value);

    ResponseEntity<?> searchNew(
            CustomerSearchType type,
            String value);
}
