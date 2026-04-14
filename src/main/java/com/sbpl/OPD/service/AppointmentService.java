package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.AppointmentDTO;
import com.sbpl.OPD.dto.AppointmentWithSlotDTO;
import com.sbpl.OPD.dto.VitalSignsDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import org.springframework.http.ResponseEntity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

/**
 * This is an appointment creation service class .
 *
 * @author Rahul Kumar
 */
public interface AppointmentService {
    ResponseEntity<?> updateAppointment(Long id, AppointmentDTO appointmentDTO);

    ResponseEntity<?> getAppointmentById(Long id);

    ResponseEntity<?> getAllAppointments(Integer pageNo, Integer pageSize, Long branchId,
                                         String startDate, String endDate);

    ResponseEntity<?> getAllAppointmentsStats(Long branchId,
                                         String startDate, String endDate);

    ResponseEntity<?> getAppointmentsByPatientId(
            Long patientId, Integer pageNo, Integer pageSize);

    ResponseEntity<?> getTodayAppointmentsByPatientId(
            Long patientId, Integer pageNo, Integer pageSize);


    ResponseEntity<?> getAppointmentsByDoctorId(
            Long doctorId, Integer pageNo, Integer pageSize);


    ResponseEntity<?> getAppointmentsByStatus(
            AppointmentStatus status, Integer pageNo, Integer pageSize, Long branchId);

    ResponseEntity<?> deleteAppointment(Long id);
    ResponseEntity<?> updateAppointmentStatus(Long id, AppointmentStatus status);
    
    // Employee self-service appointments
    ResponseEntity<?> getMyAppointments(Integer pageNo, Integer pageSize);
    ResponseEntity<?> getMyAppointmentsByStatus(AppointmentStatus status, Integer pageNo, Integer pageSize);
//    ResponseEntity<?> createMyAppointment(AppointmentDTO appointmentDTO);
    ResponseEntity<?> updateMyAppointment(Long id, AppointmentDTO appointmentDTO);
    ResponseEntity<?> cancelMyAppointment(Long id);
    ResponseEntity<?> rescheduleMyAppointment(Long id, AppointmentDTO appointmentDTO);
    
    // Additional appointment actions
    ResponseEntity<?> raiseInvoiceFromAppointment(Long appointmentId);
    ResponseEntity<?> createMyVitalSigns(VitalSignsDTO vitalSignsDTO);
    ResponseEntity<?> getVitalSignsByAppointment(Long appointmentId, Integer pageNo, Integer pageSize);
    
    // Schedule validation methods
    ResponseEntity<?> validateAppointmentTimeSlot(Long doctorId, LocalDate date, LocalTime time);
    ResponseEntity<?> getAvailableTimeSlots(Long doctorId, LocalDate date);
    
    // Appointment with slot creation
    ResponseEntity<?> createAppointmentWithSlot(AppointmentWithSlotDTO appointmentWithSlotDTO);
    
    // Appointment with slot rescheduling
    ResponseEntity<?> rescheduleAppointmentWithSlot(Long appointmentId, AppointmentWithSlotDTO appointmentWithSlotDTO);
    
    // Mark appointment as complete
    ResponseEntity<?> markAppointmentAsComplete(Long appointmentId, String consultationNotes);
    
    // Mark appointment as confirmed
    ResponseEntity<?> markAppointmentAsConfirmed(Long appointmentId, String confirmationNotes);
    
    // Utility method for data population
    void populateCompanyInformationForExistingAppointments();
    
    // Follow-up appointment management
    ResponseEntity<?> scheduleFollowUpAppointment(Long originalAppointmentId, AppointmentDTO followUpDTO);
    ResponseEntity<?> getFollowUpAppointments(Long originalAppointmentId);
    ResponseEntity<?> markAsFollowUpRequired(Long appointmentId, LocalDateTime followUpDate);
    
    // Calendar view methods
    ResponseEntity<?> getAppointmentsForCalendar(Long doctorId, Long companyId, Long branchId);
    ResponseEntity<?> getAppointmentsForCalendarWithDateRange(Long doctorId, Long companyId, Long branchId, LocalDateTime startDate, LocalDateTime endDate);
    
    // No-show appointment management
    ResponseEntity<?> markAppointmentAsNoShow(Long appointmentId, String noShowReason);
    ResponseEntity<?> getNoShowAppointments(Integer pageNo, Integer pageSize);
    ResponseEntity<?> getNoShowAppointmentsByDoctor(Long doctorId, Integer pageNo, Integer pageSize);
    ResponseEntity<?> getNoShowAppointmentsByPatient(Long patientId, Integer pageNo, Integer pageSize);
}