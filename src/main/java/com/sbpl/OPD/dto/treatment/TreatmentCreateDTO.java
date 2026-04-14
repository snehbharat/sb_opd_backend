package com.sbpl.OPD.dto.treatment;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class TreatmentCreateDTO {

  // Category (AUTO CREATE if not exists)
  private String categoryName;

  private String name;

  private BigDecimal singleSessionPrice;
  private BigDecimal minimumPrice;

  private Integer minimumSessions;
  private BigDecimal packagePrice;

  private String unitLabel;
  private BigDecimal unitPrice;

  private Boolean active;
}
