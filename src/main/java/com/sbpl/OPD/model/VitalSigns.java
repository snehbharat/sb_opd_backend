package com.sbpl.OPD.model;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing vital signs recorded during appointments.
 * Stores patient health metrics like temperature, blood pressure, etc.
 *
 * @author Rahul Kumar
 */
@Entity
@Table(name = "vital_signs", schema = "sb_opd")
@Getter
@Setter
public class VitalSigns extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "appointment_id", nullable = false)
    private Appointment appointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private User patient;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recorded_by_id", nullable = false)
    private User recordedBy;

    @Column(name = "temperature")
    private Double temperature; // in Celsius

    @Column(name = "blood_pressure_systolic")
    private Integer bloodPressureSystolic; // mmHg

    @Column(name = "blood_pressure_diastolic")
    private Integer bloodPressureDiastolic; // mmHg

    @Column(name = "heart_rate")
    private Integer heartRate; // bpm

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate; // breaths per minute

    @Column(name = "oxygen_saturation")
    private Double oxygenSaturation; // percentage

    @Column(name = "height")
    private Double height; // in cm

    @Column(name = "weight")
    private Double weight; // in kg

    @Column(name = "bmi")
    private Double bmi; // calculated

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

}