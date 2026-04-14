package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Represents an appointment in the Healthcare Management System (HMS).
 * <p>
 * This entity implements the complete appointment workflow:
 * REQUESTED → CONFIRMED → COMPLETED/CANCELLED/RESCHEDULED
 * <p>
 * Supports role-based access:
 * - Patient: Can request appointments
 * - Receptionist/Admin: Can confirm/reschedule/cancel
 * - Doctor: Can complete appointments
 * <p>
 * Triggers medical record creation and billing access upon completion.
 *
 * @author HMS Team
 */
@Entity
@Table(name = "appointments",
        schema = "sb_opd",
        indexes = {
                @Index(name = "idx_appt_doctor_status_date", columnList = "doctor_id, status, appointment_date"),
                @Index(name = "idx_appt_patient_status_date", columnList = "patient_id, status, appointment_date"),
                @Index(name = "idx_appt_doctor_appointment_date", columnList = "doctor_id, appointment_date"),
                @Index(name = "idx_appt_status_appointment_date", columnList = "status, appointment_date"),
                @Index(name = "idx_appt_created_at", columnList = "created_at"),
                @Index(name = "idx_appt_branch_status_created_ms",
                        columnList = "branch_id, status, created_at_ms"),
                @Index(name = "idx_appt_created_ms_status", columnList = "created_at_ms, status"),
                @Index(name = "idx_appt_patient_doctor", columnList = "patient_id, doctor_id")
        })
@Getter
@Setter
public class Appointment extends BaseEntity {

    @NotNull(message = "Patient is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Customer patient;

    @NotNull(message = "Doctor is mandatory")
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private CompanyProfile company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @NotNull(message = "Appointment status is required")
    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private AppointmentStatus status = AppointmentStatus.REQUESTED;

    @NotNull(message = "Appointment date is required")
//    @FutureOrPresent(message = "Appointment date cannot be in the past")
    @Column(name = "appointment_date", nullable = false)
    private LocalDateTime appointmentDate;

    @NotBlank(message = "Reason for appointment is required")
    @Size(min = 5, max = 255, message = "Reason must be between 5 and 255 characters")
    @Column(nullable = false)
    private String reason;

    @Size(max = 500, message = "Notes cannot exceed 500 characters")
    private String notes;

    //    @FutureOrPresent(message = "Scheduled time cannot be in the past")
    @Column(name = "scheduled_time")
    private LocalDateTime scheduledTime;

    @PastOrPresent(message = "Completed time cannot be in the future")
    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Size(max = 1000, message = "Consultation notes cannot exceed 1000 characters")
    @Column(name = "consultation_notes")
    private String consultationNotes;

    @Column(name = "cancellation_reason", length = 500)
    private String cancellationReason;

    @Column(name = "rescheduled_from")
    private LocalDateTime rescheduledFrom;

    @Column(name = "rescheduled_to")
    private LocalDateTime rescheduledTo;

    @Column(name = "is_emergency")
    private Boolean isEmergency = false;

    @Column(name = "priority")
    private String priority = "NORMAL"; // NORMAL, HIGH, URGENT

    @Column(name = "follow_up_required")
    private Boolean followUpRequired = false;

    @Column(name = "follow_up_date")
    private LocalDateTime followUpDate;

    @Column(name = "appointment_number", unique = true)
    private String appointmentNumber;

    @Column(name = "no_show_reason", length = 500)
    private String noShowReason;

    @Column(name = "no_show_recorded_at")
    private LocalDateTime noShowRecordedAt;

    @Column(name = "no_show_recorded_by")
    private Long noShowRecordedBy;

    public boolean isActive() {
        return this.status == AppointmentStatus.REQUESTED || this.status == AppointmentStatus.CONFIRMED;
    }

    public boolean isNoShow() {
        return this.status == AppointmentStatus.NO_SHOW;
    }
}