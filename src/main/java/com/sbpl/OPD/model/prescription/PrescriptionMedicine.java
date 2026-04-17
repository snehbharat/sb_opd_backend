package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import com.sbpl.OPD.model.catelog.MedicineCatalog;
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
 * This Is A Prescription medicines Table.
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "prescription_medicines",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_pm_version", columnList = "version_id"),
        @Index(name = "idx_pm_medicine", columnList = "medicine_id")
    }
)
@Getter
@Setter
public class PrescriptionMedicine extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "version_id", nullable = false)
  private PrescriptionVersion version;

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "medicine_id")
  private MedicineCatalog medicine;

  @Column(name = "medicine_name")
  private String medicineName;

  private String dosage; // e.g., 500mg

  @Column(name = "duration_days")
  private Integer durationDays;

  private String timing; // e.g., "1-0-1"

  @Column(columnDefinition = "TEXT")
  private String instructions; // e.g., "After Food"
}

