package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.model.catelog.TestCatalog;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is A Table For Prescription Tests Details.
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "prescription_tests",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_pt_version", columnList = "version_id"),
        @Index(name = "idx_pt_test", columnList = "test_id")
    }
)
@Getter
@Setter
public class PrescriptionTest extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "version_id", nullable = false)
  private PrescriptionVersion version;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "test_id")
  private TestCatalog test;

  // Snapshot name
  @Column(name = "test_name")
  private String testName;

  @Column(columnDefinition = "TEXT")
  private String instructions;
}

