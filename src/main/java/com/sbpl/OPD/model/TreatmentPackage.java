package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "treatment_package", schema = "sb_opd")
@Getter
@Setter
public class TreatmentPackage extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "treatment_id", nullable = false)
    private Treatment treatment;

    private String name;

    private Integer sessions;

    private BigDecimal totalPrice;

    private BigDecimal perSessionPrice;

    private Integer discountPercentage;

    private Boolean recommended;

    private Boolean followUp;

    private String followUpDate;

    private Long branchId;

    private Long clinicId;

    private Boolean active = true;
}