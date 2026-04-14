package com.sbpl.OPD.model.prescription;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * This Ia A Prescription Table.
 *
 * @author Kousik Manik
 */
@Entity
@Table(name = "prescriptions", schema = "sb_opd",
    indexes = {
        @Index(
            name = "idx_pv_clinic_doctor_patient_created_desc",
            columnList = "clinic_id, doctor_id, patient_id, created_at_ms DESC"
        ),

        @Index(
            name = "idx_pv_clinic_doctor_created_desc",
            columnList = "clinic_id, doctor_id, created_at_ms DESC"
        ),
        @Index(name = "idx_pres_patient", columnList = "patient_id"),
        @Index(name = "idx_pres_clinic", columnList = "clinic_id"),
        @Index(name = "idx_pres_doctor", columnList = "doctor_id"),
        @Index(name = "idx_pres_deleted", columnList = "is_deleted")
    }
)
@Getter
@Setter
public class Prescription extends BaseEntity {

  @Column(name = "doctor_id", nullable = false)
  private Long doctorId;

  @Column(name = "clinic_id", nullable = false)
  private Long clinicId;

  @Column(name = "patient_id", nullable = false)
  private Long patientId;

  @Column(name = "is_deleted", nullable = false)
  private boolean deleted = false;

  @OneToMany(mappedBy = "prescription", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
  private List<PrescriptionVersion> versions = new ArrayList<>();

  /**
   * Helper method to maintain bi-directional relationship.
   */
  public void addVersion(PrescriptionVersion version) {
    versions.add(version);
    version.setPrescription(this);
  }
}

