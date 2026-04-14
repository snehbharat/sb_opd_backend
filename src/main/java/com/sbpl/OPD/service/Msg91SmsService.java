package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

public interface Msg91SmsService {
    
    /**
     * Send SMS using MSG91 API
     * @param phoneNumber Recipient phone number
     * @param message SMS message content
     * @return ResponseEntity with operation result
     */
    ResponseEntity<?> sendSms(String phoneNumber, String message);
    
    /**
     * Send appointment reminder SMS
     * @param patientName Patient's full name
     * @param phoneNumber Patient's phone number
     * @param doctorName Doctor's name
     * @param appointmentDateTime Appointment date and time
     * @param appointmentId Appointment identifier
     * @param companyName Healthcare facility name
     * @return ResponseEntity with operation result
     */
    ResponseEntity<?> sendAppointmentReminderSms(
        String patientName,
        String phoneNumber,
        String doctorName,
        String appointmentDateTime,
        String appointmentId,
        String companyName
    );
    
    /**
     * Check if SMS service is enabled
     * @return boolean indicating if SMS service is enabled
     */
    boolean isSmsServiceEnabled();
    
    /**
     * Validate phone number format
     * @param phoneNumber Phone number to validate
     * @return boolean indicating if phone number is valid
     */
    boolean isValidPhoneNumber(String phoneNumber);

    public ResponseEntity<?> sendAppointmentRescheduleSms(
        String patientName,
        String phoneNumber,
        String doctorName,
        String appointmentDateTime,
        String appointmentId,
        String companyName,
        boolean rescheduled,
        boolean created,
        boolean cancel,
        boolean complete);

}