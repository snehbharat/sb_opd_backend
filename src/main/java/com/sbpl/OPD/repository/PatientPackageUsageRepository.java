package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.PatientPackageUsage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PatientPackageUsageRepository extends JpaRepository<PatientPackageUsage, Long> {

    List<PatientPackageUsage> findByPatientIdAndActiveTrue(Long patientId);

    @Query("SELECT ppu FROM PatientPackageUsage ppu WHERE ppu.patient.id = :patientId AND ppu.treatment.id = :treatmentId AND ppu.active = true")
    Optional<PatientPackageUsage> findByPatientIdAndTreatmentId(@Param("patientId") Long patientId, @Param("treatmentId") Long treatmentId);

    @Query("SELECT ppu FROM PatientPackageUsage ppu WHERE ppu.patient.id = :patientId AND ppu.completed = false AND ppu.active = true")
    List<PatientPackageUsage> findActiveUsagesByPatientId(@Param("patientId") Long patientId);

    @Query("SELECT ppu FROM PatientPackageUsage ppu WHERE ppu.patient.id = :patientId AND ppu.treatmentPackage.id = :packageId AND ppu.completed = false AND ppu.active = true")
    Optional<PatientPackageUsage> findActiveUsageByPatientAndPackage(
            @Param("patientId") Long patientId,
            @Param("packageId") Long packageId
    );

    @Query("SELECT COUNT(ppu) FROM PatientPackageUsage ppu WHERE ppu.patient.id = :patientId AND ppu.treatment.id = :treatmentId AND ppu.completed = true")
    Long countCompletedPackagesByPatientAndTreatment(
            @Param("patientId") Long patientId,
            @Param("treatmentId") Long treatmentId
    );

    @Query("SELECT ppu FROM PatientPackageUsage ppu WHERE ppu.patient.id = :patientId AND ppu.completed = true AND ppu.active = false")
    Page<PatientPackageUsage> findAllCompletedUsageByPatientAndPackage(
            @Param("patientId") Long patientId,
            Pageable pageable
    );
}