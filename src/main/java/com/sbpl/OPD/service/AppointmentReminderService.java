package com.sbpl.OPD.service;

import com.sbpl.OPD.model.Appointment;
import org.springframework.http.ResponseEntity;
import org.springframework.mail.javamail.JavaMailSender;

import java.util.List;

public interface AppointmentReminderService {
    
    /**
     * Find appointments that are scheduled within the specified time window
     * @param minutesBefore Number of minutes before appointment to search for
     * @return List of appointments within the time window
     */
    List<Appointment> findAppointmentsWithinTimeWindow(int minutesBefore);
    
    /**
     * Find appointments with specific status that are scheduled within the specified time window
     * @param minutesBefore Number of minutes before appointment to search for
     * @param status Status of appointments to search for
     * @return List of appointments within the time window with the specified status
     */
    List<Appointment> findAppointmentsWithinTimeWindow(int minutesBefore, String status);
    
    /**
     * Send reminder notifications for appointments within the specified time window
     * @param minutesBefore Number of minutes before appointment to send reminder
     * @return ResponseEntity with operation result
     */
    ResponseEntity<?> sendAppointmentReminders(int minutesBefore);

    /**
     * Send reminders for appointments happening within the next day
     * @return ResponseEntity with operation result
     */
    ResponseEntity<?> sendDailyAppointmentReminders();
    
    /**
     * Send appointment completion SMS
     * @param patientName Patient's full name
     * @param phoneNumber Patient's phone number
     * @param doctorName Doctor's name
     * @param appointmentDateTime Appointment date and time
     * @param appointmentId Appointment identifier
     * @param companyName Healthcare facility name
     * @return ResponseEntity with operation result
     */
    ResponseEntity<?> sendAppointmentCompletionSms(
        String patientName,
        String phoneNumber,
        String doctorName,
        String appointmentDateTime,
        String appointmentId,
        String companyName
    );
    
    /**
     * Send appointment creation/confirmation SMS
     * @param patientName Patient's full name
     * @param phoneNumber Patient's phone number
     * @param doctorName Doctor's name
     * @param appointmentDateTime Appointment date and time
     * @param appointmentId Appointment identifier
     * @param companyName Healthcare facility name
     * @return ResponseEntity with operation result
     */
    ResponseEntity<?> sendAppointmentCreationSms(
        String patientName,
        String phoneNumber,
        String doctorName,
        String appointmentDateTime,
        String appointmentId,
        String companyName
    );
    
    /**
     * Get mail sender bean
     * @return JavaMailSender instance
     */
    JavaMailSender getMailSender();
    
    /**
     * Get from email address
     * @return From email address
     */
    String getFromEmail();
    
    /**
     * Get MSG91 SMS service
     * @return Msg91SmsService instance
     */
    Msg91SmsService getMsg91SmsService();
}