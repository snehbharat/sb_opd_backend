package com.sbpl.OPD.service;

/**
 * Service interface for generating bill numbers.
 * 
 * @author HMS Team
 */
public interface BillNumberService {
    
    /**
     * Generate a unique bill number based on company/branch name
     * Format: {First 3 chars of company name}_{First 3 chars of branch name}_{6-digit unique number}
     * 
     * @param companyId Optional company ID
     * @param branchId Optional branch ID
     * @return Generated bill number
     */
    String generateBillNumber(Long companyId, Long branchId);
    
    /**
     * Generate bill number using billing staff's company/branch
     * 
     * @param billingStaffId Billing staff ID to get company/branch information
     * @return Generated bill number
     */
    String generateBillNumberForStaff(Long billingStaffId);
}