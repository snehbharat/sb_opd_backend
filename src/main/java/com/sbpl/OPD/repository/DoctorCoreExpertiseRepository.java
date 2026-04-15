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
     * Find all core expertise with pagination
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
     * Find by department name (case-insensitive)
     */
    List<DoctorCoreExpertise> findByDepartmentNameIgnoreCase(String departmentName);

    /**
     * Find all distinct department names
     */
    @Query("SELECT DISTINCT d.departmentName FROM DoctorCoreExpertise d WHERE d.departmentName IS NOT NULL")
    List<String> findAllDistinctDepartmentNames();

    /**
     * Count doctors by expertise (checks if expertise is in the doctor's coreExpertiseList)
     */
    @Query("SELECT COUNT(d) FROM Doctor d JOIN d.coreExpertiseList e WHERE e.id = :expertiseId")
    Long countDoctorsByExpertiseId(@Param("expertiseId") Long expertiseId);
}
