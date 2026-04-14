package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.AppointmentStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
public class AppointmentDTO {
    private Long id;
    @NotNull(message = "Patient id is required")
    @Positive(message = "Patient id must be a positive number")
    private Long patientId;
    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be a positive number")
    private Long doctorId;

    private AppointmentStatus status;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be in the present or future")
    private LocalDateTime appointmentDate;

    @NotBlank(message = "Reason for appointment is required")
    @Size(min = 5, max = 255, message = "Reason must be between 5 and 255 characters")
    private String reason;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    @NotNull(message = "Scheduled time is required")
    @FutureOrPresent(message = "Scheduled time must be in the present or future")
    private LocalDateTime scheduledTime;

    private LocalDateTime completedAt;

    @Size(max = 1000, message = "Consultation notes cannot exceed 1000 characters")
    private String consultationNotes;

    private String patientName;

    private String doctorName;

    private String appointmentNumber;

    private Long companyId;
    private Long branchId;
    private String branchName;
    
    // Additional fields that were stored during creation
    private String cancellationReason;
    private LocalDateTime rescheduledFrom;
    private LocalDateTime rescheduledTo;
    private Boolean isEmergency;
    private String priority;
    private Boolean followUpRequired;
    private LocalDateTime followUpDate;
    private Boolean isActive;
    
    // Fields for tracking appointment workflow
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // No-show tracking fields
    private String noShowReason;
    private LocalDateTime noShowRecordedAt;
    private Long noShowRecordedBy;
    private String noShowRecordedByName;


}