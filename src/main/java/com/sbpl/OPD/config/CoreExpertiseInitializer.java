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

            // Define core expertise data with proper department categorization
            List<DoctorCoreExpertise> coreExpertiseList = Arrays.asList(
                // ========== GENERAL MEDICINE & PRIMARY CARE ==========
                createExpertise("General Medicine", "General Medicine"),
                createExpertise("Family Medicine", "General Medicine"),
                createExpertise("Internal Medicine", "General Medicine"),
                createExpertise("Preventive Medicine", "General Medicine"),
                
                // ========== CARDIOLOGY & CARDIOVASCULAR ==========
                createExpertise("Cardiology", "Cardiology"),
                createExpertise("Interventional Cardiology", "Cardiology"),
                createExpertise("Electrophysiology", "Cardiology"),
                createExpertise("Cardiac Surgery", "Cardiology"),
                createExpertise("Vascular Surgery", "Cardiology"),
                
                // ========== NEUROLOGY & NEUROSURGERY ==========
                createExpertise("Neurology", "Neurology"),
                createExpertise("Neurosurgery", "Neurology"),
                createExpertise("Interventional Neurology", "Neurology"),
                createExpertise("Stroke Neurology", "Neurology"),
                createExpertise("Epileptology", "Neurology"),
                
                // ========== ORTHOPEDICS & MUSCULOSKELETAL ==========
                createExpertise("Orthopedics", "Orthopedics"),
                createExpertise("Orthopedic Surgery", "Orthopedics"),
                createExpertise("Joint Replacement Surgery", "Orthopedics"),
                createExpertise("Sports Medicine", "Orthopedics"),
                createExpertise("Spine Surgery", "Orthopedics"),
                createExpertise("Hand Surgery", "Orthopedics"),
                createExpertise("Pediatric Orthopedics", "Orthopedics"),
                
                // ========== PEDIATRICS & CHILD HEALTH ==========
                createExpertise("Pediatrics", "Pediatrics"),
                createExpertise("Neonatology", "Pediatrics"),
                createExpertise("Pediatric Cardiology", "Pediatrics"),
                createExpertise("Pediatric Neurology", "Pediatrics"),
                createExpertise("Pediatric Oncology", "Pediatrics"),
                createExpertise("Pediatric Emergency Medicine", "Pediatrics"),
                
                // ========== DERMATOLOGY & SKIN ==========
                createExpertise("Dermatology", "Dermatology"),
                createExpertise("Cosmetic Dermatology", "Dermatology"),
                createExpertise("Pediatric Dermatology", "Dermatology"),
                createExpertise("Dermatologic Surgery", "Dermatology"),
                
                // ========== OPHTHALMOLOGY & EYE CARE ==========
                createExpertise("Ophthalmology", "Ophthalmology"),
                createExpertise("Retinal Surgery", "Ophthalmology"),
                createExpertise("Cornea and Transplant", "Ophthalmology"),
                createExpertise("Glaucoma Specialist", "Ophthalmology"),
                createExpertise("Pediatric Ophthalmology", "Ophthalmology"),
                createExpertise("Refractive Surgery", "Ophthalmology"),
                
                // ========== ENT (OTOLARYNGOLOGY) ==========
                createExpertise("ENT", "ENT"),
                createExpertise("Otology", "ENT"),
                createExpertise("Rhinology", "ENT"),
                createExpertise("Laryngology", "ENT"),
                createExpertise("Head and Neck Surgery", "ENT"),
                
                // ========== GYNECOLOGY & OBSTETRICS ==========
                createExpertise("Gynecology", "Gynecology & Obstetrics"),
                createExpertise("Obstetrics", "Gynecology & Obstetrics"),
                createExpertise("Reproductive Endocrinology", "Gynecology & Obstetrics"),
                createExpertise("Maternal-Fetal Medicine", "Gynecology & Obstetrics"),
                createExpertise("Gynecologic Oncology", "Gynecology & Obstetrics"),
                createExpertise("Urogynecology", "Gynecology & Obstetrics"),
                
                // ========== PSYCHIATRY & MENTAL HEALTH ==========
                createExpertise("Psychiatry", "Psychiatry & Mental Health"),
                createExpertise("Child Psychiatry", "Psychiatry & Mental Health"),
                createExpertise("Addiction Psychiatry", "Psychiatry & Mental Health"),
                createExpertise("Geriatric Psychiatry", "Psychiatry & Mental Health"),
                createExpertise("Clinical Psychology", "Psychiatry & Mental Health"),
                createExpertise("Behavioral Therapy", "Psychiatry & Mental Health"),
                
                // ========== ONCOLOGY & CANCER CARE ==========
                createExpertise("Oncology", "Oncology"),
                createExpertise("Medical Oncology", "Oncology"),
                createExpertise("Radiation Oncology", "Oncology"),
                createExpertise("Surgical Oncology", "Oncology"),
                createExpertise("Hematologic Oncology", "Oncology"),
                createExpertise("Palliative Care", "Oncology"),
                
                // ========== RADIOLOGY & IMAGING ==========
                createExpertise("Radiology", "Radiology"),
                createExpertise("Interventional Radiology", "Radiology"),
                createExpertise("Nuclear Medicine", "Radiology"),
                createExpertise("Breast Imaging", "Radiology"),
                createExpertise("Musculoskeletal Radiology", "Radiology"),
                
                // ========== ANESTHESIOLOGY & PAIN ==========
                createExpertise("Anesthesiology", "Anesthesiology"),
                createExpertise("Pain Medicine", "Anesthesiology"),
                createExpertise("Critical Care Medicine", "Anesthesiology"),
                createExpertise("Regional Anesthesia", "Anesthesiology"),
                
                // ========== EMERGENCY & TRAUMA ==========
                createExpertise("Emergency Medicine", "Emergency & Trauma"),
                createExpertise("Trauma Surgery", "Emergency & Trauma"),
                createExpertise("Toxicology", "Emergency & Trauma"),
                createExpertise("Disaster Medicine", "Emergency & Trauma"),
                
                // ========== PATHOLOGY & LABORATORY ==========
                createExpertise("Pathology", "Pathology"),
                createExpertise("Clinical Pathology", "Pathology"),
                createExpertise("Anatomical Pathology", "Pathology"),
                createExpertise("Hematopathology", "Pathology"),
                createExpertise("Molecular Pathology", "Pathology"),
                
                // ========== UROLOGY ==========
                createExpertise("Urology", "Urology"),
                createExpertise("Pediatric Urology", "Urology"),
                createExpertise("Urologic Oncology", "Urology"),
                createExpertise("Female Urology", "Urology"),
                
                // ========== GASTROENTEROLOGY ==========
                createExpertise("Gastroenterology", "Gastroenterology"),
                createExpertise("Hepatology", "Gastroenterology"),
                createExpertise("GI Surgery", "Gastroenterology"),
                createExpertise("Pediatric Gastroenterology", "Gastroenterology"),
                
                // ========== PULMONOLOGY & RESPIRATORY ==========
                createExpertise("Pulmonology", "Pulmonology"),
                createExpertise("Sleep Medicine", "Pulmonology"),
                createExpertise("Critical Care Pulmonology", "Pulmonology"),
                createExpertise("Interventional Pulmonology", "Pulmonology"),
                
                // ========== ENDOCRINOLOGY & DIABETES ==========
                createExpertise("Endocrinology", "Endocrinology"),
                createExpertise("Diabetology", "Endocrinology"),
                createExpertise("Thyroidology", "Endocrinology"),
                createExpertise("Pediatric Endocrinology", "Endocrinology"),
                
                // ========== NEPHROLOGY ==========
                createExpertise("Nephrology", "Nephrology"),
                createExpertise("Transplant Nephrology", "Nephrology"),
                createExpertise("Interventional Nephrology", "Nephrology"),
                createExpertise("Dialysis Medicine", "Nephrology"),
                
                // ========== RHEUMATOLOGY ==========
                createExpertise("Rheumatology", "Rheumatology"),
                createExpertise("Clinical Immunology", "Rheumatology"),
                createExpertise("Osteoporosis", "Rheumatology"),
                
                // ========== INFECTIOUS DISEASES ==========
                createExpertise("Infectious Diseases", "Infectious Diseases"),
                createExpertise("HIV/AIDS Medicine", "Infectious Diseases"),
                createExpertise("Tropical Medicine", "Infectious Diseases"),
                createExpertise("Infection Control", "Infectious Diseases"),
                
                // ========== GENERAL & LAPAROSCOPIC SURGERY ==========
                createExpertise("General Surgery", "General Surgery"),
                createExpertise("Laparoscopic Surgery", "General Surgery"),
                createExpertise("Bariatric Surgery", "General Surgery"),
                createExpertise("Colorectal Surgery", "General Surgery"),
                createExpertise("Hernia Surgery", "General Surgery"),
                
                // ========== PLASTIC & RECONSTRUCTIVE SURGERY ==========
                createExpertise("Plastic Surgery", "Plastic & Reconstructive Surgery"),
                createExpertise("Cosmetic Surgery", "Plastic & Reconstructive Surgery"),
                createExpertise("Burn Surgery", "Plastic & Reconstructive Surgery"),
                createExpertise("Microsurgery", "Plastic & Reconstructive Surgery"),
                createExpertise("Craniofacial Surgery", "Plastic & Reconstructive Surgery"),
                
                // ========== ORAL & MAXILLOFACIAL ==========
                createExpertise("Oral Surgery", "Oral & Maxillofacial"),
                createExpertise("Orthodontics", "Oral & Maxillofacial"),
                createExpertise("Periodontics", "Oral & Maxillofacial"),
                createExpertise("Prosthodontics", "Oral & Maxillofacial"),
                
                // ========== PHYSIATRY & REHABILITATION ==========
                createExpertise("Physical Medicine", "Physical Medicine & Rehabilitation"),
                createExpertise("Physiotherapy", "Physical Medicine & Rehabilitation"),
                createExpertise("Occupational Therapy", "Physical Medicine & Rehabilitation"),
                createExpertise("Speech Therapy", "Physical Medicine & Rehabilitation"),
                
                // ========== ALLERGY & IMMUNOLOGY ==========
                createExpertise("Allergy & Immunology", "Allergy & Immunology"),
                
                // ========== GERIATRICS ==========
                createExpertise("Geriatrics", "Geriatrics"),
                createExpertise("Geriatric Medicine", "Geriatrics"),
                
                // ========== TRAVEL MEDICINE ==========
                createExpertise("Travel Medicine", "Travel Medicine"),
                createExpertise("Hyperbaric Medicine", "Travel Medicine")
            );

            List<DoctorCoreExpertise> savedEntities = doctorCoreExpertiseRepository.saveAll(coreExpertiseList);
            log.info("Successfully created {} core expertise entries", savedEntities.size());
            
            // Log each created entry
            savedEntities.forEach(expertise -> 
                log.debug("Created: {} [Department: {}]", expertise.getExpertiseName(), expertise.getDepartmentName())
            );

        } catch (Exception e) {
            log.error("Error initializing core expertise data: {}", e.getMessage(), e);
        }
    }

    /**
     * Helper method to create a core expertise entry
     */
    private DoctorCoreExpertise createExpertise(String expertiseName, String departmentName) {
        DoctorCoreExpertise expertise = new DoctorCoreExpertise();
        expertise.setExpertiseName(expertiseName);
        expertise.setDepartmentName(departmentName);
        return expertise;
    }
}
