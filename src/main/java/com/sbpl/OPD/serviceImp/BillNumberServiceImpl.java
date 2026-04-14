package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CompanyProfileRepository;
import com.sbpl.OPD.service.BillNumberService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

/**
 * Implementation of bill number generation service.
 * 
 * Generates unique bill numbers in the format:
 * {First 3 chars of company name}_{First 3 chars of branch name}_{6-digit unique number}
 * 
 * @author HMS Team
 */
@Service
public class BillNumberServiceImpl implements BillNumberService {
    
    private static final Logger logger = LoggerFactory.getLogger(BillNumberServiceImpl.class);
    
    @Autowired
    private CompanyProfileRepository companyProfileRepository;
    
    @Autowired
    private BranchRepository branchRepository;
    
    @Autowired
    private UserRepository userRepository;
    
    @Autowired
    private BillRepository billRepository;
    
    @Override
    public String generateBillNumber(Long companyId, Long branchId) {
        logger.info("Generating bill number for company={}, branch={}", companyId, branchId);
        
        String companyPrefix = getCompanyPrefix(companyId);
        String branchPrefix = getBranchPrefix(branchId);
        
        // Create a temporary prefix to use for uniqueness checking
        String tempPrefix = companyPrefix + "_" + branchPrefix + "_";
        String uniqueNumber = generateUniqueNumber(tempPrefix);
        
        String billNumber = companyPrefix + "_" + branchPrefix + "_" + uniqueNumber;
        logger.info("Generated bill number: {}", billNumber);
        
        return billNumber;
    }
    
    @Override
    public String generateBillNumberForStaff(Long billingStaffId) {
        logger.info("Generating bill number for billing staff={}", billingStaffId);
        
        Optional<User> staffOpt = userRepository.findById(billingStaffId);
        if (staffOpt.isEmpty()) {
            logger.warn("Billing staff not found for ID: {}", billingStaffId);
            // Fallback to default prefix
            return "HMS_DEF_000000";
        }
        
        User staff = staffOpt.get();
        Long companyId = staff.getCompany() != null ? staff.getCompany().getId() : null;
        Long branchId = staff.getBranch() != null ? staff.getBranch().getId() : null;
        
        return generateBillNumber(companyId, branchId);
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
        } while (billRepository.existsByBillNumber(fullNumber) && counter < 100);
        
        return uniqueNumber;
    }
}