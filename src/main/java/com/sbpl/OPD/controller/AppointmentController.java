package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.AppointmentDTO;
import com.sbpl.OPD.dto.AppointmentWithSlotDTO;
import com.sbpl.OPD.dto.VitalSignsDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.AppointmentService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.format.annotation.DateTimeFormat;
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

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Map;

/**
 * REST controller for appointment management.
 *
 * Provides endpoints to create, update, retrieve,
 * and manage appointments between patients and doctors.
 *
 * Supports pagination, filtering by status,
 * and validation to prevent duplicate bookings.
 *
 * @author Rahul Kumar
 */


@RestController
@RequestMapping("/api/v1/appointments")
public class AppointmentController {

    @Autowired
    private AppointmentService appointmentService;

    @Autowired
    private RbacUtil rbacUtil;

    @GetMapping
    public ResponseEntity<?> getAllAppointments(@RequestParam(required = false) Integer pageNo,
                                                @RequestParam(required = false) Integer pageSize,
                                                @RequestParam(required = false) Long branchId,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        return appointmentService.getAllAppointments(pageNo,pageSize, branchId, startDate, endDate);
    }

    @GetMapping("/all/stats")
    public ResponseEntity<?> getAllAppointments(@RequestParam(required = false) Long branchId,
                                                @RequestParam(required = false) String startDate,
                                                @RequestParam(required = false) String endDate) {
        return appointmentService.getAllAppointmentsStats(branchId, startDate, endDate);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getAppointmentById(@PathVariable Long id) {
        return appointmentService.getAppointmentById(id);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> updateAppointment(@PathVariable Long id, @RequestBody AppointmentDTO appointmentDTO) {

        return appointmentService.updateAppointment(id, appointmentDTO);
    }


    @GetMapping("/by-patient")
    public ResponseEntity<?> getAppointmentsByPatientId(
            @RequestParam @NotNull Long patientId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getTodayAppointmentsByPatientId(patientId, pageNo, pageSize);
    }

//    @GetMapping("/by-patient/today")
//    public ResponseEntity<?> getTodayAppointmentsByPatientId(
//            @RequestParam @NotNull Long patientId,
//            @RequestParam(required = false) Integer pageNo,
//            @RequestParam(required = false) Integer pageSize) {
//
//        return appointmentService.getTodayAppointmentsByPatientId(patientId, pageNo, pageSize);
//    }

    @GetMapping("/by-doctor")
    public ResponseEntity<?> getAppointmentsByDoctorId(
            @RequestParam @NotNull Long doctorId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getAppointmentsByDoctorId(doctorId, pageNo, pageSize);
    }

    @GetMapping("/by-status")
    public ResponseEntity<?> getAppointmentsByStatus(
            @RequestParam @NotNull AppointmentStatus status,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getAppointmentsByStatus(status, pageNo, pageSize, branchId);
    }

    @PutMapping("/{id}/status")
    public ResponseEntity<?> updateAppointmentStatus(@PathVariable Long id, @RequestParam AppointmentStatus status) {

        return appointmentService.updateAppointmentStatus(id, status);
    }

    @PutMapping("/reschedule/{id}")
    public ResponseEntity<?> rescheduleAppointment(
            @PathVariable Long id,
            @Valid @RequestBody AppointmentWithSlotDTO appointmentDTO) {

        return appointmentService.rescheduleAppointmentWithSlot(id, appointmentDTO);
    }


    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteAppointment(
            @PathVariable Long id) {
        if (!rbacUtil.isStaffOrHigher()) {
            throw new AccessDeniedException("Access denied: Insufficient permissions to delete appointment");
        }
        return appointmentService.deleteAppointment(id);
    }

    /**
     * Get appointments for the current authenticated user
     * Based on user role: patients see their appointments, doctors see their appointments
     */
    @GetMapping("/my/appointments")
    public ResponseEntity<?> getMyAppointments(@RequestParam(required = false) Integer pageNo,
                                             @RequestParam(required = false) Integer pageSize) {
        return appointmentService.getMyAppointments(pageNo, pageSize);
    }
    
    /**
     * Get appointments for current user by status
     */
    @GetMapping("/my/appointments/status")
    public ResponseEntity<?> getMyAppointmentsByStatus(@RequestParam @NotNull AppointmentStatus status,
                                                      @RequestParam(required = false) Integer pageNo,
                                                      @RequestParam(required = false) Integer pageSize) {
        // All authenticated users can view their own appointments by status
        return appointmentService.getMyAppointmentsByStatus(status, pageNo, pageSize);
    }
    
//    /**
//     * Create appointment for current user (patients)
//     */
//    @PostMapping("/my/create")
//    public ResponseEntity<?> createMyAppointment(@Valid @RequestBody AppointmentDTO appointmentDTO) {
//        return appointmentService.createMyAppointment(appointmentDTO);
//    }
//
    /**
     * Update my appointment - only for appointments the user has access to
     */
    @PutMapping("/my/update/{id}")
    public ResponseEntity<?> updateMyAppointment(@PathVariable Long id, @RequestBody AppointmentDTO appointmentDTO) {
        return appointmentService.updateMyAppointment(id, appointmentDTO);
    }
    
    /**
     * Cancel my appointment - only for appointments the user has access to
     */
    @PutMapping("/cancel/{id}")
    public ResponseEntity<?> cancelMyAppointment(@PathVariable Long id) {
        return appointmentService.cancelMyAppointment(id);
    }
    
    /**
     * Reschedule my appointment - only for appointments the user has access to
     */
    @PutMapping("/my/{id}/reschedule")
    public ResponseEntity<?> rescheduleMyAppointment(@PathVariable Long id, @Valid @RequestBody AppointmentDTO appointmentDTO) {
        return appointmentService.rescheduleMyAppointment(id, appointmentDTO);
    }
    
    /**
     * Mark my appointment as confirmed - only for appointments the user has access to
     * Receptionists can mark appointments as confirmed
     * Can be used on appointments with status REQUESTED
     */
    @PutMapping("/mark-confirmed/{id}")
    public ResponseEntity<?> markMyAppointmentAsConfirmed(
            @PathVariable Long id,
            @RequestParam(required = false) String confirmationNotes) {

        return appointmentService.markAppointmentAsConfirmed(id, confirmationNotes);
    }
    
    /**
     * Mark my appointment as complete - only for appointments the user has access to
     * Doctors can mark their own appointments as complete
     * Can be used on appointments with status REQUESTED, CONFIRMED, or RESCHEDULED
     */
    @PutMapping("/mark-complete/{id}")
    public ResponseEntity<?> markMyAppointmentAsComplete(
            @PathVariable Long id,
            @RequestParam(required = false) String consultationNotes) {

        return appointmentService.markAppointmentAsComplete(id, consultationNotes);
    }
    
    /**
     * Raise invoice from appointment - creates a billing record from appointment
     */
    @PostMapping("/my/{appointmentId}/raise-invoice")
    public ResponseEntity<?> raiseInvoiceFromAppointment(@PathVariable Long appointmentId) {

        return appointmentService.raiseInvoiceFromAppointment(appointmentId);
    }
    
    /**
     * Add vital signs to appointment
     */
    @PostMapping("/add-vitals/{appointmentId}")
    public ResponseEntity<?> addVitalSignsToAppointment(@PathVariable Long appointmentId, 
                                                      @Valid @RequestBody VitalSignsDTO vitalSignsDTO) {

        vitalSignsDTO.setAppointmentId(appointmentId);
        return appointmentService.createMyVitalSigns(vitalSignsDTO);
    }
    
    /**
     * Get vital signs for appointment
     */
    @GetMapping("/vitals/{appointmentId}")
    public ResponseEntity<?> getVitalSignsForAppointment(@PathVariable Long appointmentId,
                                                       @RequestParam(required = false) Integer pageNo,
                                                       @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getVitalSignsByAppointment(appointmentId, pageNo, pageSize);
    }
    
    /**
     * Check if a specific time slot is available for booking
     */
    @GetMapping("/check-slot-availability")
    public ResponseEntity<?> checkSlotAvailability(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date,
            @RequestParam LocalTime time) {
        return appointmentService.validateAppointmentTimeSlot(doctorId, date, time);
    }
    
    /**
     * Get available time slots for a specific doctor on a given date
     * Useful for rescheduling appointments
     */
    @GetMapping("/available-slots")
    public ResponseEntity<?> getAvailableTimeSlots(
            @RequestParam Long doctorId,
            @RequestParam LocalDate date) {
        return appointmentService.getAvailableTimeSlots(doctorId, date);
    }
    
    /**
     * Create appointment with slot validation
     * This endpoint validates the time slot against doctor's schedule before creating appointment
     */
    @PostMapping("/create-with-slot")
    public ResponseEntity<?> createAppointmentWithSlot(@Valid @RequestBody AppointmentWithSlotDTO appointmentWithSlotDTO) {

        return appointmentService.createAppointmentWithSlot(appointmentWithSlotDTO);
    }

    
    /**
     * Utility endpoint to populate company information for existing appointments
     * This should be called once by admin to fix existing data
     */
    @PostMapping("/populate-company-info")
    public ResponseEntity<?> populateCompanyInformation() {
        
        try {
             appointmentService.populateCompanyInformationForExistingAppointments();
            return ResponseEntity.ok().body(Map.of("message", "Company information population started"));
        } catch (Exception e) {
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to populate company information: " + e.getMessage()));
        }
    }
    
    /**
     * Schedule a follow-up appointment for an existing appointment
     * Available to doctors and staff
     */
    @PostMapping("/{id}/follow-up")
    public ResponseEntity<?> scheduleFollowUp(@PathVariable Long id, @Valid @RequestBody AppointmentDTO followUpDTO) {

        return appointmentService.scheduleFollowUpAppointment(id, followUpDTO);
    }
    
    /**
     * Get all follow-up appointments for an original appointment
     * Available to doctors and staff
     */
    @GetMapping("/{id}/follow-ups")
    public ResponseEntity<?> getFollowUpAppointments(@PathVariable Long id) {

        return appointmentService.getFollowUpAppointments(id);
    }
    
    /**
     * Mark an appointment as requiring follow-up with a specific date
     * Available to doctors and staff
     */
    @PutMapping("/{id}/mark-follow-up-required")
    public ResponseEntity<?> markAsFollowUpRequired(@PathVariable Long id, @RequestParam LocalDateTime followUpDate) {

        return appointmentService.markAsFollowUpRequired(id, followUpDate);
    }
    
    /**
     * Get appointments for calendar view filtered by doctor, company, and branch
     * Available to staff and higher roles
     */
    @GetMapping("/calendar-view")
    public ResponseEntity<?> getAppointmentsForCalendar(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId) {

        return appointmentService.getAppointmentsForCalendar(doctorId, companyId, branchId);
    }
    
    /**
     * Get appointments for calendar view with date range filtering
     * Available to staff and higher roles
     */
    @GetMapping("/calendar-view/range")
    public ResponseEntity<?> getAppointmentsForCalendarWithDateRange(
            @RequestParam(required = false) Long doctorId,
            @RequestParam(required = false) Long companyId,
            @RequestParam(required = false) Long branchId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {

        return appointmentService.getAppointmentsForCalendarWithDateRange(doctorId, companyId, branchId, startDate, endDate);
    }
    
    /**
     * Mark an appointment as no-show
     * Available to staff and higher roles
     */
    @PutMapping("/{id}/mark-no-show")
    public ResponseEntity<?> markAppointmentAsNoShow(
            @PathVariable Long id,
            @RequestParam(required = false) String noShowReason) {

        return appointmentService.markAppointmentAsNoShow(id, noShowReason);
    }
    
    /**
     * Get all no-show appointments
     * Available to staff and higher roles
     */
    @GetMapping("/no-show")
    public ResponseEntity<?> getNoShowAppointments(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getNoShowAppointments(pageNo, pageSize);
    }
    
    /**
     * Get no-show appointments for a specific doctor
     * Available to staff and higher roles
     */
    @GetMapping("/no-show/doctor/{doctorId}")
    public ResponseEntity<?> getNoShowAppointmentsByDoctor(
            @PathVariable Long doctorId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getNoShowAppointmentsByDoctor(doctorId, pageNo, pageSize);
    }
    
    /**
     * Get no-show appointments for a specific patient
     * Available to staff and higher roles
     */
    @GetMapping("/no-show/patient/{patientId}")
    public ResponseEntity<?> getNoShowAppointmentsByPatient(
            @PathVariable Long patientId,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {

        return appointmentService.getNoShowAppointmentsByPatient(patientId, pageNo, pageSize);
    }
    
    /**
     * Update appointment type and notes
     * The appointment type is automatically determined based on the appointmentType and notes content
     * Available to staff and higher roles
     * 
     * @param id The appointment ID
     * @param appointmentType The new appointmentType
     * @param notes The new notes (optional)
     *
     */
    @PutMapping("/{id}/update-type")
    public ResponseEntity<?> updateAppointmentTypeWithReasonAndNotes(
            @PathVariable Long id,
            @RequestParam String appointmentType,
            @RequestParam(required = false) String notes) {

        return appointmentService.updateAppointmentTypeWithReasonAndNotes(id, appointmentType, notes);
    }
}