package com.sbpl.OPD.service;

/**
 * Service interface for generating appointment numbers.
 * 
 * @author HMS Team
 */
public interface AppointmentNumberService {
    
    /**
     * Generate a unique appointment number based on company/branch name
     * Format: {First 3 chars of company/branch name}{6-digit unique number}
     * 
     * @param companyId Optional company ID
     * @param branchId Optional branch ID
     * @return Generated appointment number
     */
    String generateAppointmentNumber(Long companyId, Long branchId);
    
    /**
     * Generate appointment number using doctor's company/branch
     * 
     * @param doctorId Doctor ID to get company/branch information
     * @return Generated appointment number
     */
    String generateAppointmentNumberForDoctor(Long doctorId);
}