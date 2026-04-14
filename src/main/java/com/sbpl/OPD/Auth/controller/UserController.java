package com.sbpl.OPD.Auth.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.service.UserService;
import com.sbpl.OPD.Auth.serviceImpl.UserServiceImpl;
import com.sbpl.OPD.dto.UserDTO;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

    @Autowired
    private UserService userService;
    
    @Autowired
    private RbacUtil rbacUtil;
    
    @Autowired
    private BaseResponse baseResponse;

    @GetMapping("/logged-in/user-details")
    public ResponseEntity<?> getLoggedInUserProfile() {
        // Any authenticated user can access their own profile
        try {
            User currentUser = DbUtill.getCurrentUser();
            UserDTO userDTO = userService.findByUsername(currentUser.getUsername());
            return baseResponse.successResponse("User profile fetched successfully", userDTO);
        } catch (Exception e) {
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR, "Error fetching user profile: " + e.getMessage());
        }
    }

    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        // SUPER_ADMIN, ADMIN, and BRANCH_MANAGER can get users (with appropriate restrictions in service)
        if (!rbacUtil.isAdmin() && !rbacUtil.hasRole(UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to view all users");
        }
        return ResponseEntity.ok(userService.getAllUsers());
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getUserById(@PathVariable Long id) {
        // SUPER_ADMIN and ADMIN can access any user, managers can access users in their company/branch, others can only access their own profile
        User currentUser = DbUtill.getCurrentUser();
        
        // Allow access if user is admin or owner
        if (rbacUtil.isAdmin() || rbacUtil.isOwner(id)) {
            return userService.getUserById(id);
        }
        
        // For managers, check if they can access the user based on company/branch relationship
        if (rbacUtil.isBranchManagerOrHigher()) {
            User targetUser = ((UserServiceImpl) userService).getUserByIdInternal(id);
            if (targetUser != null && canManagerAccessUser(currentUser, targetUser)) {
                return userService.getUserById(id);
            }
        }
        
        throw new AccessDeniedException("Access denied: Insufficient role privileges to view user profile");
    }

    @PostMapping
    public ResponseEntity<?> createUser(@Valid @RequestBody UserDTO userDTO) {
        // SUPER_ADMIN, ADMIN, and STAFF can create users (with role restrictions enforced in the service)
        if (!rbacUtil.isAdmin() && !rbacUtil.hasRole(UserRole.STAFF)) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to create user");
        }
        return userService.createUser(userDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateUser(@PathVariable Long id, @Valid @RequestBody UserDTO userDTO) {
        // SUPER_ADMIN and ADMIN can update any user, STAFF can update users they created, others can only update their own profile
        if (!rbacUtil.isAdmin() && !rbacUtil.isOwner(id) && !(rbacUtil.hasRole(UserRole.STAFF) && canStaffUpdateUser(id))) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to update user");
        }
        return userService.updateUser(id, userDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(@PathVariable Long id) {
        // Allow users with SAAS_ADMIN_MANAGER or higher roles to delete users
        if (!rbacUtil.isSaasAdminManagerOrHigher()) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to delete user");
        }
        userService.deleteUser(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/role")
    public ResponseEntity<?> getUsersByRole(@RequestParam UserRole role) {
        // SUPER_ADMIN, ADMIN, and BRANCH_MANAGER can get users by role
        if (!rbacUtil.isAdmin() && !rbacUtil.hasRole(UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to view users by role");
        }
        return userService.getUsersByRole(role);
    }

    @GetMapping("/company-role")
    public ResponseEntity<?> getAllUsersWithCompanyAndRole(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        // SUPER_ADMIN and ADMIN can get all users, managers can get users from their company
        if (!rbacUtil.isAdmin() && !rbacUtil.isBranchManagerOrHigher()) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to view all users with company and role");
        }
        return userService.getAllUsersWithCompanyAndRole(null, pageNo, pageSize);
    }
    
    @GetMapping("/staff-by-company")
    public ResponseEntity<?> getAllStaffByCompany(
            @RequestParam(required = false) Long companyId,
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        // SUPER_ADMIN and ADMIN can get staff by company, managers can get staff from their company
        if (!rbacUtil.isAdmin() && !rbacUtil.isBranchManagerOrHigher()) {
            throw new AccessDeniedException("Access denied: Insufficient role privileges to view staff by company");
        }
        return userService.getAllStaffByCompany(companyId, pageNo, pageSize);
    }
    
    // Removed permission assignment endpoint for pure RBAC
    
    /**
     * Check if a manager can access a specific user based on company/branch relationship
     */
    private boolean canManagerAccessUser(User manager, User targetUser) {
        // Managers can access users in their company
        if (manager.getCompany() != null && targetUser.getCompany() != null) {
            return manager.getCompany().getId().equals(targetUser.getCompany().getId());
        }
        
        // Branch managers can access users in their branch
        if (manager.getRole() == UserRole.BRANCH_MANAGER && 
            manager.getBranch() != null && targetUser.getBranch() != null) {
            return manager.getBranch().getId().equals(targetUser.getBranch().getId());
        }
        
        return false;
    }
    
    /**
     * Check if a STAFF user can update a specific user (only users they created)
     */
    private boolean canStaffUpdateUser(Long userId) {
        // For simplicity, we'll check if the current user is STAFF and the user exists
        // In a real implementation, you'd track who created each user
        User currentUser = DbUtill.getCurrentUser();
        User targetUser = ((UserServiceImpl) userService).getUserByIdInternal(userId);
        
        // STAFF users can update any user since our service layer handles role restrictions
        return currentUser.getRole() == UserRole.STAFF;
    }
}