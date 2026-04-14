package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public interface TimeSlotService {
    
    /**
     * Get available time slots for a doctor on a specific date
     */
    ResponseEntity<?> getAvailableSlots(Long doctorId, LocalDate date);
    
    /**
     * Check if a specific time slot is available
     */
    ResponseEntity<?> isTimeSlotAvailable(Long doctorId, LocalDate date, LocalTime time);
    
    /**
     * Get all time slots with availability status for a doctor on a specific date
     */
    ResponseEntity<?> getAllSlotsWithAvailability(Long doctorId, LocalDate date);
    
    /**
     * Block time slots for a doctor (leave, surgery, etc.)
     */
    ResponseEntity<?> blockTimeSlots(Long doctorId, LocalDate date, List<LocalTime> times, String reason);
    
    /**
     * Unblock time slots for a doctor
     */
    ResponseEntity<?> unblockTimeSlots(Long doctorId, LocalDate date, List<LocalTime> times);
    
    /**
     * Get blocked time slots for a doctor
     */
    ResponseEntity<?> getBlockedSlots(Long doctorId, LocalDate date);
    
    /**
     * Get working hours for a doctor
     */
    ResponseEntity<?> getWorkingHours(Long doctorId);
    
    /**
     * Set working hours for a doctor
     */
    ResponseEntity<?> setWorkingHours(Long doctorId, LocalTime startTime, LocalTime endTime, List<Integer> workingDays);
}