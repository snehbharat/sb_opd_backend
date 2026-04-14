package com.sbpl.OPD.serviceImp;


import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.Auth.serviceImpl.UserServiceImpl;
import com.sbpl.OPD.dto.Doctor.request.DoctorDTO;
import com.sbpl.OPD.dto.Doctor.response.DoctorMinimalDTO;
import com.sbpl.OPD.dto.Doctor.response.DoctorResponseDTO;
import com.sbpl.OPD.dto.UserDTO;
import com.sbpl.OPD.enums.DoctorSearchType;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.model.DoctorCoreExpertise;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.repository.DepartmentRepository;
import com.sbpl.OPD.repository.DoctorCoreExpertiseRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.DoctorService;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.RbacUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.interceptor.TransactionAspectSupport;

import java.time.LocalDate;
import java.util.List;

/**
 * Implementation of doctor service logic.
 * <p>
 * Handles doctor profile management, activation,
 * and operational configurations.
 *
 * @author Rahul Kumar
 */


@Service
public class DoctorServiceImpl implements DoctorService {

    private static final Logger logger =
            LoggerFactory.getLogger(DoctorServiceImpl.class);

    private final DoctorRepository doctorRepository;
    private final UserRepository userRepository;
    private final BaseResponse baseResponse;
    private final UserServiceImpl userService;
    private final CompanyProfileRepository companyRepository;
    private final BranchRepository branchRepository;
    private final RbacUtil rbacUtil;
    private final DoctorCoreExpertiseRepository doctorCoreExpertiseRepository;
    private final DepartmentRepository departmentRepository;

    public DoctorServiceImpl(
            DoctorRepository doctorRepository,
            UserRepository userRepository,
            BaseResponse baseResponse, UserServiceImpl userService, CompanyProfileRepository companyRepository, BranchRepository branchRepository, RbacUtil rbacUtil, DoctorCoreExpertiseRepository doctorCoreExpertiseRepository,
            DepartmentRepository departmentRepository
    ) {
        this.doctorRepository = doctorRepository;
        this.userRepository = userRepository;
        this.baseResponse = baseResponse;
        this.userService = userService;
        this.companyRepository = companyRepository;
        this.branchRepository = branchRepository;
        this.rbacUtil = rbacUtil;
        this.doctorCoreExpertiseRepository = doctorCoreExpertiseRepository;
        this.departmentRepository = departmentRepository;
    }

