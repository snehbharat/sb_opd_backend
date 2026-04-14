package com.sbpl.OPD.dto.appointment;

import com.sbpl.OPD.enums.AppointmentStatus;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for appointment response data.
 *
 * @author HMS Team
 */
@Data
public class AppointmentResponseDto {

    private Long id;
    private String requestId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Patient information
    private Long patientId;
    private String patientName;
    private String patientEmail;
    private String patientPhone;
    
    // Doctor information
    private Long doctorId;
    private String doctorName;
    private String doctorEmail;
    private String doctorSpecialization;
    
    // Appointment details
    private AppointmentStatus status;
    private LocalDateTime appointmentDate;
    private String reason;
    private String notes;
    private LocalDateTime scheduledTime;
    private LocalDateTime completedAt;
    private String consultationNotes;
    private String cancellationReason;
    private LocalDateTime rescheduledFrom;
    private LocalDateTime rescheduledTo;
    private Boolean isEmergency;
    private String priority;
    private Boolean followUpRequired;
    private LocalDateTime followUpDate;
    
    // Workflow information
    private Boolean canBeConfirmed;
    private Boolean canBeCompleted;
    private Boolean canBeCancelled;
    private Boolean canBeRescheduled;
    private Boolean isCompleted;
    private Boolean isActive;
}