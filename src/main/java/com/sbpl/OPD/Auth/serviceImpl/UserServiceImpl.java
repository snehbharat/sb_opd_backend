package com.sbpl.OPD.Auth.serviceImpl;

import com.sbpl.OPD.Auth.dto.LoginDetailsDto;
import com.sbpl.OPD.Auth.dto.LoginRequestDto;
import com.sbpl.OPD.Auth.dto.LoginResponseDto;
import com.sbpl.OPD.Auth.dto.SimpleBranchDto;
import com.sbpl.OPD.Auth.dto.SimpleCompanyDto;
import com.sbpl.OPD.Auth.dto.UserCompanyRoleDto;
import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.Auth.service.UserService;
import com.sbpl.OPD.Auth.utils.JwtService;
import com.sbpl.OPD.dto.UserDTO;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.utils.DbUtill;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;


@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private CompanyProfileRepository companyProfileRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Override
    public UserDetails loadUserByUsername(String username) {
        return userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public ResponseEntity<?> loginViaUsernameAndPassword(LoginRequestDto request) {
        User user = userRepository.findByUsername(request.getUsername())
                .orElse(null);

        if (user ==null){
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,"Invalid username or password");
        }

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new RuntimeException("User account is disabled");
        }

        if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,"Invalid username or password");
        }
        Authentication authentication =
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                        user,
                        null,
                        user.getAuthorities()   // IMPORTANT: User must implement UserDetails
                );

        System.out.println("User username: " + user.getUsername());
        System.out.println("User authorities during login: " + user.getAuthorities());
        System.out.println("Authentication name: " + authentication.getName());
        System.out.println("Authentication authorities: " + authentication.getAuthorities());
        LoginResponseDto response = buildLoginResponse(user, authentication);

        return baseResponse.successResponse("Logged in successfully", response);
    }


    @Override
    public User updateCompanyOnUser(Long userId, Long companyId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        CompanyProfile company = companyProfileRepository.findById(companyId)
                .orElseThrow(() -> new RuntimeException("Company not found"));

        user.setCompany(company);

        return userRepository.save(user);
    }




    @Override
    public ResponseEntity<?> createUser1(UserDTO userDTO) {

        if (userDTO.getRole() == UserRole.SUPER_ADMIN) {
            return baseResponse.errorResponse(HttpStatus.FORBIDDEN,"You cannot create SUPER_ADMIN user");
        }

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());

        // ✅ Encode ONLY here
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        user.setRole(userDTO.getRole());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setIsActive(userDTO.getIsActive() != null ? userDTO.getIsActive() : true);

        userRepository.save(user);
        return baseResponse.successResponse("User created successfully");
    }

    @Override
    public ResponseEntity<?> createUser(UserDTO userDTO) {

        if (EnumSet.of(
                UserRole.SUPER_ADMIN,
                UserRole.SUPER_ADMIN_MANAGER,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER
        ).contains(userDTO.getRole())) {

            return baseResponse.errorResponse(
                    HttpStatus.FORBIDDEN,
                    "You cannot create this role"
            );
        }
        
        User currentUser = getCurrentLoggedInUser();
        
        // STAFF users can only create DOCTOR and PATIENT roles
        if (currentUser.getRole() == UserRole.STAFF && 
            userDTO.getRole() != UserRole.DOCTOR && 
            userDTO.getRole() != UserRole.PATIENT) {
            return baseResponse.errorResponse(
                    HttpStatus.FORBIDDEN,
                    "STAFF users can only create DOCTOR and PATIENT roles"
            );
        }

        Branch branch = null;

        if (userDTO.getRole() != UserRole.SAAS_ADMIN_MANAGER) {

            if (userDTO.getBranchId() == null) {
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Branch is required"
                );
            }

            branch = branchRepository.findByIdAndClinic_id(
                    userDTO.getBranchId(),
                    currentUser.getCompany().getId()
            );

            if (branch == null) {
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Branch not in this clinic"
                );
            }
        }

        if (!canCreateUser(currentUser, userDTO.getRole())) {
            return baseResponse.errorResponse(
                    HttpStatus.FORBIDDEN,
                    "Insufficient permissions to create user with role: " + userDTO.getRole()
            );
        }



        if (userRepository.existsByUsername(userDTO.getUsername())) {
            return baseResponse.errorResponse(HttpStatus.CONFLICT,"Username already exists");
        }
        if (userRepository.existsByEmail(userDTO.getEmail())) {
            return baseResponse.errorResponse(HttpStatus.CONFLICT,"Email already exists");
        }
        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setRole(userDTO.getRole());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setAddress(userDTO.getAddress());
        user.setDepartment(userDTO.getDepartment());
        user.setGender(userDTO.getGender());
        user.setDateOfBirth(userDTO.getDateOfBirth());
        user.setEmployeeId(generateEmployeeId());


        user.setBranch(branch);
        user.setCompany(currentUser.getCompany());

        user.setIsActive(userDTO.getIsActive() != null ? userDTO.getIsActive() : true);

        userRepository.save(user);
