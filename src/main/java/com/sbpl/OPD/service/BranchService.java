package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.branch.BranchRequestDto;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for managing Branch entities in the hierarchical structure.
 * Branches are created under clinics and contain departments.
 *
 * @author HMS Team
 */
public interface BranchService {

    /**
     * Create a new branch under a clinic.
     * Only SUPER_ADMIN and ADMIN users can create branches.
     *
     * @param dto Branch creation request data
     * @return ResponseEntity with success/failure message
     */
    ResponseEntity<?> createBranch(BranchRequestDto dto);

    /**
     * Update an existing branch.
     * Only SUPER_ADMIN and ADMIN users can update branches.
     *
     * @param branchId The ID of the branch to update
     * @param dto      Updated branch data
     * @return ResponseEntity with success/failure message
     */
    ResponseEntity<?> updateBranch(Long branchId, BranchRequestDto dto);

    /**
     * Get a branch by ID.
     * Only authorized users can access branch information.
     *
     * @param branchId The ID of the branch to retrieve
     * @return ResponseEntity with branch data
     */
    ResponseEntity<?> getBranchById(Long branchId);

    /**
     * Get all branches for a specific clinic.
     * SaaS Admin and SaaS Admin Manager can access branches by company.
     *
     * @param clinicId The ID of the clinic
     * @return ResponseEntity with list of branches
     */
    ResponseEntity<?> getBranchesByClinic(Long clinicId);
    
    /**
     * Get branches by company ID with role-based access control.
     * SaaS Admin and SaaS Admin Manager can access all branches in their company.
     *
     * @param companyId The ID of the company
     * @param pageNo Page number (starting from 0)
     * @param pageSize Number of items per page
     * @return ResponseEntity with paginated branch data
     */
    ResponseEntity<?> getBranchesByCompanyId(Long companyId, Integer pageNo, Integer pageSize);
    
    /**
     * Get branch by ID with role-based access control.
     * Higher roles can fetch any branch, others restricted by branch assignment.
     *
     * @param branchId The ID of the branch to retrieve
     * @return ResponseEntity with branch data
     */
    ResponseEntity<?> getBranchByIdWithAccessControl(Long branchId);

    /**
     * Get all branches with pagination and role-based access control.
     * Branch Manager and lower roles: Access only their assigned branch
     * SaaS Admin and SaaS Admin Manager: Access branches by company
     * Higher roles: Access all branches
     *
     * @param pageNo   Page number (starting from 0)
     * @param pageSize Number of items per page
     * @return ResponseEntity with paginated branch data
     */
    ResponseEntity<?> getAllBranches(Integer pageNo, Integer pageSize);

    /**
     * Delete a branch.
     * Only SUPER_ADMIN users can delete branches.
     *
     * @param branchId The ID of the branch to delete
     * @return ResponseEntity with success/failure message
     */
    ResponseEntity<?> deleteBranch(Long branchId);
}