package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.department.DepartmentRequestDto;
import org.springframework.http.ResponseEntity;

/**
 * Service interface for managing Department entities in the hierarchical structure.
 * Departments are created under branches and contain users with specific roles.
 *
 * @author HMS Team
 */
public interface DepartmentService {

    /**
     * Create a new department under a branch.
     * Only SUPER_ADMIN and ADMIN users can create departments.
     *
     * @param dto Department creation request data
     * @return ResponseEntity with success/failure message
     */
    ResponseEntity<?> createDepartment(DepartmentRequestDto dto);

    /**
     * Update an existing department.
     * Only SUPER_ADMIN and ADMIN users can update departments.
     *
     * @param departmentId The ID of the department to update
     * @param dto          Updated department data
     * @return ResponseEntity with success/failure message
     */
    ResponseEntity<?> updateDepartment(Long departmentId, DepartmentRequestDto dto);

    /**
     * Get a department by ID.
     * Only authorized users can access department information.
     *
     * @param departmentId The ID of the department to retrieve
     * @return ResponseEntity with department data
     */
    ResponseEntity<?> getDepartmentById(Long departmentId);

    /**
     * Get all departments for a specific branch.
     * Only SUPER_ADMIN and ADMIN users can access department information.
     *
     * @param branchId The ID of the branch
     * @return ResponseEntity with list of departments
     */
    ResponseEntity<?> getDepartmentsByBranch(Long branchId);

    /**
     * Get all departments with pagination.
     * Only SUPER_ADMIN and ADMIN users can access department information.
     *
     * @param pageNo   Page number (starting from 0)
     * @param pageSize Number of items per page
     * @return ResponseEntity with paginated department data
     */
    ResponseEntity<?> getAllDepartments(Integer pageNo, Integer pageSize);

    /**
     * Delete a department.
     * Only SUPER_ADMIN users can delete departments.
     *
     * @param departmentId The ID of the department to delete
     * @return ResponseEntity with success/failure message
     */
    ResponseEntity<?> deleteDepartment(Long departmentId);
}