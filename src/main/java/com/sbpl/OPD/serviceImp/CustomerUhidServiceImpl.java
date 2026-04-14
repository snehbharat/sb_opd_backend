package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.service.CustomerUhidService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of customer UHID (Unique Health Identification) generation service.
 * 
 * Generates unique UHID numbers in the format:
 * {First 3 chars of company name}_{First 3 chars of branch name}_{6-digit sequential number}
 * 
 * @author HMS Team
 */
@Service
public class CustomerUhidServiceImpl implements CustomerUhidService {
    
    private static final Logger logger = LoggerFactory.getLogger(CustomerUhidServiceImpl.class);
    
    @Autowired
    private CompanyProfileRepository companyProfileRepository;
    
    @Autowired
    private BranchRepository branchRepository;
    
    @Autowired
    private CustomerRepository customerRepository;
    
    @Override
    public String generateUhid(Long companyId, Long branchId) {
        logger.info("Generating UHID for company={}, branch={}", companyId, branchId);
        
        String companyPrefix = getCompanyPrefix(companyId);
        String branchPrefix = getBranchPrefix(branchId);
        
        // Create a temporary prefix to use for uniqueness checking
        String tempPrefix = companyPrefix + "_" + branchPrefix + "_";
        String uniqueNumber = generateSequentialNumber(tempPrefix);
        
        String uhid = companyPrefix + "_" + branchPrefix + "_" + uniqueNumber;
        logger.info("Generated UHID: {}", uhid);
        
        return uhid;
    }
    
    @Override
    public String generateUhidForCustomer(Long customerId) {
        logger.info("Generating UHID for customer={}", customerId);
        
        // Since customer doesn't exist yet when generating UHID, 
        // we'll use a default approach here, or this would be called after customer creation
        // For now, we'll return a default format
        return "HMS_CUST_" + String.format("%06d", customerId);
    }
    
    private String getCompanyPrefix(Long companyId) {
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
        
        StringBuilder prefix = new StringBuilder();
        String cleanName = name.trim().toUpperCase();
        
        // Extract first 3 alphabetic characters
        for (char c : cleanName.toCharArray()) {
            if (Character.isLetter(c)) {
                prefix.append(c);
                if (prefix.length() >= 3) {
                    break;
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
    
    private String generateSequentialNumber(String prefix) {
        // Find the highest existing number with this prefix and increment
        int maxNumber = 0;
        
        // We need to find the maximum existing number with this prefix
        // This might be inefficient for large datasets, but works for now
        // A better approach would be to maintain a sequence counter per prefix
        
        // Since the repository doesn't have a method to find by prefix, 
        // we'll use a simpler approach - generate based on timestamp and check for uniqueness
        String timestampPart = String.valueOf(System.currentTimeMillis() % 1000000);
        String paddedNumber = String.format("%06d", Long.parseLong(timestampPart));
        
        // Check if this number already exists, if so increment
        String uniqueNumber = paddedNumber;
        int counter = 0;
        String fullUhid;
        
        do {
            fullUhid = prefix + uniqueNumber;
            // Check if UHID already exists in the database
            boolean exists = customerRepository.existsByUhid(fullUhid);
            if (exists) {
                // Increment the number and try again
                counter++;
                long newNumber = Long.parseLong(paddedNumber) + counter;
                uniqueNumber = String.format("%06d", newNumber % 1000000); // Keep it 6 digits
            } else {
                break;
            }
        } while (counter < 100); // Prevent infinite loop
        
        if (counter >= 100) {
            // If we've tried 100 times and still have conflicts, use a random approach
            uniqueNumber = String.format("%06d", (int)(Math.random() * 1000000));
        }
        
        return uniqueNumber;
    }
}