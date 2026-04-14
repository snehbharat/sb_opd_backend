package com.sbpl.OPD.utils;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import org.springframework.stereotype.Service;

/**
 * Centralized Role-Based Access Control utility service.
 * Provides methods to check user permissions and roles for access control.
 */
@Service
public class RbacUtil {

    // Removed PermissionService dependency for pure RBAC

    /**
     * Check if the current user has the specified role.
     */
    public boolean hasRole(UserRole requiredRole) {
        User currentUser = DbUtill.getCurrentUser();
        return currentUser.getRole() == requiredRole;
    }

    /**
     * Check if the current user has any of the specified roles.
     */
    public boolean hasAnyRole(UserRole... requiredRoles) {
        User currentUser = DbUtill.getCurrentUser();
        for (UserRole role : requiredRoles) {
            if (currentUser.getRole() == role) {
                return true;
            }
        }
        return false;
    }

    // Removed permission-related methods for pure RBAC

    // Removed role-or-permission methods for pure RBAC

    /**
     * Check if the current user is the owner of the resource (based on ID comparison).
     */
    public boolean isOwner(Long resourceId) {
        User currentUser = DbUtill.getCurrentUser();
        return currentUser.getId().equals(resourceId);
    }

    /**
     * Check if the current user is a super admin.
     */
    public boolean isSuperAdmin() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER);
    }

    /**
     * Check if the current user is an admin (either SUPER_ADMIN, SUPER_ADMIN_MANAGER, or SAAS_ADMIN).
     */
    public boolean isAdmin() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER, UserRole.SAAS_ADMIN);
    }

    /**
     * Check if the current user is SAAS admin manager or higher.
     */
    public boolean isSaasAdminManagerOrHigher() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER);
    }

    /**
     * Check if the current user is branch manager or higher.
     */
    public boolean isBranchManagerOrHigher() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER, UserRole.SAAS_ADMIN, UserRole.BRANCH_MANAGER);
    }

    /**
     * Check if the current user is doctor or higher.
     */
    public boolean isDoctorOrHigher() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER, UserRole.SAAS_ADMIN, UserRole.BRANCH_MANAGER, UserRole.DOCTOR);
    }

    /**
     * Check if the current user is staff or higher.
     */
    public boolean isStaffOrHigher() {
        return hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SUPER_ADMIN_MANAGER, UserRole.SAAS_ADMIN_MANAGER, UserRole.SAAS_ADMIN, UserRole.BRANCH_MANAGER, UserRole.STAFF, UserRole.DOCTOR, UserRole.RECEPTIONIST, UserRole.BILLING_STAFF);
    }

    /**
     * Check if the current user can access medical records.
     * Includes RECEPTIONIST, BILLING_STAFF, PATIENT, and doctor or higher roles.
     */
    public boolean canAccessMedicalRecords() {
        return hasAnyRole(UserRole.RECEPTIONIST, UserRole.BILLING_STAFF, UserRole.PATIENT) || isDoctorOrHigher();
    }

    /**
     * Check if user has access to a specific branch.
     * SUPER_ADMIN and SUPER_ADMIN_MANAGER: Access to all branches
     * SAAS_ADMIN and SAAS_ADMIN_MANAGER: Access to all branches
     * BRANCH_MANAGER: Access only to their assigned branch
     * Other roles: Access based on their branch assignment
     */
    public boolean hasBranchAccess(Long branchId) {
        User currentUser = DbUtill.getCurrentUser();

        // Super admins and SAAS admins have access to all branches
        if (isSuperAdmin() || isAdmin() || isSaasAdminManagerOrHigher()) {
            return true;
        }

        // Branch managers can only access their assigned branch
        if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
            return currentUser.getBranch() != null && currentUser.getBranch().getId().equals(branchId);
        }

        // Other roles can access their assigned branch
        return currentUser.getBranch() != null && currentUser.getBranch().getId().equals(branchId);
    }

    /**
     * Check if user has access to a specific company/clinic.
     * SUPER_ADMIN and SUPER_ADMIN_MANAGER: Access to all companies
     * SAAS_ADMIN and SAAS_ADMIN_MANAGER: Access to their assigned company
     * BRANCH_MANAGER and others: Access to their company through branch
     */
    public boolean hasCompanyAccess(Long companyId) {
        User currentUser = DbUtill.getCurrentUser();

        // Super admins have access to all companies
        if (isSuperAdmin()) {
            return true;
        }

        // SAAS admins and SAAS admin managers can access their assigned company
        if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
            return currentUser.getCompany() != null && currentUser.getCompany().getId().equals(companyId);
        }

        // Branch managers and others access through their branch's company
        if (currentUser.getBranch() != null && currentUser.getBranch().getClinic() != null) {
            return currentUser.getBranch().getClinic().getId().equals(companyId);
        }

        return false;
    }

    /**
     * Perform comprehensive access check based on role hierarchy.
     * Super Admin has access to everything.
     * Admin has access to most things except super admin functions.
     * Other roles follow role-based access control.
     */
    public boolean hasAccess(UserRole[] allowedRoles) {
        // Super admin has access to everything
        if (isSuperAdmin()) {
            return true;
        }

        // Check if user has any of the allowed roles
        return hasAnyRole(allowedRoles);
    }
}