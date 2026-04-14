package com.sbpl.OPD.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.sbpl.OPD.enums.AppointmentStatus;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * DTO for appointment data specifically formatted for calendar display
 */
@Getter
@Setter
public class AppointmentCalendarDTO {
    private Long id;
    private String title;  // Brief description of the appointment
    private String patientName;
    private String doctorName;
    private String reason;
    private AppointmentStatus status;
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime start;  // Start time of the appointment
    
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime end;    // End time of the appointment (calculated)
    
    private String backgroundColor;  // Color for calendar event based on status
    private String borderColor;      // Border color for calendar event
    private String textColor;        // Text color for calendar event
    private Boolean allDay = false;  // Whether it's an all-day event
    private String appointmentNumber;
    
    // Constructors
    public AppointmentCalendarDTO() {}
    
    public AppointmentCalendarDTO(Long id, String title, String patientName, String doctorName, 
                                String reason, AppointmentStatus status, LocalDateTime start, 
                                LocalDateTime end, String appointmentNumber) {
        this.id = id;
        this.title = title;
        this.patientName = patientName;
        this.doctorName = doctorName;
        this.reason = reason;
        this.status = status;
        this.start = start;
        this.end = end;
        this.appointmentNumber = appointmentNumber;
        
        // Set colors based on appointment status
        setColorScheme();
    }
    
    private void setColorScheme() {
        switch (this.status) {
            case CONFIRMED:
                this.backgroundColor = "#28a745";  // Green
                this.borderColor = "#28a745";
                this.textColor = "#ffffff";
                break;
            case REQUESTED:
                this.backgroundColor = "#ffc107";  // Yellow
                this.borderColor = "#ffc107";
                this.textColor = "#000000";
                break;
            case COMPLETED:
                this.backgroundColor = "#17a2b8";  // Blue
                this.borderColor = "#17a2b8";
                this.textColor = "#ffffff";
                break;
            case CANCELLED:
                this.backgroundColor = "#dc3545";  // Red
                this.borderColor = "#dc3545";
                this.textColor = "#ffffff";
                break;
            case RESCHEDULED:
                this.backgroundColor = "#fd7e14";  // Orange
                this.borderColor = "#fd7e14";
                this.textColor = "#ffffff";
                break;
            default:
                this.backgroundColor = "#6c757d";  // Gray
                this.borderColor = "#6c757d";
                this.textColor = "#ffffff";
        }
    }
}