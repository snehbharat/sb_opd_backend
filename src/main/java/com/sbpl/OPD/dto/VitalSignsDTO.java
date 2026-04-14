package com.sbpl.OPD.dto;

import lombok.Data;

import java.util.Date;

/**
 * Data Transfer Object for vital signs data.
 * Used for API communication and data exchange.
 */
@Data
public class VitalSignsDTO {
    private Long id;
    private Long appointmentId;
    private Long patientId;
    private Long recordedById;
    private Double temperature;
    private Integer bloodPressureSystolic;
    private Integer bloodPressureDiastolic;
    private Integer heartRate;
    private Integer respiratoryRate;
    private Double oxygenSaturation;
    private Double height;
    private Double weight;
    private Double bmi;
    private String notes;
    private Date createdAt;
    private Date updatedAt;
}