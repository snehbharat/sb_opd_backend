package com.sbpl.OPD.service;

import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;

/**
 * Service interface for handling appointment notifications
 */
public interface AppointmentNotificationService {
    
    /**
     * Send notification when appointment is created
     */
    void sendCreationNotification(Appointment appointment);
    
    /**
     * Send notification when appointment is confirmed
     */
    void sendConfirmationNotification(Appointment appointment);
    
    /**
     * Send notification when appointment is rescheduled
     */
    void sendRescheduleNotification(Appointment appointment);
    
    /**
     * Send notification when appointment is cancelled
     */
    void sendCancellationNotification(Appointment appointment);
    
    /**
     * Send notification when appointment is completed
     */
    void sendCompletionNotification(Appointment appointment);
    
    /**
     * Send notification when appointment is marked as no-show
     */
    void sendNoShowNotification(Appointment appointment);
    
    // Email notification methods
    void sendEmailCreationNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendEmailConfirmationNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendEmailRescheduleNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendEmailCancellationNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendEmailCompletionNotification(Appointment appointment, Customer patient, Doctor doctor);
    
    // SMS notification methods
    void sendSmsCreationNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendSmsConfirmationNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendSmsRescheduleNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendSmsCancellationNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendSmsCompletionNotification(Appointment appointment, Customer patient, Doctor doctor);
    
    // Email template builders
    String buildAppointmentCreationEmail(Appointment appointment, Customer patient, Doctor doctor);
    String buildAppointmentConfirmationEmail(Appointment appointment, Customer patient, Doctor doctor);
    String buildAppointmentRescheduleEmail(Appointment appointment, Customer patient, Doctor doctor);
    String buildAppointmentCancellationEmail(Appointment appointment, Customer patient, Doctor doctor);
    String buildAppointmentCompletionEmail(Appointment appointment, Customer patient, Doctor doctor);
    
    // No-show notification methods
    void sendEmailNoShowNotification(Appointment appointment, Customer patient, Doctor doctor);
    void sendSmsNoShowNotification(Appointment appointment, Customer patient, Doctor doctor);
    String buildAppointmentNoShowEmail(Appointment appointment, Customer patient, Doctor doctor);
    String buildAppointmentNoShowSms(Appointment appointment, Customer patient, Doctor doctor);
}