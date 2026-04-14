package com.sbpl.OPD.service;

import org.springframework.http.ResponseEntity;

public interface PatientDashboardService {
    
    /**
     * Get patient personal dashboard statistics
     * @param patientId Patient ID
     * @return Personal health metrics and appointment summary
     */
    ResponseEntity<?> getPatientDashboardStatistics(Long patientId);
    
    /**
     * Get patient appointment history and upcoming appointments
     * @param patientId Patient ID
     * @return Appointment history and schedule
     */
    ResponseEntity<?> getPatientAppointments(Long patientId);
    
    /**
     * Get patient medical records and health history
     * @param patientId Patient ID
     * @return Medical records and health information
     */
    ResponseEntity<?> getPatientMedicalRecords(Long patientId);
    
    /**
     * Get patient billing and payment history
     * @param patientId Patient ID
     * @return Billing statements and payment records
     */
    ResponseEntity<?> getPatientBillingHistory(Long patientId);
    
    /**
     * Get patient health trends and analytics
     * @param patientId Patient ID
     * @return Health trends and vital signs analysis
     */
    ResponseEntity<?> getPatientHealthTrends(Long patientId);
    
    /**
     * Get patient doctor relationships and preferences
     * @param patientId Patient ID
     * @return Doctor relationships and visit history
     */
    ResponseEntity<?> getPatientDoctorRelationships(Long patientId);
    
    /**
     * Get patient notifications and reminders
     * @param patientId Patient ID
     * @return Appointment reminders and health notifications
     */
    ResponseEntity<?> getPatientNotifications(Long patientId);
    
    /**
     * Get patient health goals and recommendations
     * @param patientId Patient ID
     * @return Personalized health recommendations
     */
    ResponseEntity<?> getPatientHealthRecommendations(Long patientId);
    
    /**
     * Get today's appointment statistics for patient based on creation date
     * @param patientId Patient ID
     * @return Today's appointment statistics (pending, completed, confirmed, total)
     */
    ResponseEntity<?> getTodaysPatientAppointments(Long patientId);
}