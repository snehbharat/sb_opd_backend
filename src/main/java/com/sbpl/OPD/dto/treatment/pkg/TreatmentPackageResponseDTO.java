package com.sbpl.OPD.dto.treatment.pkg;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TreatmentPackageResponseDTO {

    private Long id;

    private String treatmentName;

    private String packageName;

    private Integer sessions;

    private BigDecimal totalPrice;

    private Integer discountPercentage;

    private Boolean recommended;

    private Boolean followUp;

    private String followUpDate;

    private Long branchId;

    private Long clinicId;

    private Boolean active;
}