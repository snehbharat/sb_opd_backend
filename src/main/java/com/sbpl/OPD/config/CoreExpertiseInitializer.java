package com.sbpl.OPD.config;

import com.sbpl.OPD.model.DoctorCoreExpertise;
import com.sbpl.OPD.repository.DoctorCoreExpertiseRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

/**
 * Core Expertise Data Initializer
 * Author: Rahul Kumar
 * Date: 2026-04-14
 * 
 * This component initializes the doctor_core_expertise lookup table
 * with default medical expertise areas when the application starts.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CoreExpertiseInitializer implements CommandLineRunner {

    private final DoctorCoreExpertiseRepository doctorCoreExpertiseRepository;

    @Override
    public void run(String... args) {
        log.info("Initializing Doctor Core Expertise data...");
        initializeCoreExpertise();
        log.info("Doctor Core Expertise initialization completed");
    }

    /**
     * Initialize default core expertise entries
     */
    private void initializeCoreExpertise() {
        try {
            // Check if data already exists
            long existingCount = doctorCoreExpertiseRepository.count();
            if (existingCount > 0) {
                log.info("Core expertise data already exists ({} records), skipping initialization", existingCount);
                return;
            }

            log.info("No core expertise data found. Creating default entries...");

            // Define default core expertise data
            List<DoctorCoreExpertise> coreExpertiseList = Arrays.asList(
                createExpertise("General Medicine", "Primary care and general medical practice", "Medical"),
                createExpertise("Cardiology", "Heart and cardiovascular system diseases", "Surgical"),
                createExpertise("Neurology", "Brain and nervous system disorders", "Medical"),
                createExpertise("Orthopedics", "Musculoskeletal system and bone disorders", "Surgical"),
                createExpertise("Pediatrics", "Child health and development", "Medical"),
                createExpertise("Dermatology", "Skin, hair, and nail conditions", "Medical"),
                createExpertise("Ophthalmology", "Eye care and vision disorders", "Surgical"),
                createExpertise("ENT", "Ear, Nose, and Throat specialist", "Surgical"),
                createExpertise("Gynecology", "Women's health and reproductive system", "Surgical"),
                createExpertise("Psychiatry", "Mental health and behavioral disorders", "Medical"),
                createExpertise("Oncology", "Cancer diagnosis and treatment", "Medical"),
                createExpertise("Radiology", "Medical imaging and diagnostic radiology", "Diagnostic"),
                createExpertise("Anesthesiology", "Anesthesia and pain management", "Surgical"),
                createExpertise("Emergency Medicine", "Emergency and trauma care", "Medical"),
                createExpertise("Pathology", "Laboratory medicine and disease diagnosis", "Diagnostic")
            );

            // Save all entries
            List<DoctorCoreExpertise> savedEntities = doctorCoreExpertiseRepository.saveAll(coreExpertiseList);
            log.info("Successfully created {} core expertise entries", savedEntities.size());
            
            // Log each created entry
            savedEntities.forEach(expertise -> 
                log.debug("Created: {} [Category: {}]", expertise.getExpertiseName(), expertise.getCategory())
            );

        } catch (Exception e) {
            log.error("Error initializing core expertise data: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to create a core expertise entry
     */
    private DoctorCoreExpertise createExpertise(String name, String description, String category) {
        DoctorCoreExpertise expertise = new DoctorCoreExpertise();
        expertise.setExpertiseName(name);
        expertise.setDescription(description);
        expertise.setCategory(category);
        expertise.setIsActive(true);
        return expertise;
    }
}