    @Transactional
    @Override
    public ResponseEntity<?> createDoctor(DoctorDTO dto) {

        logger.info("Doctor creation request received [email={}]", dto.getDoctorEmail());

        try {

            CompanyProfile company = companyRepository.findById(dto.getCompanyId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid company ID"));

            Branch branch = branchRepository.findById(dto.getBranchId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid branch ID"));

            // Validate department if provided
            if (dto.getDepartment() != null && !dto.getDepartment().trim().isEmpty()) {
                validateDepartmentForBranch(dto.getDepartment(), dto.getBranchId());
            }

            UserDTO userDTO = new UserDTO();
//            userDTO.setUsername(dto.getDoctorEmail());
            String username = dto.getUsername();
            if (username == null || username.trim().isEmpty()) {
                username = dto.getPhoneNumber();
            }
            userDTO.setUsername(username);

            userDTO.setEmail(dto.getDoctorEmail());
            String password = dto.getPassword();
            if (password == null || password.trim().isEmpty()) {
                password = dto.getDoctorEmail();
            }
            userDTO.setPassword(password);
            userDTO.setFirstName(dto.getDoctorName());
            userDTO.setBranchId(dto.getBranchId());
            userDTO.setCompanyId(dto.getCompanyId());
            userDTO.setLastName("");
            userDTO.setPhoneNumber(dto.getPhoneNumber());
            userDTO.setRole(UserRole.DOCTOR);
            userDTO.setIsActive(dto.getIsActive());

            User savedUser = userService.createUserInternal(userDTO);

            Long currentUserId = DbUtill.getLoggedInUserId();
            mapToDoctorEntity(
                    dto,
                    savedUser,
                    currentUserId,
                    company,
                    branch
            );

            logger.info("Doctor created successfully [clinicId={},branchId = {}]",dto.getCompanyId(),dto.getBranchId());

            return baseResponse.successResponse(
                    "Doctor created successfully"
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Doctor creation failed | {}", e.getMessage());
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (DataIntegrityViolationException e) {
            logger.warn("Doctor creation failed due to data integrity violation", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            String errorMessage = parseDatabaseConstraintError(e);
            return baseResponse.errorResponse(HttpStatus.CONFLICT, errorMessage);

        } catch (Exception e) {
            logger.error("Error creating doctor", e);
            TransactionAspectSupport.currentTransactionStatus().setRollbackOnly();
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to create doctor"
            );
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> updateDoctor(Long doctorId, DoctorDTO dto) {

        logger.info("Doctor update request received [doctorId={}]", doctorId);

        try {

            Doctor doctor = getDoctorOrThrow(doctorId);

            boolean isAdmin = isAdmin();
            boolean isSelf = isSelfDoctor(doctor);

            validateAccess(isAdmin, isSelf);

            User user = doctor.getUser();

            if (dto.getRegistrationNumber() != null &&
                    !dto.getRegistrationNumber().equals(doctor.getRegistrationNumber())) {

                if (doctorRepository.existsByRegistrationNumber(dto.getRegistrationNumber())) {
                    throw new IllegalArgumentException("Registration number already exists");
                }

                doctor.setRegistrationNumber(dto.getRegistrationNumber());
            }

            if (dto.getPrefix() != null)
                doctor.setPrefix(dto.getPrefix());

            if (dto.getDoctorName() != null)
                doctor.setDoctorName(dto.getDoctorName());

            if (dto.getDoctorEmail() != null)
                doctor.setDoctorEmail(dto.getDoctorEmail());

            if (dto.getPhoneNumber() != null)
                doctor.setPhoneNumber(dto.getPhoneNumber());

            if (dto.getGender() != null)
                doctor.setGender(dto.getGender());

            if (dto.getDateOfBirth() != null)
                doctor.setDateOfBirth(dto.getDateOfBirth());

            if (dto.getSpecialization() != null)
                doctor.setSpecialization(dto.getSpecialization());

            if (dto.getSubSpecialization() != null)
                doctor.setSubSpecialization(dto.getSubSpecialization());

            if (dto.getExperienceYears() != null)
                doctor.setExperienceYears(dto.getExperienceYears());

            if (dto.getQualification() != null)
                doctor.setQualification(dto.getQualification());

            if (dto.getUniversity() != null)
                doctor.setUniversity(dto.getUniversity());

            if (dto.getDepartment() != null && !dto.getDepartment().trim().isEmpty()) {
                validateDepartmentForBranch(dto.getDepartment(), doctor.getBranch().getId());
                doctor.setDepartment(dto.getDepartment());
            }

            if (dto.getConsultationRoom() != null)
                doctor.setConsultationRoom(dto.getConsultationRoom());

            if (dto.getShiftTiming() != null)
                doctor.setShiftTiming(dto.getShiftTiming());

            if (dto.getJoiningDate() != null)
                doctor.setJoiningDate(dto.getJoiningDate());

            if (dto.getOnlineConsultationAvailable() != null)
                doctor.setOnlineConsultationAvailable(dto.getOnlineConsultationAvailable());

            if (isAdmin && dto.getConsultationFee() != null)
                doctor.setConsultationFee(dto.getConsultationFee());

            if (isAdmin && dto.getIsActive() != null)
                doctor.setIsActive(dto.getIsActive());

            if (dto.getDoctorEmail() != null &&
                    !dto.getDoctorEmail().equals(user.getEmail())) {

                if (userRepository.existsByEmail(dto.getDoctorEmail())) {
                    throw new IllegalArgumentException("Email already exists");
                }

                user.setEmail(dto.getDoctorEmail());
                user.setUsername(dto.getDoctorEmail());
            }

            if (dto.getDoctorName() != null)
                user.setFirstName(dto.getDoctorName());

            if (dto.getPhoneNumber() != null)
                user.setPhoneNumber(dto.getPhoneNumber());

            if (isAdmin && dto.getIsActive() != null)
                user.setIsActive(dto.getIsActive());

            if (isAdmin) {

                if (dto.getCompanyId() != null) {
                    CompanyProfile company = companyRepository.findById(dto.getCompanyId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid company ID"));
                    doctor.setCompany(company);
                }

                if (dto.getBranchId() != null) {
                    Branch branch = branchRepository.findById(dto.getBranchId())
                            .orElseThrow(() -> new IllegalArgumentException("Invalid branch ID"));
                    doctor.setBranch(branch);
                }
            }

            // Update core expertise if provided
            if (dto.getCoreExpertiseId() != null) {
                DoctorCoreExpertise coreExpertise = doctorCoreExpertiseRepository.findById(dto.getCoreExpertiseId())
                        .orElseThrow(() -> new IllegalArgumentException("Invalid core expertise ID"));
                doctor.setCoreExpertise(coreExpertise);
            } else if (dto.getCoreExpertiseId() == null && doctor.getCoreExpertise() != null) {
                // Allow removing core expertise
                doctor.setCoreExpertise(null);
            }

            doctorRepository.save(doctor);

            logger.info("Doctor updated successfully [doctorId={}]", doctorId);
            return baseResponse.successResponse(
                    "Doctor updated successfully"
            );

        } catch (IllegalArgumentException e) {

            logger.warn("Doctor update failed [doctorId={}] | {}", doctorId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (DataIntegrityViolationException e) {
            logger.warn("Doctor update failed due to data integrity violation [doctorId={}]", doctorId, e);
            String errorMessage = parseDatabaseConstraintError(e);
            return baseResponse.errorResponse(HttpStatus.CONFLICT, errorMessage);

        } catch (Exception e) {

            logger.error("Unexpected error while updating doctor", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update doctor"
            );
        }
    }



    @Override
    public ResponseEntity<?> getDoctorById(Long doctorId) {

        logger.info("Fetching doctor [doctorId={}]", doctorId);

        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            return baseResponse.successResponse(
                    "Doctor fetched successfully",
                    convertToResponseDTO(doctor)
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllDoctors(Integer pageNo, Integer pageSize, Long branchId) {
        User currentUser = DbUtill.getCurrentUser();
        PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

        logger.info("Fetching doctors [pageNo={}, pageSize={}, branchId={}]", pageNo, pageSize, branchId);

        Page<Doctor> page;
        
        if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
            // Super Admin and Super Admin Manager can see all doctors
            page = doctorRepository.findAll(pageRequest);
            logger.info("SUPER_ADMIN accessing all doctors system-wide");
        } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
            // SAAS_ADMIN and SAAS_ADMIN_MANAGER: if branchId is provided, fetch branch-wise; otherwise company-wise
            if (branchId != null) {
                // Validate branch access
                Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                if (!isBranchAccessibleToUser(branch, currentUser)) {
                    return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                            "You don't have permission to access this branch's doctors");
                }
                page = doctorRepository.findByBranchId(branchId, pageRequest);
                logger.info("SAAS_ADMIN accessing doctors for branch ID: {}", branchId);
            } else {
                // Fetch company-wise
                Long companyId = DbUtill.getLoggedInCompanyId();
                page = doctorRepository.findByCompanyId(companyId, pageRequest);
                logger.info("SAAS_ADMIN accessing doctors for company ID: {}", companyId);
            }
        } else {
            if (currentUser.getBranch() != null) {
                page = doctorRepository.findByBranchId(currentUser.getBranch().getId(), pageRequest);
                logger.info("{} accessing doctors for branch ID: {}", currentUser.getRole(), currentUser.getBranch().getId());
            } else {
                page = Page.empty();
                logger.warn("{} user has no branch assignment", currentUser.getRole());
            }
        }

        String successMessage;
        if (rbacUtil.isSuperAdmin()) {
            successMessage = "All doctors fetched successfully (system-wide access)";
        } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
            if (branchId != null) {
                successMessage = "Branch doctors fetched successfully";
            } else {
                successMessage = "Company doctors fetched successfully";
            }
        } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
            if (currentUser.getBranch() != null) {
                successMessage = "Branch doctors fetched successfully";
            } else {
                successMessage = "No branch assignment found";
            }
        } else {
            if (currentUser.getBranch() != null) {
                successMessage = "Branch doctors fetched successfully";
            } else {
                successMessage = "No branch assignment found";
            }
        }

        return baseResponse.successResponse(
                successMessage,
                DbUtill.buildPaginatedResponse(
                        page,
                        page.getContent().stream().map(this::convertToResponseDTO).toList()
                )
        );
    }
//
//    @Override
//    public ResponseEntity<?> getAllDoctorsMinimal() {
//        User currentUser = DbUtill.getCurrentUser();
//        List<Doctor> doctors;
//
//        if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
//            if (currentUser.getBranch() != null) {
//                doctors = doctorRepository.findByBranchId(currentUser.getBranch().getId());
//            } else {
//                doctors = new ArrayList<>();
//            }
//        } else if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
//            // Super Admin and Super Admin Manager can see all doctors
//            doctors = doctorRepository.findAll();
//        } else {
//            // Other roles see only their company's doctors
//            Long companyId = DbUtill.getLoggedInCompanyId();
//            doctors = doctorRepository.findByCompanyId(companyId);
//        }
//
//        List<DoctorMinimalDTO> minimalDoctors = doctors.stream()
//                .map(this::convertToMinimalDTO)
//                .collect(Collectors.toList());
//
//        return baseResponse.successResponse(
//                "Doctors fetched successfully",
//                minimalDoctors
//        );
//    }

    @Override
    public ResponseEntity<?> getDoctorsByDepartment(
            String department, Integer pageNo, Integer pageSize) {
        User currentUser = DbUtill.getCurrentUser();
        Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

        Page<Doctor> page;
        
        if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
            if (currentUser.getBranch() != null) {
                page = doctorRepository.findByBranchIdAndDepartment(currentUser.getBranch().getId(), department, pageable);
            } else {
                page = Page.empty();
            }
        } else if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
            // Super Admin and Super Admin Manager can see all doctors by department
            page = doctorRepository.findByDepartment(department, pageable);
        } else {
            // Other roles see only their company's doctors by department
            Long companyId = DbUtill.getLoggedInCompanyId();
            page = doctorRepository.findByCompanyIdAndDepartment(companyId, department, pageable);
        }

        return baseResponse.successResponse(
                "Doctors fetched successfully",
                DbUtill.buildPaginatedResponse(
                        page,
                        page.getContent().stream().map(this::convertToResponseDTO).toList()
                )
        );
    }

    @Override
    public ResponseEntity<?> activateOrDeactivateDoctor(Long doctorId, Boolean active) {

        try {
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));

            boolean isAdmin = isAdmin();

            validateAccess(isAdmin, true);


            doctor.setIsActive(active);
            doctorRepository.save(doctor);

            return baseResponse.successResponse(
                    "Doctor status updated successfully"
            );

        } catch (IllegalArgumentException e) {
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> deleteDoctor(Long doctorId) {

        if (!doctorRepository.existsById(doctorId)) {
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    "Doctor not found"
            );
        }

        doctorRepository.deleteById(doctorId);

        return baseResponse.successResponse("Doctor deleted successfully");
    }

    @Override
    public ResponseEntity<?> searchDoctor(DoctorSearchType type, String value) {

        Long branchId = null;
        Long companyId = null;
        UserRole role = DbUtill.getLoggedInUserOriginalRole();

        boolean isAdmin = role.equals(UserRole.SAAS_ADMIN_MANAGER) || role.equals(UserRole.SAAS_ADMIN);

        if (isAdmin) {
            companyId = DbUtill.getLoggedInCompanyId();
        } else {
           branchId = DbUtill.getLoggedInBranchId();
        }

        Object result = switch (type) {
            case NAME -> isAdmin
                ? doctorRepository.findDoctorsByNameAndCompanyId(value, companyId)
                : doctorRepository.findDoctorsByNameAndBranchId(value, branchId);

            case EMAIL -> isAdmin
                ? doctorRepository.findByDoctorEmailAndCompanyId(value, companyId)
                : doctorRepository.findByDoctorEmailAndBranchId(value, branchId);

            case PHONE -> isAdmin
                ? doctorRepository.findByPhoneNumberAndCompanyId(value, companyId)
                : doctorRepository.findByPhoneNumberAndBranchId(value, branchId);

            case REGISTRATION_NO -> isAdmin
                ? doctorRepository.findByRegistrationNumberAndCompanyId(value, companyId)
                : doctorRepository.findByRegistrationNumberAndBranchId(value, branchId);

            case SPECIALIZATION -> isAdmin
                ? doctorRepository.findBySpecializationContainingIgnoreCaseAndCompanyId(value, companyId)
                : doctorRepository.findBySpecializationContainingIgnoreCaseAndBranchId(value, branchId);

            case GLOBAL -> isAdmin
                ? doctorRepository.searchGloballyByCompany(value, companyId)
                : doctorRepository.searchGloballyByBranch(value, branchId);

            default -> null;
        };

        String message = (result == null ||
            (result instanceof List<?> list && list.isEmpty()))
            ? "No doctors found for given search criteria"
            : "Doctor search completed successfully";

        return baseResponse.successResponse(message, result);
    }

    private Doctor mapToDoctorEntity(
            DoctorDTO dto,
            User savedUser,
            Long currentUserId,
            CompanyProfile company,
            Branch branch
    ) {

        Doctor doctor = new Doctor();

        doctor.setUser(savedUser);
        doctor.setCreatedBy(currentUserId);

        doctor.setPrefix(dto.getPrefix());
        doctor.setDoctorName(dto.getDoctorName());
        doctor.setDoctorEmail(dto.getDoctorEmail());
        doctor.setPhoneNumber(dto.getPhoneNumber());
        doctor.setGender(dto.getGender());
        doctor.setDateOfBirth(dto.getDateOfBirth());

        doctor.setSpecialization(dto.getSpecialization());
        doctor.setSubSpecialization(dto.getSubSpecialization());
        doctor.setRegistrationNumber(dto.getRegistrationNumber());
        doctor.setExperienceYears(dto.getExperienceYears());

        doctor.setQualification(dto.getQualification());
        doctor.setUniversity(dto.getUniversity());

        doctor.setDepartment(dto.getDepartment());
        doctor.setConsultationRoom(dto.getConsultationRoom());
        doctor.setShiftTiming(dto.getShiftTiming());

        doctor.setConsultationFee(dto.getConsultationFee());
        doctor.setOnlineConsultationAvailable(
                Boolean.TRUE.equals(dto.getOnlineConsultationAvailable())
        );

        doctor.setJoiningDate(
                dto.getJoiningDate() != null ? dto.getJoiningDate() : LocalDate.now()
        );

        doctor.setIsActive(true);

        // Set core expertise if provided
        if (dto.getCoreExpertiseId() != null) {
            DoctorCoreExpertise coreExpertise = doctorCoreExpertiseRepository.findById(dto.getCoreExpertiseId())
                    .orElseThrow(() -> new IllegalArgumentException("Invalid core expertise ID"));
            doctor.setCoreExpertise(coreExpertise);
        }

        doctor.setCompany(company);
        doctor.setBranch(branch);

        return doctorRepository.save(doctor);
    }


    private DoctorResponseDTO convertToResponseDTO(Doctor doctor) {

        DoctorResponseDTO dto = new DoctorResponseDTO();

        dto.setDoctorId(doctor.getId());
        dto.setPrefix(doctor.getPrefix());

        if (doctor.getUser() != null) {
            dto.setUserId(doctor.getUser().getId());
            dto.setDoctorName(
                    doctor.getUser().getFirstName() + " " +
                            (doctor.getUser().getLastName() != null ? doctor.getUser().getLastName() : "")
            );
        }

        dto.setCreatedBy(doctor.getCreatedBy());

        dto.setDoctorEmail(doctor.getDoctorEmail());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setGender(doctor.getGender());
        dto.setDateOfBirth(doctor.getDateOfBirth());

        dto.setSpecialization(doctor.getSpecialization());
        dto.setSubSpecialization(doctor.getSubSpecialization());
        dto.setRegistrationNumber(doctor.getRegistrationNumber());
        dto.setExperienceYears(doctor.getExperienceYears());
        dto.setQualification(doctor.getQualification());
        dto.setUniversity(doctor.getUniversity());

        dto.setDepartment(doctor.getDepartment());
        dto.setConsultationRoom(doctor.getConsultationRoom());
        dto.setShiftTiming(doctor.getShiftTiming());

        dto.setConsultationFee(doctor.getConsultationFee());
        dto.setOnlineConsultationAvailable(doctor.getOnlineConsultationAvailable());

        dto.setActive(doctor.getIsActive());
        dto.setJoiningDate(doctor.getJoiningDate());

        if (doctor.getCompany() != null) {
            dto.setCompanyId(doctor.getCompany().getId());
            dto.setCompanyName(doctor.getCompany().getCompanyName());
        }

        if (doctor.getBranch() != null) {
            dto.setBranchId(doctor.getBranch().getId());
            dto.setBranchName(doctor.getBranch().getBranchName());
        }

        if (doctor.getCoreExpertise() != null) {
            dto.setCoreExpertiseId(doctor.getCoreExpertise().getId());
            dto.setCoreExpertiseName(doctor.getCoreExpertise().getExpertiseName());
            dto.setCoreExpertiseCategory(doctor.getCoreExpertise().getCategory());
        }

        return dto;
    }


    private DoctorMinimalDTO mapToMinimalDto(Doctor doctor) {
        DoctorMinimalDTO dto = new DoctorMinimalDTO();

        dto.setDoctorId(doctor.getId());
        dto.setPrefix(doctor.getPrefix());
        dto.setDoctorName(doctor.getDoctorName());
        dto.setDoctorEmail(doctor.getDoctorEmail());
        dto.setPhoneNumber(doctor.getPhoneNumber());
        dto.setSpecialization(doctor.getSpecialization());
        dto.setDepartment(doctor.getDepartment());
        dto.setOnlineConsultationAvailable(doctor.getOnlineConsultationAvailable());
        dto.setIsActive(doctor.getIsActive());
        dto.setConsultationRoom(doctor.getConsultationRoom());
        dto.setRegistrationNumber(doctor.getRegistrationNumber());

        if (doctor.getCompany() != null)
            dto.setCompanyId(doctor.getCompany().getId());

        if (doctor.getBranch() != null)
            dto.setBranchId(doctor.getBranch().getId());

        if (doctor.getCoreExpertise() != null) {
            dto.setCoreExpertiseId(doctor.getCoreExpertise().getId());
            dto.setCoreExpertiseName(doctor.getCoreExpertise().getExpertiseName());
        }

        return dto;
    }


    private Doctor getDoctorOrThrow(Long doctorId) {

        Long companyId = DbUtill.getLoggedInCompanyId();

        return doctorRepository
                .findByIdAndCompanyId(doctorId, companyId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
    }

    private boolean isAdmin() {
        return SecurityContextHolder.getContext().getAuthentication()
                .getAuthorities()
                .stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> role.equals("ROLE_ADMIN") || role.equals("ROLE_SUPER_ADMIN") || role.equals("ROLE_SAAS_ADMIN") || role.equals("ROLE_SAAS_ADMIN_MANAGER"));
    }


    private boolean isSelfDoctor(Doctor doctor) {
        Long loggedInUserId = DbUtill.getLoggedInUserId();
        System.out.println("i m from is isSelfDoctor : "+loggedInUserId);
        return doctor.getUser().getId().equals(loggedInUserId);
    }

    private void validateAccess(boolean isAdmin, boolean isSelf) {
        if (!isAdmin && !isSelf) {
            throw new IllegalArgumentException("You are not authorized to update this doctor");
        }
    }

    /**
     * Parse database constraint violation errors to provide user-friendly messages
     * @param e DataIntegrityViolationException
     * @return User-friendly error message
     */
    private String parseDatabaseConstraintError(DataIntegrityViolationException e) {
        String message = getRootCauseMessage(e);

        if (message != null) {
            if (message.toLowerCase().contains("duplicate") && message.toLowerCase().contains("constraint")) {
                if (message.contains("idx_doctor_registration_no")) {
                    return "Registration number already exists. Please use a different registration number.";
                } else if (message.contains("doctor_email")) {
                    return "Doctor email already exists. Please use a different email address.";
                } else if (message.contains("phone_number")) {
                    return "Phone number already exists. Please use a different phone number.";
                } else {
                    return "Duplicate entry detected. Please check your input values.";
                }
            }
            else if (message.contains("violates foreign key constraint")) {
                return "Referenced record does not exist. Please check your input data.";
            }
            else if (message.contains("violates not-null constraint")) {
                return "Required field is missing. Please provide all required information.";
            }
        }
        return "Data validation failed. Please check your input and try again.";
    }

    /**
     * Get the root cause message from an exception
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

    /**
     * Validate that a department exists for the given branch
     * @param departmentName The department name to validate
     * @param branchId The branch ID to check against
     * @throws IllegalArgumentException if department doesn't exist for the branch
     */
    private void validateDepartmentForBranch(String departmentName, Long branchId) {
        boolean departmentExists = departmentRepository.existsByDepartmentNameAndBranch_Id(
                departmentName.trim(), branchId
        );
        
        if (!departmentExists) {
            logger.warn("Department validation failed | departmentName={}, branchId={}", departmentName, branchId);
            throw new IllegalArgumentException(
                    "Department '" + departmentName + "' does not exist for the selected branch. " +
                    "Please create the department first or select a valid department."
            );
        }
        
        logger.debug("Department validation passed | departmentName={}, branchId={}", departmentName, branchId);
    }
}
