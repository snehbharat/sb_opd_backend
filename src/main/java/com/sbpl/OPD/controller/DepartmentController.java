package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.department.DepartmentRequestDto;
import com.sbpl.OPD.service.DepartmentService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
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

/**
 * REST controller for managing Department entities.
 * 
 * Departments are part of the hierarchical structure: Company -> Clinic -> Branch -> Department -> Users
 * Only authorized users can perform operations based on their roles.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/departments")
@RequiredArgsConstructor
@Slf4j
public class DepartmentController {

    @Autowired
    private DepartmentService departmentService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Create a new department.
     * Access: SUPER_ADMIN, ADMIN
     */
    @PostMapping
    public ResponseEntity<?> createDepartment(@Valid @RequestBody DepartmentRequestDto dto) {

        log.info("Department creation request received [name={}]", dto.getDepartmentName());
        return departmentService.createDepartment(dto);
    }

    /**
     * Update an existing department.
     * Access: SUPER_ADMIN, ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateDepartment(
            @PathVariable Long id,
            @Valid @RequestBody DepartmentRequestDto dto) {
        log.info("Department update request received [id={}]", id);
        return departmentService.updateDepartment(id, dto);
    }

    /**
     * Get department by ID.
     * Access: SUPER_ADMIN, ADMIN, BRANCH_MANAGER
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getDepartmentById(@PathVariable Long id) {

        log.info("Fetching department [id={}]", id);
        return departmentService.getDepartmentById(id);
    }

    /**
     * Get all departments for a specific branch.
     * Access: SUPER_ADMIN, ADMIN, BRANCH_MANAGER
     */
    @GetMapping("/branch/{branchId}")
    public ResponseEntity<?> getDepartmentsByBranch(@PathVariable Long branchId) {

        log.info("Fetching departments for branch [id={}]", branchId);
        return departmentService.getDepartmentsByBranch(branchId);
    }

    /**
     * Get all departments with pagination.
     * Access: SUPER_ADMIN, ADMIN, BRANCH_MANAGER
     */
    @GetMapping
    public ResponseEntity<?> getAllDepartments(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {

        log.info("Fetching all departments [pageNo={}, pageSize={}]", pageNo, pageSize);
        return departmentService.getAllDepartments(pageNo, pageSize);
    }

    /**
     * Delete a department.
     * Access: SUPER_ADMIN only
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteDepartment(@PathVariable Long id) {
        log.info("Delete request received [id={}]", id);
        return departmentService.deleteDepartment(id);
    }
}