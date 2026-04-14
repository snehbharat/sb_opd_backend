package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.branch.BranchRequestDto;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.BranchService;
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
 * REST controller for managing Branch entities.
 * 
 * Branches are part of the hierarchical structure: Company -> Clinic -> Branch -> Department -> Users
 * Only authorized users can perform operations based on their roles.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/branches")
@RequiredArgsConstructor
@Slf4j
public class BranchController {

    @Autowired
    private BranchService branchService;

    @Autowired
    private RbacUtil rbacUtil;

    /**
     * Create a new branch.
     * Access: SUPER_ADMIN, ADMIN
     */
    @PostMapping
    public ResponseEntity<?> createBranch(@Valid @RequestBody BranchRequestDto dto) {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient permissions to create branch");
        }
        log.info("Branch creation request received [name={}]", dto.getBranchName());
        return branchService.createBranch(dto);
    }

    /**
     * Update an existing branch.
     * Access: SUPER_ADMIN, ADMIN
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateBranch(
            @PathVariable Long id,
            @Valid @RequestBody BranchRequestDto dto) {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN, UserRole.SAAS_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient permissions to update branch");
        }
        log.info("Branch update request received [id={}]", id);
        return branchService.updateBranch(id, dto);
    }

    /**
     * Get branch by ID.
     * Access: SUPER_ADMIN, ADMIN, BRANCH_MANAGER
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getBranchById(@PathVariable Long id) {
    //        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN, UserRole.SAAS_ADMIN_MANAGER, UserRole.BRANCH_MANAGER)) {
    //            throw new AccessDeniedException("Access denied: Insufficient permissions to view branch");
    //        }
        log.info("Fetching branch [id={}]", id);
        return branchService.getBranchById(id);
    }

    /**
     * Get all branches for a specific clinic.
     * Access: SUPER_ADMIN, ADMIN, BRANCH_MANAGER
     */
    @GetMapping("/clinic/{clinicId}")
    public ResponseEntity<?> getBranchesByClinic(@PathVariable Long clinicId) {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN, UserRole.SAAS_ADMIN_MANAGER, UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient permissions to view branches by clinic");
        }
        log.info("Fetching branches for clinic [id={}]", clinicId);
        return branchService.getBranchesByClinic(clinicId);
    }

    /**
     * Get all branches with pagination.
     * Access: SUPER_ADMIN, SAAS_ADMIN, SAAS_ADMIN_MANAGER, BRANCH_MANAGER
     */
    @GetMapping
    public ResponseEntity<?> getAllBranches(
            @RequestParam(defaultValue = "0") Integer pageNo,
            @RequestParam(defaultValue = "10") Integer pageSize) {
        if (!rbacUtil.hasAnyRole(UserRole.SUPER_ADMIN, UserRole.SAAS_ADMIN, UserRole.SAAS_ADMIN_MANAGER, UserRole.BRANCH_MANAGER)) {
            throw new AccessDeniedException("Access denied: Insufficient permissions to view all branches");
        }
        log.info("Fetching all branches [pageNo={}, pageSize={}]", pageNo, pageSize);
        return branchService.getAllBranches(pageNo, pageSize);
    }

    /**
     * Delete a branch.
     * Access: SUPER_ADMIN only
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteBranch(@PathVariable Long id) {
        log.info("Delete request received [id={}]", id);
        return branchService.deleteBranch(id);
    }
}