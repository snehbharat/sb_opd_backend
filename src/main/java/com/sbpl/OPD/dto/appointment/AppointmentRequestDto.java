package com.sbpl.OPD.dto.appointment;

import jakarta.validation.constraints.FutureOrPresent;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * DTO for appointment creation requests.
 *
 * @author HMS Team
 */
@Data
public class AppointmentRequestDto {

    @NotNull(message = "Patient ID is required")
    private Long patientId;

    @NotNull(message = "Doctor ID is required")
    private Long doctorId;

    @NotNull(message = "Appointment date is required")
    @FutureOrPresent(message = "Appointment date cannot be in the past")
    private LocalDateTime appointmentDate;

    @NotBlank(message = "Reason for appointment is required")
    private String reason;

    private String notes;
    private LocalDateTime scheduledTime;
    private Boolean isEmergency = false;
    private String priority = "NORMAL";
}