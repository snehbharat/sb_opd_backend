package com.sbpl.OPD.dto.treatment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TreatmentResponseDTO {

  private Long id;
  private String categoryName;
  private String treatmentName;

  private BigDecimal singleSessionPrice;
  private BigDecimal minimumPrice;

  private Integer minimumSessions;
  private BigDecimal packagePrice;

  private String unitLabel;
  private BigDecimal unitPrice;
}
