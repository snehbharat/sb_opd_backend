package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.Treatment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TreatmentRepository
        extends JpaRepository<Treatment, Long> {

    List<Treatment> findByActiveTrue();

    List<Treatment> findByCategoryIdAndActiveTrue(Long categoryId);

    @Query("""
                SELECT t FROM Treatment t
                WHERE t.active = true
                AND LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    List<Treatment> searchByName(String keyword);

    @Query("SELECT t FROM Treatment t WHERE t.clinicId = :companyId AND t.active = true")
    List<Treatment> findByCompanyIdAndActiveTrue(Long companyId);

    @Query("SELECT t FROM Treatment t WHERE t.clinicId = :companyId AND t.branchId = :branchId AND t.active = true")
    List<Treatment> findByCompanyIdAndBranchIdAndActiveTrue(Long companyId, Long branchId);

    @Query("SELECT t FROM Treatment t WHERE t.branchId = :branchId AND t.active = true")
    List<Treatment> findByBranchIdAndActiveTrue(Long branchId);

    @Query("""
            SELECT t FROM Treatment t
            WHERE t.active = true
            AND LOWER(t.name) LIKE LOWER(CONCAT('%', :keyword, '%'))
            AND (:companyId IS NULL OR t.clinicId = :companyId)
            AND (:branchId IS NULL OR t.branchId = :branchId)
            """)
    List<Treatment> searchTreatments(String keyword, Long companyId, Long branchId);

    @Query("""
            SELECT t FROM Treatment t
            WHERE t.active = true
            AND t.category.id = :categoryId
            AND (:companyId IS NULL OR t.clinicId = :companyId)
            AND (:branchId IS NULL OR t.branchId = :branchId)
            """)
    List<Treatment> findByCategoryWithFilters(
            @Param("categoryId") Long categoryId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId
    );
}
