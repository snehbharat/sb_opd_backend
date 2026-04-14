package com.sbpl.OPD.controller;

import com.sbpl.OPD.service.Msg91SmsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/sms")
public class SmsController {

    @Autowired
    private Msg91SmsService msg91SmsService;

    @PostMapping("/send-test")
    public ResponseEntity<?> sendTestSms(@RequestBody Map<String, String> request) {
        String phoneNumber = request.get("phoneNumber");
        String message = request.get("message");
        
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Phone number is required");
        }
        
        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Message is required");
        }
        
        return msg91SmsService.sendSms(phoneNumber, message);
    }

    @PostMapping("/send-appointment-reminder")
    public ResponseEntity<?> sendAppointmentReminderSms(@RequestBody Map<String, String> request) {
        String patientName = request.get("patientName");
        String phoneNumber = request.get("phoneNumber");
        String doctorName = request.get("doctorName");
        String appointmentDateTime = request.get("appointmentDateTime");
        String appointmentId = request.get("appointmentId");
        String companyName = request.get("companyName");
        
        // Validate required fields
        if (phoneNumber == null || phoneNumber.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Phone number is required");
        }
        
        if (patientName == null || patientName.trim().isEmpty()) {
            return ResponseEntity.badRequest().body("Patient name is required");
        }
        
        return msg91SmsService.sendAppointmentReminderSms(
            patientName,
            phoneNumber,
            doctorName,
            appointmentDateTime,
            appointmentId,
            companyName
        );
    }

    @GetMapping("/status")
    public ResponseEntity<?> getSmsServiceStatus() {
        return ResponseEntity.ok(Map.of(
            "enabled", msg91SmsService.isSmsServiceEnabled(),
            "validPhoneNumber", msg91SmsService.isValidPhoneNumber("9876543210")
        ));
    }
}