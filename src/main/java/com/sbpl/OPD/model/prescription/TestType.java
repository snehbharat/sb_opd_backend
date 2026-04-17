package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is a Test Type Database.
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "test_type",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_test_type_active", columnList = "is_active"),
        @Index(name = "idx_test_type_name", columnList = "name")
    }
)
@Getter
@Setter
public class TestType extends BaseEntity {

  private String name;
  private String description;
  @Column(name = "is_active", nullable = false)
  private boolean isActive = true;

}
