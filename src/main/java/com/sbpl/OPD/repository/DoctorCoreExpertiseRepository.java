package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.DoctorCoreExpertise;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository for managing doctor core expertise entities.
 *
 * @author Rahul Kumar
 */
@Repository
public interface DoctorCoreExpertiseRepository extends JpaRepository<DoctorCoreExpertise, Long> {

    /**
     * Find all active core expertise entries
     */
    List<DoctorCoreExpertise> findByIsActiveTrue();

    /**
     * Find all core expertise with pagination
     */
    Page<DoctorCoreExpertise> findByIsActiveTrue(Pageable pageable);

    /**
     * Find all core expertise with pagination (including inactive)
     */
    Page<DoctorCoreExpertise> findAll(Pageable pageable);

    /**
     * Find by expertise name (case-insensitive)
     */
    Optional<DoctorCoreExpertise> findByExpertiseNameIgnoreCase(String expertiseName);

    /**
     * Check if expertise name exists
     */
    boolean existsByExpertiseNameIgnoreCase(String expertiseName);

    /**
     * Search expertise by name containing (case-insensitive)
     */
    List<DoctorCoreExpertise> findByExpertiseNameContainingIgnoreCase(String name);

    /**
     * Find by category
     */
    List<DoctorCoreExpertise> findByCategoryIgnoreCase(String category);

    /**
     * Count doctors by expertise
     */
    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.coreExpertise.id = :expertiseId")
    Long countDoctorsByExpertiseId(@Param("expertiseId") Long expertiseId);

    /**
     * Get all expertise with doctor counts
     */
    @Query("SELECT e.id, e.expertiseName, COUNT(d) FROM DoctorCoreExpertise e LEFT JOIN Doctor d ON d.coreExpertise.id = e.id GROUP BY e.id, e.expertiseName")
    List<Object[]> getExpertiseWithDoctorCounts();
}
