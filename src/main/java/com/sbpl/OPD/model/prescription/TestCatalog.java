package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is a Test Catalog Table.
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "test_catalog",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_test_active", columnList = "is_active"),
        @Index(name = "idx_test_name", columnList = "name")
    }
)
@Getter
@Setter
public class TestCatalog extends BaseEntity {

  private String name;
  private String category;
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

  @Column(name = "type_id")
  private Long typeId;
}

