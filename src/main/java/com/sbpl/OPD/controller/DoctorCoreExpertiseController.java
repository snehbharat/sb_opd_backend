package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.experties.CreateExpertiseRequest;
import com.sbpl.OPD.dto.experties.UpdateExpertiseRequest;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.DoctorCoreExpertiseService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for managing doctor's core expertise.
 * Provides endpoints for CRUD operations on doctor expertise areas.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/doctor-expertise")
@Validated
public class DoctorCoreExpertiseController {

    private final DoctorCoreExpertiseService expertiseService;
    private final BaseResponse baseResponse;

    public DoctorCoreExpertiseController(DoctorCoreExpertiseService expertiseService, BaseResponse baseResponse) {
        this.expertiseService = expertiseService;
        this.baseResponse = baseResponse;
    }

    /**
     * Create a new core expertise for doctors
     * 
     * @param request Request containing expertise details
     * @return Response with created expertise
     */
    @PostMapping
    public ResponseEntity<?> createCoreExpertise(@RequestBody CreateExpertiseRequest request) {
        if (request.getExpertiseName() == null || request.getExpertiseName().trim().isEmpty()) {
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Expertise name is required");
        }
        
        return expertiseService.createCoreExpertise(
                request.getExpertiseName(),
                request.getDescription(),
                request.getCategory()
        );
    }

    /**
     * Update an existing core expertise
     * 
     * @param expertiseId ID of the expertise to update
     * @param request Request containing updated values
     * @return Response with updated expertise
     */
    @PutMapping("/{expertiseId}")
    public ResponseEntity<?> updateCoreExpertise(
            @PathVariable Long expertiseId,
            @RequestBody UpdateExpertiseRequest request) {
        
        return expertiseService.updateCoreExpertise(
                expertiseId,
                request.getExpertiseName(),
                request.getDescription(),
                request.getCategory(),
                request.getIsActive()
        );
    }

    /**
     * Get a specific core expertise by ID
     * 
     * @param expertiseId ID of the expertise
     * @return Response with expertise details
     */
    @GetMapping("/{expertiseId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getCoreExpertiseById(@PathVariable Long expertiseId) {
        return expertiseService.getCoreExpertiseById(expertiseId);
    }

    /**
     * Get all core expertise (paginated)
     * 
     * @param pageNo Page number (default: 0)
     * @param pageSize Page size (default: 10)
     * @param activeOnly Filter by active status (default: true)
     * @return Paginated list of expertise
     */
    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> getAllCoreExpertise(
            @RequestParam(required = false, defaultValue = "0") Integer pageNo,
            @RequestParam(required = false, defaultValue = "10") Integer pageSize,
            @RequestParam(required = false, defaultValue = "true") Boolean activeOnly) {
        
        return expertiseService.getAllCoreExpertise(pageNo, pageSize, activeOnly);
    }

    /**
     * Search core expertise by name
     * 
     * @param name Name to search for
     * @return List of matching expertise
     */
    @GetMapping("/search")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<?> searchCoreExpertiseByName(
            @RequestParam @NotBlank(message = "Search name is required") String name) {
        return expertiseService.searchCoreExpertiseByName(name);
    }

    /**
     * Activate or deactivate a core expertise
     * 
     * @param expertiseId ID of the expertise
     * @param active New active status
     * @return Success response
     */
    @PutMapping("/{expertiseId}/status")
    public ResponseEntity<?> activateOrDeactivateExpertise(
            @PathVariable Long expertiseId,
            @RequestParam Boolean active) {
        
        return expertiseService.activateOrDeactivateExpertise(expertiseId, active);
    }

    /**
     * Delete a core expertise (only if not used by any doctors)
     * 
     * @param expertiseId ID of the expertise to delete
     * @return Success response
     */
    @DeleteMapping("/{expertiseId}")
    public ResponseEntity<?> deleteCoreExpertise(@PathVariable Long expertiseId) {
        return expertiseService.deleteCoreExpertise(expertiseId);
    }

}