//        convertToDTO(savedUser);
        return baseResponse.successResponse("User created successfully");
    }

    @Override
    public ResponseEntity<?> updateUser(Long id, UserDTO userDTO) {

        User currentUser = getCurrentLoggedInUser();

        User user = userRepository.findById(id).orElse(null);

        if (user == null) {
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    "User not found"
            );
        }

        // 🔒 Prevent updating protected roles
        if (userDTO.getRole() != null &&
                EnumSet.of(
                        UserRole.SUPER_ADMIN,
                        UserRole.SUPER_ADMIN_MANAGER
                ).contains(userDTO.getRole())) {

            return baseResponse.errorResponse(
                    HttpStatus.FORBIDDEN,
                    "You cannot assign this role"
            );
        }

        // 🔐 Optional: prevent lower roles from updating higher roles
        if (!canCreateUser(currentUser, user.getRole())) {
            return baseResponse.errorResponse(
                    HttpStatus.FORBIDDEN,
                    "You do not have permission to update this user"
            );
        }

        // 🔎 Username uniqueness check
        if (userDTO.getUsername() != null &&
                userRepository.existsByUsernameAndIdNot(userDTO.getUsername(), id)) {

            return baseResponse.errorResponse(
                    HttpStatus.CONFLICT,
                    "Username already exists"
            );
        }

        // 🔎 Email uniqueness check
        if (userDTO.getEmail() != null &&
                userRepository.existsByEmailAndIdNot(userDTO.getEmail(), id)) {

            return baseResponse.errorResponse(
                    HttpStatus.CONFLICT,
                    "Email already exists"
            );
        }

        // 🔎 Phone uniqueness check
        if (userDTO.getPhoneNumber() != null &&
                userRepository.existsByPhoneNumberAndIdNot(userDTO.getPhoneNumber(), id)) {

            return baseResponse.errorResponse(
                    HttpStatus.CONFLICT,
                    "Phone number already exists"
            );
        }

        // 🔎 EmployeeId uniqueness check
        if (userDTO.getEmployeeId() != null &&
                userRepository.existsByEmployeeIdAndIdNot(userDTO.getEmployeeId(), id)) {

            return baseResponse.errorResponse(
                    HttpStatus.CONFLICT,
                    "Employee ID already exists"
            );
        }

        // ✅ Update allowed fields
        if (userDTO.getUsername() != null)
            user.setUsername(userDTO.getUsername());

        if (userDTO.getEmail() != null)
            user.setEmail(userDTO.getEmail());

        if (userDTO.getPassword() != null && !userDTO.getPassword().isEmpty())
            user.setPassword(passwordEncoder.encode(userDTO.getPassword()));

        if (userDTO.getRole() != null)
            user.setRole(userDTO.getRole());

        if (userDTO.getFirstName() != null)
            user.setFirstName(userDTO.getFirstName());

        if (userDTO.getLastName() != null)
            user.setLastName(userDTO.getLastName());

        if (userDTO.getPhoneNumber() != null)
            user.setPhoneNumber(userDTO.getPhoneNumber());

        if (userDTO.getAddress() != null)
            user.setAddress(userDTO.getAddress());

        if (userDTO.getDepartment() != null)
            user.setDepartment(userDTO.getDepartment());

        if (userDTO.getGender() != null)
            user.setGender(userDTO.getGender());

        if (userDTO.getEmployeeId() != null)
            user.setEmployeeId(userDTO.getEmployeeId());

        if (userDTO.getDateOfBirth() != null)
            user.setDateOfBirth(userDTO.getDateOfBirth());

        if (userDTO.getIsActive() != null)
            user.setIsActive(userDTO.getIsActive());

        try {

            userRepository.save(user);

            return baseResponse.successResponse("User updated successfully");

        } catch (DataIntegrityViolationException e) {

            String errorMessage = parseDatabaseConstraintError(e);

            return baseResponse.errorResponse(
                    HttpStatus.CONFLICT,
                    errorMessage
            );
        }
    }


    @Override
    public ResponseEntity<?> getUserById(Long id) {
        User user = userRepository.findById(id)
                .orElse(null);
        if (user==null){
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND,"User not found");
        }
        return baseResponse.successResponse("user fetched successfully",convertToDTO(user));
    }

    public ResponseEntity<?>getUserByClinicAndBranch(Long clinicId,Long branchId){
        return baseResponse.errorResponse(HttpStatus.NOT_IMPLEMENTED, "Method not implemented");
    }

    @Override
    public List<UserDTO> getAllUsers() {
        return userRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    @Override
    public void deleteUser(Long id) {
        User currentUser = getCurrentLoggedInUser();
        User userToDelete = userRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("User not found"));
        
        // Check if current user can delete the target user based on role hierarchy
        if (!canCreateUser(currentUser, userToDelete.getRole())) {
            throw new RuntimeException("You do not have permission to delete this user");
        }
        
        userToDelete.setIsActive(false);
        userRepository.save(userToDelete);
    }


    @Override
    public UserDTO findByUsername(String username) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("User not found"));
        return convertToDTO(user);
    }

    /**
     * Internal method to get user by ID without security checks
     * Used for permission validation purposes
     */
    public User getUserByIdInternal(Long id) {
        return userRepository.findById(id).orElse(null);
    }

    @Override
    public ResponseEntity<?> getUsersByRole(UserRole role) {
        try {
            List<UserDTO> users = userRepository.findByRoleWithCompanyAndBranch(role).stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            return baseResponse.successResponse("Users with role " + role + " fetched successfully", users);
        } catch (Exception e) {
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to fetch users by role: " + e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllUsersWithCompanyAndRole(Long companyId, Integer pageNo, Integer pageSize) {
        try {
            // Get currently logged-in user
            User currentUser = getCurrentLoggedInUser();
            
            // Build page request
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            
            Page<User> userPage;
            String message;
            
            // If super admin, show all users
            if (currentUser.getRole() == UserRole.SUPER_ADMIN || 
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                
                if (companyId != null) {
                    // Super admin filtering by specific company
                    List<User> filteredUsers = userRepository.findAll().stream()
                            .filter(user -> user.getCompany() != null && 
                                          user.getCompany().getId().equals(companyId))
                            .collect(Collectors.toList());
                    
                    // Convert to page manually for filtered results
                    int start = (int) pageRequest.getOffset();
                    int end = Math.min((start + pageRequest.getPageSize()), filteredUsers.size());
                    List<User> pagedUsers = filteredUsers.subList(start, end);
                    userPage = new PageImpl<>(pagedUsers, pageRequest, filteredUsers.size());
                    message = "Users with company relationship fetched successfully";
                } else {
                    // Super admin - show all users with pagination
                    userPage = userRepository.findAll(pageRequest);
                    message = "All users with company and role information fetched successfully";
                }
                
            } else {
                // For admin and other roles, show only users from their company
                if (currentUser.getCompany() == null) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Current user is not associated with any company"
                    );
                }
                
                Long userCompanyId = currentUser.getCompany().getId();
                List<User> companyUsers = userRepository.findAll().stream()
                        .filter(user -> user.getCompany() != null && 
                                      user.getCompany().getId().equals(userCompanyId))
                        .collect(Collectors.toList());
                
                // Convert to page manually for filtered results
                int start = (int) pageRequest.getOffset();
                int end = Math.min((start + pageRequest.getPageSize()), companyUsers.size());
                List<User> pagedUsers = companyUsers.subList(start, end);
                userPage = new PageImpl<>(pagedUsers, pageRequest, companyUsers.size());
                message = "Users from your company fetched successfully";
            }
            
            List<UserCompanyRoleDto> usersWithCompanyRole = userPage.getContent().stream()
                    .map(this::convertToUserCompanyRoleDto)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(userPage, usersWithCompanyRole);
            
            return baseResponse.successResponse(message, response);
            
        } catch (Exception e) {
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to fetch users with company and role information: " + e.getMessage()
            );
        }
    }
    
    @Override
    public ResponseEntity<?> getAllStaffByCompany(Long companyId, Integer pageNo, Integer pageSize) {
        try {
            // Get currently logged-in user
            User currentUser = getCurrentLoggedInUser();
            
            // Build page request
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            
            // Determine company ID to filter by
            Long targetCompanyId;
            if (currentUser.getRole() == UserRole.SUPER_ADMIN || 
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                if (companyId == null) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Company ID is required for super admin"
                    );
                }
                targetCompanyId = companyId;
            } else {
                // For admin and other roles, use their company
                if (currentUser.getCompany() == null) {
                    return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "Current user is not associated with any company"
                    );
                }
                targetCompanyId = currentUser.getCompany().getId();
            }
            
            // Define staff roles (excluding admin roles)
            List<UserRole> staffRoles = Arrays.asList(
                UserRole.BILLING_STAFF,
                UserRole.RECEPTIONIST,
                UserRole.BRANCH_MANAGER,
                UserRole.STAFF
            );
            
            // Fetch all users from the company
            List<User> allCompanyUsers = userRepository.findAllManagementAndStaff(staffRoles).stream()
                    .filter(user -> user.getCompany() != null && 
                                  user.getCompany().getId().equals(targetCompanyId))
                    .collect(Collectors.toList());
            
            // Filter only staff users (non-admin)
            List<User> staffUsers = allCompanyUsers.stream()
                    .filter(user -> staffRoles.contains(user.getRole()))
                    .collect(Collectors.toList());
            
            // Apply pagination manually
            int start = (int) pageRequest.getOffset();
            int end = Math.min((start + pageRequest.getPageSize()), staffUsers.size());
            List<User> pagedStaffUsers = staffUsers.subList(start, end);
            
            Page<User> staffPage = new PageImpl<>(pagedStaffUsers, pageRequest, staffUsers.size());
            
            List<UserCompanyRoleDto> staffWithCompanyRole = staffPage.getContent().stream()
                    .map(this::convertToUserCompanyRoleDto)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(staffPage, staffWithCompanyRole);
            
            return baseResponse.successResponse(
                "Staff members from company fetched successfully", 
                response
            );
            
        } catch (Exception e) {
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Failed to fetch staff by company: " + e.getMessage()
            );
        }
    }

    /**
     * Get the currently logged-in user from security context
     *
     * @return User object of the currently logged-in user
     */
    private User getCurrentLoggedInUser() {
        Long currentUserId = DbUtill.getLoggedInUserId();
        return userRepository.findById(currentUserId)
                .orElseThrow(() -> new IllegalArgumentException("Current user not found"));
    }

    /**
     * Check if the current user has permission to create a user with the specified role
     * according to the hierarchical structure:
     * - SUPER_ADMIN can create any role
     * - ADMIN can create SAAS_ADMIN_MANAGER, BRANCH_MANAGER, DOCTOR, RECEPTIONIST, BILLING_STAFF, etc.
     * - SAAS_ADMIN_MANAGER can create BRANCH_MANAGER, DOCTOR, RECEPTIONIST, BILLING_STAFF, etc.
     * - BRANCH_MANAGER can create DOCTOR, RECEPTIONIST, BILLING_STAFF for their branch
     * - Other roles cannot create users
     *
     * @param currentUser The currently logged-in user
     * @param newUserRole The role of the user being created
     * @return true if the current user can create the new user, false otherwise
     */
    private boolean canCreateUser(User currentUser, UserRole newUserRole) {
      return switch (currentUser.getRole()) {
        case SUPER_ADMIN ->
          // SUPER_ADMIN can create any role
            true;
        case SAAS_ADMIN_MANAGER ->
          // SAAS_ADMIN_MANAGER can create roles below their level (including SAAS_ADMIN)
            newUserRole != UserRole.SUPER_ADMIN && newUserRole != UserRole.SUPER_ADMIN_MANAGER
                && newUserRole != UserRole.SAAS_ADMIN_MANAGER;
        case SAAS_ADMIN ->
          // SAAS_ADMIN can create roles below their level (but not SAAS_ADMIN_MANAGER)
            newUserRole != UserRole.SUPER_ADMIN && newUserRole != UserRole.SUPER_ADMIN_MANAGER
                && newUserRole != UserRole.SAAS_ADMIN && newUserRole != UserRole.SAAS_ADMIN_MANAGER;
        case BRANCH_MANAGER ->
          // BRANCH_MANAGER can create roles below their level for their branch
            newUserRole != UserRole.SUPER_ADMIN && newUserRole != UserRole.SUPER_ADMIN_MANAGER
                && newUserRole != UserRole.SAAS_ADMIN && newUserRole != UserRole.SAAS_ADMIN_MANAGER
                && newUserRole != UserRole.BRANCH_MANAGER;
        case STAFF ->
          // STAFF can create DOCTOR and PATIENT (CUSTOMER) roles
            newUserRole == UserRole.DOCTOR || newUserRole == UserRole.PATIENT;
        default ->
          // Other roles cannot create users
            false;
      };
    }

    private LoginResponseDto buildLoginResponse(
            User user,
            Authentication authentication
    ) {

        // 1️⃣ JWT Payload
        Map<String, Object> payload = new HashMap<>();
        payload.put("userId", user.getId());
        payload.put("username", user.getUsername());
        Optional.of(user)
            .map(User::getEmail)
            .ifPresent(email -> payload.put("email", email));

        Optional.of(user)
            .map(User::getPhoneNumber)
            .ifPresent(phone -> payload.put("mobile", phone));

        payload.put("role", user.getRole().name());

        payload.put(
            "companyId",
            user.getCompany() != null ? user.getCompany().getId() : null
        );

        payload.put("branchId", user.getBranch() != null ? user.getBranch().getId() : null);

        String accessToken = jwtService.generateAccessToken(authentication, payload);
        String refreshToken = jwtService.generateRefreshToken(authentication, payload);

        LoginDetailsDto loginDetails = LoginDetailsDto.builder()
                .userId(user.getId().toString())
                .username(user.getUsername())
                .fullName(user.getFirstName() + " " + user.getLastName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();

        return new LoginResponseDto(
                accessToken,
                refreshToken,
                loginDetails
        );
    }


    private UserDTO convertToDTO(User user) {
        UserDTO dto = new UserDTO();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());

        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setAddress(user.getAddress());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setDepartment(user.getDepartment());
        
        // Set company information if available
        if (user.getCompany() != null) {
            dto.setCompanyId(user.getCompany().getId());
            dto.setCompanyName(user.getCompany().getCompanyName());
        }
        
        // Set branch information if available
        if (user.getBranch() != null) {
            dto.setBranchId(user.getBranch().getId());
            dto.setBranchName(user.getBranch().getBranchName());
        }

        return dto;
    }

    private UserCompanyRoleDto convertToUserCompanyRoleDto(User user) {
        UserCompanyRoleDto dto = new UserCompanyRoleDto();
        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setEmail(user.getEmail());
        dto.setFirstName(user.getFirstName());
        dto.setLastName(user.getLastName());
        dto.setPhoneNumber(user.getPhoneNumber());
        dto.setRole(user.getRole());
        dto.setIsActive(user.getIsActive());
        dto.setDateOfBirth(user.getDateOfBirth());
        dto.setGender(user.getGender());
        dto.setAddress(user.getAddress());
        dto.setEmployeeId(user.getEmployeeId());
        dto.setDepartment(user.getDepartment());
        
        if (user.getCompany() != null) {
            SimpleCompanyDto companyDto = new SimpleCompanyDto();
            companyDto.setId(user.getCompany().getId());
            companyDto.setCompanyName(user.getCompany().getCompanyName());
            if (user.getCompany().getUser() != null) {
                companyDto.setAdminUserId(user.getCompany().getUser().getId());
            }
            dto.setCompany(companyDto);
        }
        
        if (user.getBranch() != null) {
            SimpleBranchDto branchDto = new SimpleBranchDto();
            branchDto.setId(user.getBranch().getId());
            branchDto.setBranchName(user.getBranch().getBranchName());
            if (user.getBranch().getClinic() != null) {
                branchDto.setClinicId(user.getBranch().getClinic().getId());
            }
            dto.setBranch(branchDto);
        }
        
        return dto;
    }

    public User createUserInternal(UserDTO userDTO) {

        if (userRepository.existsByUsername(userDTO.getUsername())) {
            throw new IllegalArgumentException("Username already exists");
        }

        if (userRepository.existsByEmail(userDTO.getEmail())) {
            throw new IllegalArgumentException("Email already exists");
        }
        User currentUser = getCurrentLoggedInUser();
        Branch branch = branchRepository.findByIdAndClinic_id(
                userDTO.getBranchId(),
                currentUser.getCompany().getId()
        );

        User user = new User();
        user.setUsername(userDTO.getUsername());
        user.setEmail(userDTO.getEmail());
        user.setPassword(passwordEncoder.encode(userDTO.getPassword()));
        user.setCompany(currentUser.getCompany());
        user.setBranch(branch);
        user.setRole(userDTO.getRole());
        user.setFirstName(userDTO.getFirstName());
        user.setLastName(userDTO.getLastName());
        user.setPhoneNumber(userDTO.getPhoneNumber());
        user.setIsActive(true);

        return userRepository.save(user);
    }


    @Transactional
    public User updateSelf(Long userId, UserDTO dto) {

        User user = getUserOrThrow(userId);

        updateCommonFields(user, dto);
        return userRepository.save(user);
    }

    @Transactional
    public User updateUserByAdmin(Long userId, UserDTO dto) {

        User user = getUserOrThrow(userId);

        updateCommonFields(user, dto);

        // Admin-only fields
        if (dto.getRole() != null) {
            user.setRole(dto.getRole());
        }

        if (dto.getIsActive() != null) {
            user.setIsActive(dto.getIsActive());
        }

        return userRepository.save(user);
    }



    private void updateCommonFields(User user, UserDTO dto) {

        if (dto.getFirstName() != null)
            user.setFirstName(dto.getFirstName());

        if (dto.getLastName() != null)
            user.setLastName(dto.getLastName());

        if (dto.getPhoneNumber() != null)
            user.setPhoneNumber(dto.getPhoneNumber());

        if (dto.getIsActive() != null)
            user.setIsActive(dto.getIsActive());

        if (dto.getEmail() != null &&
                !dto.getEmail().equals(user.getEmail())) {

            if (userRepository.existsByEmail(dto.getEmail())) {
                throw new IllegalArgumentException("Email already exists");
            }

            user.setEmail(dto.getEmail());
            user.setUsername(dto.getEmail());
        }
    }

    protected User getUserOrThrow(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with id: " + userId));
    }

    /**
     * Parse database constraint violation errors to provide user-friendly messages
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
                if (message.contains("user_username")) {
                    return "Username already exists. Please use a different username.";
                } else if (message.contains("user_email")) {
                    return "Email already exists. Please use a different email address.";
                } else {
                    return "Duplicate entry detected. Please check your input values.";
                }
            }
            // Handle other constraint violations
            else if (message.toLowerCase().contains("foreign key")) {
                return "Referenced record does not exist. Please check your input data.";
            }
            else if (message.toLowerCase().contains("not-null") || message.toLowerCase().contains("null value")) {
                return "Required field is missing. Please provide all required information.";
            }
        }
        

        
        // Default message if we can't parse the specific constraint
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


    private String generateEmployeeId() {

        Long count = userRepository.count() + 1;

        return String.format("EMP-%04d", count);
    }



}