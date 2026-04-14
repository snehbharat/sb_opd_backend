package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.service.AppointmentNumberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementation of appointment number generation service.
 * 
 * Generates unique appointment numbers in the format:
 * {First 3 chars of company name}_{First 3 chars of branch name}_{6-digit unique number}
 * 
 * @author Rahul Kumar
 */
@Service
public class AppointmentNumberServiceImpl implements AppointmentNumberService {
    
    private static final Logger logger = LoggerFactory.getLogger(AppointmentNumberServiceImpl.class);
    
    @Autowired
    private CompanyProfileRepository companyProfileRepository;
    
    @Autowired
    private BranchRepository branchRepository;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Override
    public String generateAppointmentNumber(Long companyId, Long branchId) {
        logger.info("Generating appointment number for company={}, branch={}", companyId, branchId);
        
        String companyPrefix = getCompanyPrefix(companyId);
        String branchPrefix = getBranchPrefix(branchId);
        
        // Create a temporary prefix to use for uniqueness checking
        String tempPrefix = companyPrefix + "_" + branchPrefix + "_";
        String uniqueNumber = generateUniqueNumber(tempPrefix);
        
        String appointmentNumber = companyPrefix + "_" + branchPrefix + "_" + uniqueNumber;
        logger.info("Generated appointment number: {}", appointmentNumber);
        
        return appointmentNumber;
    }
    
    @Override
    public String generateAppointmentNumberForDoctor(Long doctorId) {
        logger.info("Generating appointment number for doctor={}", doctorId);
        
        Optional<Doctor> doctorOpt = doctorRepository.findById(doctorId);
        if (doctorOpt.isEmpty()) {
            logger.warn("Doctor not found for ID: {}", doctorId);
            // Fallback to default prefix
            return "HMS_DEF_000000";
        }
        
        Doctor doctor = doctorOpt.get();
        Long companyId = doctor.getCompany() != null ? doctor.getCompany().getId() : null;
        Long branchId = doctor.getBranch() != null ? doctor.getBranch().getId() : null;
        
        return generateAppointmentNumber(companyId, branchId);
    }
    
    protected String getCompanyPrefix(Long companyId) {
        if (companyId == null) {
            return "HMS"; // Default company prefix
        }
        
        try {
            Optional<CompanyProfile> companyOpt = companyProfileRepository.findById(companyId);
            if (companyOpt.isPresent()) {
                String companyName = companyOpt.get().getCompanyName();
                return extractThreeLetterPrefix(companyName);
            }
        } catch (Exception e) {
            logger.warn("Error getting company prefix, using default. Error: {}", e.getMessage());
        }
        
        return "HMS"; // Default if company not found
    }
    
    private String getBranchPrefix(Long branchId) {
        if (branchId == null) {
            return "DEF"; // Default branch prefix
        }
        
        try {
            Optional<Branch> branchOpt = branchRepository.findById(branchId);
            if (branchOpt.isPresent()) {
                String branchName = branchOpt.get().getBranchName();
                return extractThreeLetterPrefix(branchName);
            }
        } catch (Exception e) {
            logger.warn("Error getting branch prefix, using default. Error: {}", e.getMessage());
        }
        
        return "DEF"; // Default if branch not found
    }
    
    private String extractThreeLetterPrefix(String name) {
        if (name == null || name.trim().isEmpty()) {
            return "XXX";
        }
        
        // Split the name by spaces and take the first letter of each word
        String[] words = name.trim().split("\\s+");
        StringBuilder prefix = new StringBuilder();
        
        for (String word : words) {
            if (prefix.length() >= 3) {
                break;
            }
            
            // Take the first letter of each word
            if (!word.isEmpty()) {
                char firstChar = Character.toUpperCase(word.charAt(0));
                if (Character.isLetter(firstChar)) {
                    prefix.append(firstChar);
                }
            }
        }
        
        // If we have less than 3 characters, pad with 'X'
        while (prefix.length() < 3) {
            prefix.append('X');
        }
        
        // Ensure we only take the first 3 characters
        return prefix.substring(0, 3);
    }
    
    private String generateUniqueNumber(String prefix) {
        // Generate a 6-digit number with current timestamp to ensure uniqueness
        LocalDateTime now = LocalDateTime.now();
        String timestampPart = now.format(DateTimeFormatter.ofPattern("HHmmss"));
        
        // Check if this number already exists, if so increment
        String uniqueNumber = timestampPart;
        int counter = 0;
        String fullNumber;
        
        do {
            fullNumber = prefix + uniqueNumber;
            if (counter > 0) {
                // If collision, use a different approach
                uniqueNumber = String.format("%06d", (int)(System.currentTimeMillis() % 1000000));
            }
            counter++;
        } while (appointmentRepository.existsByAppointmentNumber(fullNumber) && counter < 100);
        
        return uniqueNumber;
    }
}