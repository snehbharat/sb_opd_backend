package com.sbpl.OPD.serviceImp;


import com.opencsv.exceptions.CsvValidationException;
import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.dto.customer.request.CustomerRegistrationDto;
import com.sbpl.OPD.dto.customer.request.CustomerUpdateDto;
import com.sbpl.OPD.dto.customer.request.PatientCsvImportRequestDto;
import com.sbpl.OPD.dto.customer.response.CustomerListView;
import com.sbpl.OPD.dto.customer.response.CustomerResponseDto;
import com.sbpl.OPD.dto.customer.response.CustomerWithPackageUsageDTO;
import com.sbpl.OPD.dto.customer.response.PatientCsvImportResponseDto;
import com.sbpl.OPD.dto.treatment.pkg.PackageUsageInfoDTO;
import com.sbpl.OPD.enums.CustomerSearchType;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Department;
import com.sbpl.OPD.model.PatientPackageUsage;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DepartmentRepository;
import com.sbpl.OPD.repository.PatientPackageUsageRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.CustomerService;
import com.sbpl.OPD.service.CustomerUhidService;
import com.sbpl.OPD.utils.CsvParserUtil;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.RbacUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Service implementation responsible for managing customers.
 * This includes:
 * - Creating customers
 * - Fetching customers
 * - Updating customer details
 * - Deleting customers
 * <p>
 * All validations and error handling are managed here.
 *
 * @author Rahul Kumar
 */
@Service
public class CustomerServiceImpl implements CustomerService {

