package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.TreatmentPackage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TreatmentPackageRepository extends JpaRepository<TreatmentPackage, Long> {

    List<TreatmentPackage> findByActiveTrue();

    List<TreatmentPackage> findByTreatmentIdAndActiveTrue(Long treatmentId);

    @Query("SELECT tp FROM TreatmentPackage tp WHERE tp.active = true AND tp.branchId = :branchId")
    List<TreatmentPackage> findByBranchIdAndActiveTrue(@Param("branchId") Long branchId);

    @Query("SELECT tp FROM TreatmentPackage tp WHERE tp.active = true AND tp.clinicId = :clinicId")
    List<TreatmentPackage> findByCompanyIdAndActiveTrue(@Param("clinicId") Long clinicId);

    @Query("SELECT tp FROM TreatmentPackage tp WHERE tp.active = true AND tp.clinicId = :clinicId AND tp.branchId = :branchId")
    List<TreatmentPackage> findByCompanyIdAndBranchIdAndActiveTrue(
            @Param("clinicId") Long clinicId,
            @Param("branchId") Long branchId
    );

    @Query("""
            SELECT tp FROM TreatmentPackage tp
            WHERE tp.active = true
            AND LOWER(tp.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND (:clinicId IS NULL OR tp.clinicId = :clinicId)
            AND (:branchId IS NULL OR tp.branchId = :branchId)
            """)
    List<TreatmentPackage> searchTreatmentPackages(
            @Param("keyword") String keyword,
            @Param("clinicId") Long clinicId,
            @Param("branchId") Long branchId
    );

    @Query("""
            SELECT tp FROM TreatmentPackage tp
            WHERE tp.active = true
            AND tp.treatment.id = :treatmentId
            AND (:clinicId IS NULL OR tp.clinicId = :clinicId)
            AND (:branchId IS NULL OR tp.branchId = :branchId)
            """)
    List<TreatmentPackage> findByTreatmentIdWithFilters(
            @Param("treatmentId") Long treatmentId,
            @Param("clinicId") Long clinicId,
            @Param("branchId") Long branchId
    );

    @Query("""
            SELECT tp FROM TreatmentPackage tp
            WHERE tp.active = true
            AND tp.recommended = true
            AND (:clinicId IS NULL OR tp.clinicId = :clinicId)
            AND (:branchId IS NULL OR tp.branchId = :branchId)
            """)
    List<TreatmentPackage> findRecommendedPackages(
            @Param("clinicId") Long clinicId,
            @Param("branchId") Long branchId
    );

    @Query("""
            SELECT tp FROM TreatmentPackage tp
            WHERE tp.active = true
            AND tp.treatment.id = :treatmentId
            AND tp.name = :packageName
            AND tp.sessions = :sessions
            AND (:clinicId IS NULL OR tp.clinicId = :clinicId)
            """)
    Optional<TreatmentPackage> findExistingPackage(
            @Param("treatmentId") Long treatmentId,
            @Param("packageName") String packageName,
            @Param("sessions") Integer sessions,
            @Param("clinicId") Long clinicId
    );
}