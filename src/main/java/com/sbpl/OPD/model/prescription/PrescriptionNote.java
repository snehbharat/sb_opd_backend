package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
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
 * This Is A Table For Prescription Notes Details.
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "prescription_notes",
    schema = "sb_opd",
    indexes = {
        @Index(name = "idx_pn_version", columnList = "version_id")
    }
)
@Getter
@Setter
public class PrescriptionNote extends BaseEntity {
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "version_id", nullable = false)
  private PrescriptionVersion version;

  @Column(name = "note_type")
  private String noteType;

  @Column(columnDefinition = "TEXT", nullable = false)
  private String content;
}

