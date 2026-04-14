package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This Is a Prescription Version Table.
 *
 * @author Kousik Manik
 */
@Entity
@Table(
    name = "prescription_versions",
    schema = "sb_opd",
    uniqueConstraints = {
        @UniqueConstraint(
            name = "uk_prescription_version",
            columnNames = {"prescription_id", "version_no"}
        )
    },
    indexes = {
        @Index(name = "idx_pv_prescription", columnList = "prescription_id"),
        @Index(name = "idx_pv_doctor", columnList = "doctor_id"),
        @Index(name = "idx_pv_request_id", columnList = "request_id")
    }
)
@Getter
@Setter
public class PrescriptionVersion extends BaseEntity {

  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(name = "prescription_id", nullable = false)
  private Prescription prescription;

  @Column(name = "doctor_id", nullable = false)
  private Long doctorId;

  @Column(name = "version_no", nullable = false)
  private Integer versionNo;

  @Column(columnDefinition = "TEXT")
  private String diagnosis;

  @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PrescriptionMedicine> medicines = new ArrayList<>();

  @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PrescriptionTest> tests = new ArrayList<>();

  @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PrescriptionAdvice> advice = new ArrayList<>();

  @OneToMany(mappedBy = "version", cascade = CascadeType.ALL, orphanRemoval = true)
  private List<PrescriptionNote> notes = new ArrayList<>();
}

