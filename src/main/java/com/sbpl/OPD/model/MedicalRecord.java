package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a medical record in the Healthcare Management System (HMS).
 *
 * Implements secure file storage with role-based access control.
 * Supports various record types and automatic creation from completed appointments.
 *
 * @author Rahul Kumar
 */
@Entity
@Table(name = "medical_records",
        schema = "sb_opd",
        indexes = {
                @Index(name = "idx_medrec_patient_type_created", columnList = "patient_id, record_type, created_at"),
                @Index(name = "idx_medrec_doctor_created", columnList = "doctor_id, created_at"),
                @Index(name = "idx_medrec_appointment", columnList = "appointment_id"),
                @Index(name = "idx_medrec_record_type_created", columnList = "record_type, created_at"),
                @Index(name = "idx_medrec_created_at", columnList = "created_at"),
                @Index(name = "idx_medrec_is_active", columnList = "is_active")
        })
@Getter
@Setter
public class MedicalRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Customer patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @Column(name = "record_type", nullable = false)
    private String recordType;

    @Column(name = "file_name")
    private String fileName;

    @Column(name = "file_path")
    private String filePath;

    @Column(name = "mime_type")
    private String mimeType;

    @Column(name = "file_size")
    private Long fileSize;

    @Column(name = "description", length = 1000)
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "uploaded_by")
    private Long uploadedBy;

    @Column(name = "access_level", nullable = false)
    private String accessLevel = "PATIENT_ONLY";

    @Column(name = "tags")
    private String tags;

    @Column(name = "is_confidential")
    private Boolean isConfidential = false;

}