package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "treatment_category", schema = "sb_opd")
@Getter
@Setter
public class TreatmentCategory extends BaseEntity {

  @Column(nullable = false, unique = true)
  private String name;

  @Column(nullable = false)
  private Boolean active = true;
}
