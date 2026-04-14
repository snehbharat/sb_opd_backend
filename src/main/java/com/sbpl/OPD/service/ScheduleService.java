package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.BulkScheduleDTO;
import com.sbpl.OPD.dto.ScheduleDTO;
import com.sbpl.OPD.dto.WeekTemplateDTO;
import com.sbpl.OPD.enums.ScheduleStatus;
import org.springframework.http.ResponseEntity;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * Service interface for schedule management operations.
 *
 * Provides methods to manage doctor schedules, check availability,
 * and validate appointment timing against schedule constraints.
 *
 * @author HMS Team
 */
public interface ScheduleService {
    
    ResponseEntity<?> createSchedule(ScheduleDTO scheduleDTO);
    
    ResponseEntity<?> updateSchedule(Long id, ScheduleDTO scheduleDTO);
    
    ResponseEntity<?> getScheduleById(Long id);
    
    ResponseEntity<?> getAllSchedules(Integer pageNo, Integer pageSize,Long branchId);
    
    ResponseEntity<?> getSchedulesByDoctorId(Long doctorId, Integer pageNo, Integer pageSize);
    
    ResponseEntity<?> getSchedulesByStatus(ScheduleStatus status, Integer pageNo, Integer pageSize,Long branchId);
    
    ResponseEntity<?> deleteSchedule(Long id);
    
    ResponseEntity<?> updateScheduleStatus(Long id, ScheduleStatus status);
    
    // Business logic methods
    ResponseEntity<?> getAvailableSlots(Long doctorId, LocalDate date);
    
    ResponseEntity<?> checkAvailability(Long doctorId, LocalDate date, LocalTime time);
    
    ResponseEntity<?> bulkCreateSchedules(List<ScheduleDTO> scheduleDTOs);
    
    ResponseEntity<?> getWeeklySchedule(Long doctorId, DayOfWeek dayOfWeek);
    
    ResponseEntity<?> getActiveSchedulesForDate(Long doctorId, LocalDate date);
    
    // Check if a specific time slot is available for booking
    ResponseEntity<?> isTimeSlotAvailable(Long doctorId, LocalDate date, LocalTime time);
    
    // Get all time slots with availability status for a doctor on a specific date
    ResponseEntity<?> getAllSlotsWithAvailability(Long doctorId, LocalDate date);
    
    // Create week template schedules for a doctor
    ResponseEntity<?> createWeekTemplate(WeekTemplateDTO weekTemplateDTO);
    
    // Create bulk schedules using BulkScheduleDTO
    ResponseEntity<?> createBulkSchedules(BulkScheduleDTO bulkScheduleDTO);
    
    // Methods for fetching schedules by company and branch
    ResponseEntity<?> getSchedulesByCompanyId(Long companyId, Integer pageNo, Integer pageSize);
    
    // Fetch schedules by company with optional branch filter (for SAAS admins)
    ResponseEntity<?> getSchedulesByCompanyWithOptionalBranch(Long companyId, Long branchId, Integer pageNo, Integer pageSize);
    
    ResponseEntity<?> getSchedulesByBranchId(Long branchId, Integer pageNo, Integer pageSize);
    
    ResponseEntity<?> getSchedulesByCompanyIdAndDoctorId(Long companyId, Long doctorId, Integer pageNo, Integer pageSize);
    
    ResponseEntity<?> getSchedulesByBranchIdAndDoctorId(Long branchId, Long doctorId, Integer pageNo, Integer pageSize);
}