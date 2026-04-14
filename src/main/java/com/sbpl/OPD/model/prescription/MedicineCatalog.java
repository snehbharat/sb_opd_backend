package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is a medicine catalog Table .
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "medicine_catalog",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_medicine_active", columnList = "is_active"),
        @Index(name = "idx_medicine_name", columnList = "name")
    }
)
@Getter
@Setter
public class MedicineCatalog extends BaseEntity {

  private String name;
  private String form;
  private String strength;
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;
}

