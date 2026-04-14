package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.CompanyProfile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Branch entity.
 * Provides CRUD operations and custom queries for Branch entities.
 *
 * @author HMS Team
 */
@Repository
public interface BranchRepository extends JpaRepository<Branch, Long> {

    /**
     * Find branches by clinic ID
     */
    List<Branch> findByClinic_Id(Long clinicId);


   Branch findByIdAndClinic_id(Long id,Long clinicId);

    /**
     * Find branch by name and clinic ID
     */
    Optional<Branch> findByBranchNameAndClinic_Id(String branchName, Long clinicId);

    /**
     * Check if branch exists by name and clinic ID
     */
    boolean existsByBranchNameAndClinic_Id(String branchName, Long clinicId);

    /**
     * Find all branches with pagination
     */
    @Override
    Page<Branch> findAll(Pageable pageable);
    
    /**
     * Find all branches with pagination and descending order by createdAt
     * Using JPQL query to ensure proper sorting
     */

    Page<Branch> findAllByOrderByCreatedAtDesc(Pageable pageable);
    
    /**
     * Find branches by clinic (CompanyProfile) with pagination
     */
    Page<Branch> findByClinic(CompanyProfile clinic, Pageable pageable);
    
    /**
     * Find branch by ID and clinic ID with pagination
     */
    Page<Branch> findByIdAndClinic_id(Long id, Long clinicId, Pageable pageable);

    List<Branch> findByClinicId(Long id);

//    List<Branch> findByClinicId(Long id);
}