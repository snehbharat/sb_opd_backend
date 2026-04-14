package com.sbpl.OPD.controller;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.dto.BulkScheduleDTO;
import com.sbpl.OPD.dto.ScheduleDTO;
import com.sbpl.OPD.dto.WeekTemplateDTO;
import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.ScheduleService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

/**
 * REST controller for schedule management.
 * <p>
 * Provides endpoints to create, update, retrieve,
 * and manage doctor schedules with proper RBAC validation.
 *
 * @author Rahul Kumar
 */
@RestController
@RequestMapping("/api/v1/schedules")
public class ScheduleController {

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private RbacUtil rbacUtil;

    @GetMapping
    public ResponseEntity<?> getAllSchedules(@RequestParam(required = false) Integer pageNo,
                                             @RequestParam(required = false) Integer pageSize,
                                             @RequestParam(required = false) Long branchId) {
        return scheduleService.getAllSchedules(pageNo, pageSize,branchId);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getScheduleById(@PathVariable Long id) {
        return scheduleService.getScheduleById(id);
    }

    @PostMapping("/create")
    public ResponseEntity<?> createSchedule(@Valid @RequestBody ScheduleDTO scheduleDTO) {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to create schedule");
        }
        return scheduleService.createSchedule(scheduleDTO);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateSchedule(@PathVariable Long id, @Valid @RequestBody ScheduleDTO scheduleDTO) {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to update schedule");
        }
        return scheduleService.updateSchedule(id, scheduleDTO);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteSchedule(@PathVariable Long id) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to delete schedule");
        }
        return scheduleService.deleteSchedule(id);
    }

    @GetMapping("/by-doctor")
    public ResponseEntity<?> getSchedulesByDoctorId(
            @RequestParam Long doctorId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        if (!rbacUtil.isStaffOrHigher()) {
            throw new AccessDeniedException("Insufficient role to view schedules by doctor");
        }
        return scheduleService.getSchedulesByDoctorId(doctorId, pageNo, pageSize);
    }

    @GetMapping("/by-status")
    public ResponseEntity<?> getSchedulesByStatus(
            @RequestParam ScheduleStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        if (!rbacUtil.isStaffOrHigher()) {
            throw new AccessDeniedException("Insufficient role to view schedules by status");
        }
        return scheduleService.getSchedulesByStatus(status, pageNo, pageSize,branchId);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateScheduleStatus(@PathVariable Long id, @RequestParam ScheduleStatus status) {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to update schedule status");
        }
        return scheduleService.updateScheduleStatus(id, status);
    }

    /**
     * Get available time slots for a doctor on a specific date
     */
    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableSlots(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date) {
        if (!rbacUtil.hasAnyRole(UserRole.PATIENT,
                UserRole.DOCTOR,
                UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to check available slots");
        }
        return scheduleService.getAvailableSlots(doctorId, date);
    }

    /**
     * Check if a specific time is available for a doctor on a given date
     */
    @GetMapping("/check-availability")
    public ResponseEntity<?> checkAvailability(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date,
            @RequestParam LocalTime time) {
        if (!rbacUtil.hasAnyRole(UserRole.PATIENT,
                UserRole.DOCTOR,
                UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to check availability");
        }
        return scheduleService.checkAvailability(doctorId, date, time);
    }

    /**
     * Bulk create schedules for a doctor using individual ScheduleDTOs
     */
    @PostMapping("/bulk-create")
    public ResponseEntity<?> bulkCreateSchedules(@Valid @RequestBody List<ScheduleDTO> scheduleDTOs) {
        if (scheduleDTOs == null || scheduleDTOs.isEmpty()) {
            throw new IllegalArgumentException("Schedule list cannot be empty");
        }
        
        if (scheduleDTOs.size() > 100) {
            throw new IllegalArgumentException("Cannot create more than 100 schedules at once");
        }

        return scheduleService.bulkCreateSchedules(scheduleDTOs);
    }

    /**
     * Create bulk schedules for a doctor using BulkScheduleDTO
     * Perfect for date range-based scheduling with common settings
     */
    @PostMapping("/create-bulk")
    public ResponseEntity<?> createBulkSchedules(@Valid @RequestBody BulkScheduleDTO bulkScheduleDTO) {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to create bulk schedules");
        }
        
        if (bulkScheduleDTO.getStartDate() == null || bulkScheduleDTO.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        
        // Validate date range
        if (bulkScheduleDTO.getStartDate().isAfter(bulkScheduleDTO.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        // Limit the date range to prevent excessive schedule creation
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            bulkScheduleDTO.getStartDate(), bulkScheduleDTO.getEndDate());
        if (daysBetween > 365) { // Max 1 year
            throw new IllegalArgumentException("Date range cannot exceed 365 days");
        }

        return scheduleService.createBulkSchedules(bulkScheduleDTO);
    }

    /**
     * Get weekly schedule for a doctor on a specific day of week
     */
    @GetMapping("/weekly-schedule")
    public ResponseEntity<?> getWeeklySchedule(
            @RequestParam Long doctorId,
            @RequestParam DayOfWeek dayOfWeek) {
        return scheduleService.getWeeklySchedule(doctorId, dayOfWeek);
    }

    /**
     * Get active schedules for a doctor on a specific date
     */
    @GetMapping("/active-for-date")
    public ResponseEntity<?> getActiveSchedulesForDate(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date) {

        return scheduleService.getActiveSchedulesForDate(doctorId, date);
    }

    /**
     * Check if a specific time slot is available for booking
     */
    @GetMapping("/is-slot-available")
    public ResponseEntity<?> isTimeSlotAvailable(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date,
            @RequestParam LocalTime time) {
        return scheduleService.isTimeSlotAvailable(doctorId, date, time);
    }

    /**
     * Get all time slots with availability status for a doctor on a specific date
     */
    @GetMapping("/slots-with-availability")
    public ResponseEntity<?> getAllSlotsWithAvailability(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date) {
        return scheduleService.getAllSlotsWithAvailability(doctorId, date);
    }

    /**
     * Create week template schedules for a doctor
     * Perfect for "one doctor, entire week, one setup" scenarios
     */
    @PostMapping("/create-week-template")
    public ResponseEntity<?> createWeekTemplate(@Valid @RequestBody WeekTemplateDTO weekTemplateDTO) {
        if (!rbacUtil.hasAnyRole(UserRole.RECEPTIONIST,
                UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN_MANAGER,
                UserRole.SUPER_ADMIN)) {
            throw new AccessDeniedException("Insufficient role to create week template");
        }
        
        if (weekTemplateDTO.getStartDate() == null || weekTemplateDTO.getEndDate() == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }
        
        // Validate date range
        if (weekTemplateDTO.getStartDate().isAfter(weekTemplateDTO.getEndDate())) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        // Limit the date range to prevent excessive schedule creation
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(
            weekTemplateDTO.getStartDate(), weekTemplateDTO.getEndDate());
        if (daysBetween > 365) { // Max 1 year
            throw new IllegalArgumentException("Date range cannot exceed 365 days");
        }

        return scheduleService.createWeekTemplate(weekTemplateDTO);
    }
    
    /**
     * Get schedules by company ID (company-wise)
     */
    @GetMapping("/by-company/{companyId}")
    public ResponseEntity<?> getSchedulesByCompanyId(
            @PathVariable Long companyId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN,
                UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient role to view company schedules");
        }
        return scheduleService.getSchedulesByCompanyId(companyId, pageNo, pageSize);
    }
    
    /**
     * Get schedules by company with optional branch filter
     * If branchId is provided, returns branch-wise schedules; otherwise returns company-wise schedules
     * Perfect for SAAS admins who need flexibility between company and branch views
     */
    @GetMapping("/by-company/{companyId}/with-branch-filter")
    public ResponseEntity<?> getSchedulesByCompanyWithOptionalBranch(
            @PathVariable Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        if (!rbacUtil.hasAnyRole(UserRole.SAAS_ADMIN,
                UserRole.SAAS_ADMIN_MANAGER,
                UserRole.BRANCH_MANAGER,
                UserRole.SUPER_ADMIN,
                UserRole.SUPER_ADMIN_MANAGER)) {
            throw new AccessDeniedException("Insufficient role to view company schedules");
        }
        return scheduleService.getSchedulesByCompanyWithOptionalBranch(companyId, branchId, pageNo, pageSize);
    }
}