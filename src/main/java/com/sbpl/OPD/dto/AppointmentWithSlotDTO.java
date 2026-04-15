package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.AppointmentStatus;
import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.time.LocalTime;

/**
 * DTO for creating appointments with slot validation.
 * This DTO includes date and time slot information for validation
 * against doctor's schedule before creating the appointment.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class AppointmentWithSlotDTO {
    
    @NotNull(message = "Patient id is required")
    @Positive(message = "Patient id must be a positive number")
    private Long patientId;
    
    @NotNull(message = "Doctor id is required")
    @Positive(message = "Doctor id must be a positive number")
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date must be in the present or future")
    private LocalDate appointmentDate;

    @NotNull(message = "Appointment time is required")
//    @FutureOrPresent(message = "Appointment time must be in the present or future")
    private LocalTime appointmentTime;

    @NotBlank(message = "Reason for appointment is required")
    @Size(min = 5, max = 255, message = "Reason must be between 5 and 255 characters")
    private String reason;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @Size(max = 1000, message = "Consultation notes cannot exceed 1000 characters")
    private String consultationNotes;

    private Boolean isEmergency = false;

    private String priority = "NORMAL"; // NORMAL, HIGH, URGENT

    private Boolean followUpRequired = false;

    private Long companyId;

    private String appointmentType;
    
    private Long branchId;
    
    // Additional fields for response
    private String patientName;
    private String doctorName;
    private String appointmentNumber;
}