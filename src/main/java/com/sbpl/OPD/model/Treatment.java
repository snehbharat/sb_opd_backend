package com.sbpl.OPD.model;


import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Entity
@Table(name = "treatment", schema = "sb_opd")
@Getter
@Setter
public class Treatment extends BaseEntity {

  @ManyToOne
  @JoinColumn(name = "category_id", nullable = false)
  private TreatmentCategory category;

  @Column(nullable = false)
  private String name;

  // Pricing
  private BigDecimal singleSessionPrice;
  private BigDecimal minimumPrice;

  private Integer minimumSessions;   // e.g. 5, 6, 8
  private BigDecimal packagePrice;

  private String unitLabel; // UNIT / GRAFT (optional)
  private BigDecimal unitPrice;

  private Long branchId;
  private Long clinicId;

  private Boolean active = true;
}
