package com.sbpl.OPD.dto.treatment.pkg;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TreatmentPackageCreateDTO {

    private Long treatmentId;

    private String name;

    private Integer sessions;

    private BigDecimal totalPrice;

    private Boolean followUp;

    private String followUpDate;

    private BigDecimal perSessionPrice;

    private Integer discountPercentage;

    private Boolean recommended;

    private Boolean active;
}