    private static final Logger logger =
            LoggerFactory.getLogger(CustomerServiceImpl.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private CompanyProfileRepository companyRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private DepartmentRepository departmentRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private RbacUtil rbacUtil;

    @Autowired
    private CustomerUhidService customerUhidService;

    @Autowired
    private PatientPackageUsageRepository patientPackageUsageRepository;

    /**
     * Fetch all customers with pagination.
     */
    @Override
    public ResponseEntity<?> getAllCustomers(Integer pageNo, Integer pageSize, Long branchId) {

        logger.info("Fetching customers [pageNo={}, pageSize={}, branchId={}]", pageNo, pageSize, branchId);
        User currentUser = DbUtill.getCurrentUser();
        
        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithoutSort(pageNo, pageSize);
            Page<CustomerListView> page;
            
            if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                // Super Admin and Super Admin Manager can see all customers
                page = customerRepository.findAllProjectedByOrderByCreatedAtDesc(pageRequest);
                logger.info("SUPER_ADMIN accessing all customers system-wide");
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                // SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's customers");
                    }
                    page = customerRepository.findByBranchId(branchId, pageRequest);
                    logger.info("SAAS_ADMIN accessing customers for branch ID: {}", branchId);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    page = customerRepository.findByCompanyIdProjected(companyId, pageRequest);
                    logger.info("SAAS_ADMIN accessing customers for company ID: {}", companyId);
                }
            } else {
                // Other roles (BRANCH_MANAGER, DOCTOR, RECEPTIONIST, STAFF, BILLING_STAFF, etc.) see only customers from their assigned branch
                if (currentUser.getCompany() != null) {
                    page = customerRepository.findByCompanyIdProjected(currentUser.getCompany().getId(), pageRequest);
                    logger.info("{} accessing customers for branch ID: {}", currentUser.getRole(), currentUser.getBranch().getId());
                } else {
                    logger.warn("{} user has no branch assignment", currentUser.getRole());
                    page = Page.empty();
                }
            }

            String successMessage;
            if (rbacUtil.isSuperAdmin()) {
                successMessage = "All customers fetched successfully (system-wide access)";
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                if (branchId != null) {
                    successMessage = "Branch customers fetched successfully";
                } else {
                    successMessage = "Company customers fetched successfully";
                }
            } else {
                if (currentUser.getBranch() != null) {
                    successMessage = "Branch customers fetched successfully";
                } else {
                    successMessage = "No branch assignment found";
                }
            }

            return baseResponse.successResponse(
                    successMessage,
                    DbUtill.buildPaginatedResponse(page, page.getContent())
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching customers", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch customers at the moment"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllCustomersNew(Integer pageNo, Integer pageSize, Long branchId) {

        logger.info("Fetching customers [pageNo={}, pageSize={}, branchId={}]", pageNo, pageSize, branchId);
        User currentUser = DbUtill.getCurrentUser();
        
        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithoutSort(pageNo, pageSize);
            Page<CustomerListView> page;
            
            if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                // Super Admin and Super Admin Manager can see all customers
                page = customerRepository.findAllProjectedByOrderByCreatedAtDesc(pageRequest);
                logger.info("SUPER_ADMIN accessing all customers system-wide");
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                // SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's customers");
                    }
                    page = customerRepository.findByBranchId(branchId, pageRequest);
                    logger.info("SAAS_ADMIN accessing customers for branch ID: {}", branchId);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    page = customerRepository.findByCompanyIdProjected(companyId, pageRequest);
                    logger.info("SAAS_ADMIN accessing customers for company ID: {}", companyId);
                }
            } else {
                // Other roles (BRANCH_MANAGER, DOCTOR, RECEPTIONIST, STAFF, BILLING_STAFF, etc.) see only customers from their assigned branch
                if (currentUser.getCompany() != null) {
                    page = customerRepository.findByCompanyIdProjected(currentUser.getCompany().getId(), pageRequest);
                    logger.info("{} accessing customers for branch ID: {}", currentUser.getRole(), currentUser.getBranch().getId());
                } else {
                    logger.warn("{} user has no branch assignment", currentUser.getRole());
                    page = Page.empty();
                }
            }

            List<CustomerWithPackageUsageDTO> customersWithPackageUsage = page.getContent().stream()
                    .map(this::convertToCustomerWithPackageUsageDTO)
                    .collect(Collectors.toList());

            String successMessage;
            if (rbacUtil.isSuperAdmin()) {
                successMessage = "All customers fetched successfully (system-wide access)";
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                if (branchId != null) {
                    successMessage = "Branch customers fetched successfully";
                } else {
                    successMessage = "Company customers fetched successfully";
                }
            } else {
                if (currentUser.getBranch() != null) {
                    successMessage = "Branch customers fetched successfully";
                } else {
                    successMessage = "No branch assignment found";
                }
            }

            Page<CustomerWithPackageUsageDTO> resultPage = new PageImpl<>(customersWithPackageUsage, pageRequest, page.getTotalElements());

            return baseResponse.successResponse(
                    successMessage,
                    DbUtill.buildPaginatedResponse(resultPage, resultPage.getContent())
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching customers", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch customers at the moment"
            );
        }
    }

    private CustomerWithPackageUsageDTO convertToCustomerWithPackageUsageDTO(CustomerListView customerView) {
        CustomerWithPackageUsageDTO dto = new CustomerWithPackageUsageDTO();
        dto.setId(customerView.getId());
        dto.setPrefix(customerView.getPrefix());
        dto.setFirstName(customerView.getFirstName());
        dto.setLastName(customerView.getLastName());
        dto.setPhoneNumber(customerView.getPhoneNumber());
        dto.setEmail(customerView.getEmail());
        dto.setDateOfBirth(customerView.getDateOfBirth());
        dto.setAge(customerView.getAge());
        dto.setAddress(customerView.getAddress());
        dto.setGender(customerView.getGender());
        dto.setUhid(customerView.getUhid());
        dto.setCompanyId(customerView.getCompanyId());
        dto.setBranchId(customerView.getBranchId());
        dto.setDepartmentId(customerView.getDepartmentId());
        dto.setTotalPaidAmount(customerView.getTotalPaidAmount());
        dto.setTotalDueAmount(customerView.getTotalDueAmount());
        dto.setTotalBillAmount(customerView.getTotalBillAmount());

        List<PatientPackageUsage> activeUsages = patientPackageUsageRepository.findActiveUsagesByPatientId(customerView.getId());
        
        if (!activeUsages.isEmpty()) {
            PatientPackageUsage latestUsage = activeUsages.get(0);
            PackageUsageInfoDTO usageDTO = new PackageUsageInfoDTO();
            usageDTO.setPackageUsageId(latestUsage.getId());
            usageDTO.setTreatmentPackageId(latestUsage.getTreatmentPackage().getId());
            usageDTO.setPackageName(latestUsage.getTreatmentPackage().getName());
            usageDTO.setTreatmentName(latestUsage.getTreatment().getName());
            usageDTO.setTotalSessions(latestUsage.getTotalSessions());
            usageDTO.setSessionsUsed(latestUsage.getSessionsUsed());
            usageDTO.setSessionsRemaining(latestUsage.getSessionsRemaining());
            usageDTO.setCompleted(latestUsage.getCompleted());
            usageDTO.setPurchaseDate(latestUsage.getPurchaseDate());
            usageDTO.setFollowUpDate(latestUsage.getFollowUpDate());
            usageDTO.setFollowUp(latestUsage.getFollowUp());
            usageDTO.setLastSessionDate(latestUsage.getLastSessionDate());
            dto.setPackageUsageInfo(usageDTO);
        }

        return dto;
    }

    /**
     * Fetch customer by ID.
     */
    @Override
    public ResponseEntity<?> getCustomerById(Long id) {

        logger.info("Fetching customer by id={}", id);

        try {
            Customer customer = getCustomerOrThrow(id);
            CustomerResponseDto customerDto = convertToResponseDto(customer);
            return baseResponse.successResponse("Customer fetched successfully", customerDto);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching customer", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch customer"
            );
        }
    }

    /**
     * Fetch customer by phone number.
     */
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getCustomerByPhoneNumber(String phoneNumber) {

        logger.info("Fetching customer by phoneNumber={}", phoneNumber);

        try {
            Optional<CustomerListView> customer =
                    customerRepository.findProjectedByPhoneNumber(phoneNumber);

            if (customer.isEmpty()) {
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with phone number"
                );
            }

            return baseResponse.successResponse(
                    "Customer fetched successfully",
                    customer.get()
            );

        } catch (Exception e) {
            logger.error("Error while fetching customer by phone", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch customer"
            );
        }
    }

    /**
     * Fetch customer by email.
     */
    @Override
    public ResponseEntity<?> getCustomerByEmail(String email) {

        logger.info("Fetching customer by email={}", email);

        try {
            Optional<CustomerListView> customer =
                    customerRepository.findProjectedByEmail(email);

            if (customer.isEmpty()) {
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Customer not found with email"
                );
            }

            return baseResponse.successResponse(
                    "Customer fetched successfully",
                    customer.get()
            );

        } catch (Exception e) {
            logger.error("Error while fetching customer by email", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch customer"
            );
        }
    }

    /**
     * Create a new customer.
     */
    @Override
    public ResponseEntity<?> createCustomer(CustomerRegistrationDto dto) {

        logger.info("Creating customer [phone={}, email={}]",
                dto.getPhoneNumber(), dto.getEmail());

        try {

            CompanyProfile company = companyRepository.findById(dto.getCompanyId())
                    .orElse(null);
            if (company == null) {
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Company not found");
            }
            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElse(null);

            if (branch == null) {
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Branch not found");
            }


//            if (customerRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
//                return baseResponse.errorResponse(
//                        HttpStatus.CONFLICT,
//                        "Phone number already exists"
//                );
//            }

//            if (dto.getEmail() != null &&
//                    customerRepository.existsByEmail(dto.getEmail())) {
//                return baseResponse.errorResponse(
//                        HttpStatus.CONFLICT,
//                        "Email already exists"
//                );
//            }

            Customer customer = new Customer();
            customer.setPrefix(dto.getPrefix());
            customer.setFirstName(dto.getFirstName());
            customer.setLastName(dto.getLastName());
            customer.setPhoneNumber(dto.getPhoneNumber());
            customer.setEmail(dto.getEmail());
            customer.setAddress(dto.getAddress());
            customer.setGender(dto.getGender());
            customer.setBranch(branch);
            customer.setDateOfBirth(dto.getDateOfBirth());
            customer.setAge(dto.getAge()); // Age can be null now
            customer.setCompany(company);
            
            // Generate and set UHID
            String uhid = customerUhidService.generateUhid(dto.getCompanyId(), dto.getBranchId());
            customer.setUhid(uhid);
            
            // Set department if provided
            if (dto.getDepartmentId() != null) {
                Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));
                customer.setDepartment(department);
            }

            Customer savedCustomer = customerRepository.save(customer);
            CustomerResponseDto responseDto = convertToResponseDto(savedCustomer);

            return baseResponse.successResponse(
                    "Customer created successfully"
            );

        } catch (DataIntegrityViolationException e) {
            logger.warn("Customer creation failed due to data integrity violation", e);
            String errorMessage = parseDatabaseConstraintError(e);
            return baseResponse.errorResponse(HttpStatus.CONFLICT, errorMessage);

        } catch (Exception e) {
            logger.error("Error while creating customer", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create customer"
            );
        }
    }

    /**
     * Update customer details.
     */
    @Override
    public ResponseEntity<?> updateCustomer(Long id, CustomerUpdateDto dto) {

        logger.info("Updating customer id={}", id);

        try {

            Customer dbCustomer = getCustomerOrThrow(id);

            if (dto.getPrefix() != null) {
                dbCustomer.setPrefix(dto.getPrefix());
            }

            if (dto.getFirstName() != null) {
                dbCustomer.setFirstName(dto.getFirstName());
            }

            if (dto.getLastName() != null) {
                dbCustomer.setLastName(dto.getLastName());
            }
            if (dto.getAge() != null) {
                dbCustomer.setAge(dto.getAge()); // Age can be null now
            }
            if (dto.getDateOfBirth() != null) {
                dbCustomer.setDateOfBirth(dto.getDateOfBirth());
            }


            if (dto.getCompanyId() != null) {
                CompanyProfile company = companyRepository.findById(dto.getCompanyId())
                        .orElseThrow(() -> new IllegalArgumentException("Company not found"));
                dbCustomer.setCompany(company);
            }

            if (dto.getBranchId() != null) {
                Branch branch = branchRepository.findById(dto.getBranchId())
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
                dbCustomer.setBranch(branch);
            }

            if (dto.getPhoneNumber() != null &&
                    !dto.getPhoneNumber().equals(dbCustomer.getPhoneNumber())) {

                if (customerRepository.existsByPhoneNumber(dto.getPhoneNumber())) {
                    return baseResponse.errorResponse(
                            HttpStatus.CONFLICT,
                            "Phone number already exists"
                    );
                }
                dbCustomer.setPhoneNumber(dto.getPhoneNumber());
            }

            if (dto.getEmail() != null &&
                    !dto.getEmail().equals(dbCustomer.getEmail())) {

                if (customerRepository.existsByEmail(dto.getEmail())) {
                    return baseResponse.errorResponse(
                            HttpStatus.CONFLICT,
                            "Email already exists"
                    );
                }
                dbCustomer.setEmail(dto.getEmail());
            }

            if (dto.getAddress() != null) {
                dbCustomer.setAddress(dto.getAddress());
            }
            
            if (dto.getGender() != null) {
                dbCustomer.setGender(dto.getGender());
            }
            
            // Update department if provided
            if (dto.getDepartmentId() != null) {
                Department department = departmentRepository.findById(dto.getDepartmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Department not found with ID: " + dto.getDepartmentId()));
                
                // Ensure the department belongs to the same company as the customer's branch
                User currentUser = DbUtill.getCurrentUser();
                if (!(currentUser.getRole() == UserRole.SUPER_ADMIN || 
                      currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER)) {
                    // For non-super admins, validate that the department belongs to the same company
                    Long userCompanyId = DbUtill.getLoggedInCompanyId();
                    if (!department.getBranch().getClinic().getId().equals(userCompanyId)) {
                        return baseResponse.errorResponse(
                                HttpStatus.FORBIDDEN,
                                "Cannot assign department that does not belong to your company"
                        );
                    }
                }
                
                dbCustomer.setDepartment(department);
            }

            Customer updatedCustomer = customerRepository.save(dbCustomer);
            CustomerResponseDto responseDto = convertToResponseDto(updatedCustomer);

            return baseResponse.successResponse(
                    "Customer updated successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (DataIntegrityViolationException e) {
            logger.warn("Customer update failed due to data integrity violation [customerId={}]", id, e);
            String errorMessage = parseDatabaseConstraintError(e);
            return baseResponse.errorResponse(HttpStatus.CONFLICT, errorMessage);

        } catch (Exception e) {
            logger.error("Error while updating customer", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update customer"
            );
        }
    }

    /**
     * Delete customer.
     */
    @Override
    public ResponseEntity<?> deleteCustomer(Long id) {

        logger.info("Deleting customer id={}", id);

        try {
            Customer customer = getCustomerOrThrow(id);
            customerRepository.delete(customer);

            return baseResponse.successResponse(
                    "Customer deleted successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while deleting customer", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete customer"
            );
        }
    }

    @Override
    public ResponseEntity<?> importPatientsFromCsv(PatientCsvImportRequestDto requestDto) {
        logger.info("Importing patients from CSV [companyId={}, branchId={}]",
                requestDto.getCompanyId(), requestDto.getBranchId());

        try {
            // Validate inputs
            if (requestDto.getCsvFile() == null || requestDto.getCsvFile().isEmpty()) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "CSV file is required"
                );
            }

            // Validate company and branch exist
            var company = companyRepository.findById(requestDto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Company not found"));
            var branch = branchRepository.findById(requestDto.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found"));

            // Check file extension
            String fileName = requestDto.getCsvFile().getOriginalFilename();
            if (fileName == null || !fileName.toLowerCase().endsWith(".csv")) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Only CSV files are allowed"
                );
            }

            // Parse CSV file
            List<String[]> csvRecords;
            try {
                csvRecords = CsvParserUtil.parseCsvFile(requestDto.getCsvFile());
            } catch (CsvValidationException e) {
                logger.error("Error parsing CSV file", e);
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Invalid CSV format: " + e.getMessage()
                );
            }

            // Process each record
            int totalProcessed = 0;
            int successfulImports = 0;
            int failedImports = 0;
            List<String> errors = new ArrayList<>();
            List<Long> importedPatientIds = new ArrayList<>();

            for (int i = 0; i < csvRecords.size(); i++) {
                String[] record = csvRecords.get(i);
                totalProcessed++;

                try {
                    // Validate record fields - expect 10 fields based on your CSV structure
                    if (record.length < 6) { // Minimum required fields (id, patient_name, patient_phone, patient_gender, patient_age, department_name)
                        errors.add("Row " + (i + 2) + ": Insufficient data columns. Expected at least 6 fields (id, patient_name, patient_phone, patient_gender, patient_age, department_name)");
                        failedImports++;
                        continue;
                    }
                    String patientFullName = record[1].trim();
                    String patientPhone = record[2].trim();
                    String patientGender = record[4].trim();
                    String patientAgeStr = record[5].trim();
                    String departmentIdStr = record[6].trim();
                    String departmentName = record[7].trim();
                    String patientPincode = record[8].trim();
                    String patientAddress = record[9].trim();

                    // Combine pincode with address
                    String fullAddress = patientAddress;
                    if (!patientPincode.isEmpty()) {
                        fullAddress = patientAddress + " - Pincode: " + patientPincode;
                    }

                    // Split patient name into first and last name
                    String firstName = "";
                    String lastName = "";
                    if (patientFullName != null && !patientFullName.trim().isEmpty()) {
                        String[] nameParts = patientFullName.trim().split("\\s+", 2);
                        firstName = nameParts[0];
                        lastName = nameParts.length > 1 ? nameParts[1] : "";
                    }

                    // Convert age to Long - extract numeric value from text like "26 years 00 months 00 days"
                    Long patientAge = null;
                    if (!patientAgeStr.trim().isEmpty()) {
                        // Extract the first numeric value from the age string (typically the years part)
                        String ageNumeric = patientAgeStr.trim().replaceAll("[^0-9\\s]+", " ").trim();
                        if (!ageNumeric.isEmpty()) {
                            // Get the first number from the cleaned string (usually the years)
                            String[] numbers = ageNumeric.split("\\s+");
                            if (numbers.length > 0) {
                                try {
                                    patientAge = Long.parseLong(numbers[0]);
                                } catch (NumberFormatException e) {
                                    logger.warn("Invalid age format for patient {}: {}", patientFullName, patientAgeStr);
                                }
                            }
                        }
                    }

                    // Check for duplicate phone numbers
                    if (customerRepository.existsByPhoneNumber(patientPhone)) {
                        errors.add("Row " + (i + 2) + ": Phone number already exists - " + patientPhone);
                        failedImports++;
                        continue;
                    }

                    // Create customer object
                    Customer customer = new Customer();
                    customer.setFirstName(firstName);
                    customer.setLastName(lastName);
                    customer.setPhoneNumber(patientPhone);
                    customer.setGender(patientGender); // Set gender from CSV
                    customer.setAddress(fullAddress); // Use combined address with pincode
                    customer.setBranch(branch);
                    customer.setAge(patientAge);
                    customer.setCompany(company);

                    // Generate and set UHID
                    String uhid = customerUhidService.generateUhid(requestDto.getCompanyId(), requestDto.getBranchId());
                    customer.setUhid(uhid);

                    // Link to department if department exists - lookup by name only
                    Department department = null;

                    // Only try to find department by name (as per requirement)
                    if (departmentName != null && !departmentName.trim().isEmpty()) {
                        department = departmentRepository.findByDepartmentNameAndBranch_Id(departmentName, branch.getId()).orElse(null);

                        if (department != null) {
                            logger.debug("Found department by name '{}' for patient {}", departmentName, patientFullName);
                        } else {
                            logger.warn("Department not found by name '{}' for patient {}. Patient will be created without department association.", departmentName, patientFullName);
                        }
                    } else {
                        logger.warn("Department name is empty for patient {}. Patient will be created without department association.", patientFullName);
                    }

                    // Set the department if found
                    if (department != null) {
                        customer.setDepartment(department);
                    }

                    // Save the customer
                    Customer savedCustomer = customerRepository.save(customer);
                    importedPatientIds.add(savedCustomer.getId());
                    successfulImports++;

                } catch (NumberFormatException e) {
                    errors.add("Row " + (i + 2) + ": Invalid number format - " + e.getMessage());
                    failedImports++;
                } catch (Exception e) {
                    errors.add("Row " + (i + 2) + ": Error processing record - " + e.getMessage());
                    failedImports++;
                }
            }

            // Prepare response
            PatientCsvImportResponseDto responseDto = new PatientCsvImportResponseDto();
            responseDto.setTotalProcessed(totalProcessed);
            responseDto.setSuccessfulImports(successfulImports);
            responseDto.setFailedImports(failedImports);
            responseDto.setErrors(errors);
            responseDto.setImportedPatientIds(importedPatientIds);

            String message = String.format(
                    "CSV import completed. Total: %d, Successful: %d, Failed: %d",
                    totalProcessed, successfulImports, failedImports
            );

            return baseResponse.successResponse(message, responseDto);

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );
        } catch (Exception e) {
            logger.error("Error importing patients from CSV", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to import patients from CSV"
            );
        }
    }


    @Override
    public ResponseEntity<?> search(
        CustomerSearchType type,
        String value) {
        try {
            Pageable pageable = DbUtill.buildPageRequest(0, 100);

            Long branchId = null;
            Long companyId = null;

            UserRole role = DbUtill.getLoggedInUserOriginalRole();
            boolean isAdmin = true;

            if (isAdmin) {
                companyId = DbUtill.getLoggedInCompanyId();
            } else {
                branchId = DbUtill.getLoggedInBranchId();
            }

            List<CustomerListView> result = switch (type) {

                case UHID -> isAdmin
                    ? customerRepository.findByUhidAndCompanyId(value, companyId, pageable)
                    : customerRepository.findByUhidAndBranchId(value, branchId, pageable);

                case PHONE -> isAdmin
                    ? customerRepository.findByPhoneNumberAndCompanyId(value, companyId, pageable)
                    : customerRepository.findByPhoneNumberAndBranchId(value, branchId, pageable);

                case EMAIL -> isAdmin
                    ? customerRepository.findByEmailAndCompanyId(value, companyId, pageable)
                    : customerRepository.findByEmailAndBranchId(value, branchId, pageable);

                case NAME -> isAdmin
                    ? customerRepository.findByNameAndCompanyId(value, companyId, pageable)
                    : customerRepository.findByNameAndBranchId(value, branchId, pageable);

                case GLOBAL -> isAdmin
                    ? customerRepository.searchGloballyByCompany(value, companyId, pageable)
                    : customerRepository.searchGloballyByBranch(value, branchId, pageable);

                default -> Collections.emptyList();
            };

            assert result != null;
            String message = result.isEmpty()
                ? "No customers found for given search criteria"
                : "Customer search completed successfully";

            return baseResponse.successResponse(message, result);
        } catch (Exception e) {
            logger.error("Error searching for customers from CSV", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "something went wrong");
        }
    }


    @Override
    public ResponseEntity<?> searchNew(
            CustomerSearchType type,
            String value) {
        try {
            Pageable pageable = DbUtill.buildPageRequest(0, 100);

            Long branchId = null;
            Long companyId = null;

            UserRole role = DbUtill.getLoggedInUserOriginalRole();
            boolean isAdmin = true;

            if (isAdmin) {
                companyId = DbUtill.getLoggedInCompanyId();
            } else {
                branchId = DbUtill.getLoggedInBranchId();
            }

            List<CustomerListView> result = switch (type) {

                case UHID -> isAdmin
                        ? customerRepository.findByUhidAndCompanyId(value, companyId, pageable)
                        : customerRepository.findByUhidAndBranchId(value, branchId, pageable);

                case PHONE -> isAdmin
                        ? customerRepository.findByPhoneNumberAndCompanyId(value, companyId, pageable)
                        : customerRepository.findByPhoneNumberAndBranchId(value, branchId, pageable);

                case EMAIL -> isAdmin
                        ? customerRepository.findByEmailAndCompanyId(value, companyId, pageable)
                        : customerRepository.findByEmailAndBranchId(value, branchId, pageable);

                case NAME -> isAdmin
                        ? customerRepository.findByNameAndCompanyId(value, companyId, pageable)
                        : customerRepository.findByNameAndBranchId(value, branchId, pageable);

                case GLOBAL -> isAdmin
                        ? customerRepository.searchGloballyByCompany(value, companyId, pageable)
                        : customerRepository.searchGloballyByBranch(value, branchId, pageable);

                default -> Collections.emptyList();
            };

            List<CustomerWithPackageUsageDTO> customersWithPackageUsage = result.stream()
                    .map(this::convertToCustomerWithPackageUsageDTO)
                    .toList();
            String message = result.isEmpty()
                    ? "No customers found for given search criteria"
                    : "Customer search completed successfully";


            return baseResponse.successResponse(message, customersWithPackageUsage);
        } catch (Exception e) {
            logger.error("Error searching for customers from CSV", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "something went wrong");
        }
    }

    /**
     * Utility method to fetch customer or throw exception.
     */
    private Customer getCustomerOrThrow(Long customerId) {

        if (customerId == null) {
            throw new IllegalArgumentException("customerId cannot be null");
        }

        User currentUser = DbUtill.getCurrentUser();
        if (currentUser.getRole() == UserRole.SUPER_ADMIN ||
            currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
            return customerRepository.findByIdWithAssociations(customerId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found for id: " + customerId
                        )
                );
        }
        Long companyId = DbUtill.getLoggedInCompanyId();
        return customerRepository.findByIdAndCompanyIdWithAssociations(customerId, companyId)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "Customer not found for id: " + customerId + " in your company"
                        )
                );
    }

    private CustomerResponseDto convertToResponseDto(Customer customer) {
        CustomerResponseDto dto = new CustomerResponseDto();

        dto.setId(customer.getId());
        dto.setPrefix(customer.getPrefix());
        dto.setFirstName(customer.getFirstName());
        dto.setLastName(customer.getLastName());
        dto.setPhoneNumber(customer.getPhoneNumber());
        dto.setEmail(customer.getEmail());
        dto.setAge(customer.getAge());
        dto.setDateOfBirth(customer.getDateOfBirth());
        dto.setAddress(customer.getAddress());
        dto.setGender(customer.getGender());
        dto.setUhid(customer.getUhid());

        // Set company details if available
        if (customer.getCompany() != null) {
            dto.setCompanyId(customer.getCompany().getId());
            dto.setCompanyName(customer.getCompany().getCompanyName());
        }

        // Set branch details if available
        if (customer.getBranch() != null) {
            dto.setBranchId(customer.getBranch().getId());
            dto.setBranchName(customer.getBranch().getBranchName());
        }

        // Set department details if available
        if (customer.getDepartment() != null) {
            dto.setDepartmentId(customer.getDepartment().getId());
            dto.setDepartmentName(customer.getDepartment().getDepartmentName());
        }

        // Set timestamps
        if (customer.getCreatedAt() != null) {
            dto.setCreatedAt(customer.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (customer.getUpdatedAt() != null) {
            dto.setUpdatedAt(customer.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }

        return dto;
    }

    /**
     * Parse database constraint violation errors to provide user-friendly messages
     *
     * @param e DataIntegrityViolationException
     * @return User-friendly error message
     */
    private String parseDatabaseConstraintError(DataIntegrityViolationException e) {
        // Get the root cause message
        String message = getRootCauseMessage(e);

        // Handle unique constraint violations based on exception message
        if (message != null) {

            // Handle unique constraint violations
            if (message.toLowerCase().contains("duplicate") && message.toLowerCase().contains("constraint")) {
                if (message.contains("customer_phone_number")) {
                    return "Phone number already exists. Please use a different phone number.";
                } else if (message.contains("customer_email")) {
                    return "Email already exists. Please use a different email address.";
                } else {
                    return "Duplicate entry detected. Please check your input values.";
                }
            }
            // Handle other constraint violations
            else if (message.toLowerCase().contains("foreign key")) {
                return "Referenced record does not exist. Please check your input data.";
            } else if (message.toLowerCase().contains("not-null") || message.toLowerCase().contains("null value")) {
                return "Required field is missing. Please provide all required information.";
            }
        }

        // Default message if we can't parse the specific constraint
        return "Data validation failed. Please check your input and try again.";
    }

    /**
     * Get the root cause message from an exception
     *
     * @param e Exception to analyze
     * @return Root cause message or null if not found
     */
    private String getRootCauseMessage(Exception e) {
        Throwable cause = e;
        while (cause.getCause() != null) {
            cause = cause.getCause();
        }
        return cause.getMessage();
    }

    /**
     * Check if a branch is accessible to the current user based on their role
     * @param branch The branch to check
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
}