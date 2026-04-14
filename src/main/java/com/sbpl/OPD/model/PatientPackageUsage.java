package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Entity
@Table(name = "patient_package_usage", schema = "sb_opd")
@Getter
@Setter
public class PatientPackageUsage extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Customer patient;

    @ManyToOne
    @JoinColumn(name = "treatment_package_id", nullable = false)
    private TreatmentPackage treatmentPackage;

    @ManyToOne
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    private Integer totalSessions;

    private Integer sessionsUsed;

    private Integer sessionsRemaining;

    private BigDecimal packagePricePaid;

    private Date purchaseDate;

    private Boolean followUp;

    private String followUpDate;

    private Date lastSessionDate;

    private Boolean completed = false;

    private Boolean active = true;

    private Long branchId;

    private Long clinicId;
}