package com.sbpl.OPD.service;

/**
 * Service interface for generating unique health identification (UHID) numbers.
 * 
 * @author HMS Team
 */
public interface CustomerUhidService {
    
    /**
     * Generate a unique UHID based on company/branch information
     * Format: {First 3 chars of company name}_{First 3 chars of branch name}_{sequential number}
     * 
     * @param companyId Company ID
     * @param branchId Branch ID
     * @return Generated UHID
     */
    String generateUhid(Long companyId, Long branchId);
    
    /**
     * Generate UHID using customer's company/branch
     * 
     * @param customerId Customer ID to get company/branch information
     * @return Generated UHID
     */
    String generateUhidForCustomer(Long customerId);
}