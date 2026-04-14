package com.sbpl.OPD.controller;

import com.sbpl.OPD.service.PatientDashboardService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/patient/dashboard")
public class PatientDashboardController {

    @Autowired
    private PatientDashboardService patientDashboardService;

    /**
     * Get patient personal dashboard statistics
     * Access: PATIENT (own data only)
     */
    @GetMapping("/statistics")
    public ResponseEntity<?> getPatientDashboardStatistics(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientDashboardStatistics(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient appointment history and upcoming appointments
     * Access: PATIENT (own data only)
     */
    @GetMapping("/appointments")
    public ResponseEntity<?> getPatientAppointments(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientAppointments(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient medical records and health history
     * Access: PATIENT (own data only)
     */
    @GetMapping("/medical-records")
    public ResponseEntity<?> getPatientMedicalRecords(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientMedicalRecords(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient billing and payment history
     * Access: PATIENT (own data only)
     */
    @GetMapping("/billing")
    public ResponseEntity<?> getPatientBillingHistory(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientBillingHistory(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient health trends and analytics
     * Access: PATIENT (own data only)
     */
    @GetMapping("/health-trends")
    public ResponseEntity<?> getPatientHealthTrends(
            @RequestParam Long patientId) {
        try {
            return patientDashboardService.getPatientHealthTrends(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient doctor relationships and preferences
     * Access: PATIENT (own data only)
     */
    @GetMapping("/doctors")
    public ResponseEntity<?> getPatientDoctorRelationships(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientDoctorRelationships(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient notifications and reminders
     * Access: PATIENT (own data only)
     */
    @GetMapping("/notifications")
    public ResponseEntity<?> getPatientNotifications(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientNotifications(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }

    /**
     * Get patient health goals and recommendations
     * Access: PATIENT (own data only)
     */
    @GetMapping("/recommendations")
    public ResponseEntity<?> getPatientHealthRecommendations(
            @RequestParam Long patientId) {
        try {

            return patientDashboardService.getPatientHealthRecommendations(patientId);

        } catch (Exception e) {
            return ResponseEntity.status(500).body("Error: " + e.getMessage());
        }
    }
}