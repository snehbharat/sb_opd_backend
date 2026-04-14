package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

/**
 * Service interface for managing doctor core expertise.
 *
 * @author Rahul Kumar
 */
public interface DoctorCoreExpertiseService {

    /**
     * Create a new core expertise
     */
    ResponseEntity<?> createCoreExpertise(String expertiseName, String description, String category);

    /**
     * Update an existing core expertise
     */
    ResponseEntity<?> updateCoreExpertise(Long expertiseId, String expertiseName, String description, String category, Boolean isActive);

    /**
     * Get core expertise by ID
     */
    ResponseEntity<?> getCoreExpertiseById(Long expertiseId);

    /**
     * Get all core expertise (paginated)
     */
    ResponseEntity<?> getAllCoreExpertise(Integer pageNo, Integer pageSize, Boolean activeOnly);

    /**
     * Search core expertise by name
     */
    ResponseEntity<?> searchCoreExpertiseByName(String name);

    /**
     * Activate or deactivate a core expertise
     */
    ResponseEntity<?> activateOrDeactivateExpertise(Long expertiseId, Boolean active);

    /**
     * Delete a core expertise (only if no doctors are using it)
     */
    ResponseEntity<?> deleteCoreExpertise(Long expertiseId);
}
