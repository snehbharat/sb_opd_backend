package com.sbpl.OPD.controller;

import com.sbpl.OPD.service.AppointmentReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/appointments/reminders")
public class AppointmentReminderController {

    @Autowired
    private AppointmentReminderService appointmentReminderService;


    @GetMapping("/send-daily")
    public ResponseEntity<?> sendDailyAppointmentReminders() {
        return appointmentReminderService.sendDailyAppointmentReminders();
    }

    @GetMapping("/send-custom/{minutesBefore}")
    public ResponseEntity<?> sendCustomAppointmentReminders(@PathVariable int minutesBefore) {
        return appointmentReminderService.sendAppointmentReminders(minutesBefore);
    }

    @GetMapping("/find-within/{minutesBefore}")
    public ResponseEntity<?> findAppointmentsWithinTimeWindow(@PathVariable int minutesBefore) {
        var appointments = appointmentReminderService.findAppointmentsWithinTimeWindow(minutesBefore);
        return ResponseEntity.ok(appointments);
    }

    @GetMapping("/find-within/{minutesBefore}/status/{status}")
    public ResponseEntity<?> findAppointmentsWithinTimeWindow(@PathVariable int minutesBefore, @PathVariable String status) {
        var appointments = appointmentReminderService.findAppointmentsWithinTimeWindow(minutesBefore, status);
        return ResponseEntity.ok(appointments);
    }
}