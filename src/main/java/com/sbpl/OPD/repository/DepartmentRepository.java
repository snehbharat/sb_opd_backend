package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.Department;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository interface for Department entity.
 * Provides CRUD operations and custom queries for Department entities.
 *
 * @author HMS Team
 */
@Repository
public interface DepartmentRepository extends JpaRepository<Department, Long> {

    /**
     * Find departments by branch ID
     */
    List<Department> findByBranch_Id(Long branchId);
    
    /**
     * Find departments by branch ID with descending order by createdAt
     */
    List<Department> findByBranch_IdOrderByCreatedAtDesc(@Param("branchId") Long branchId);

    /**
     * Find department by name and branch ID
     */
    Optional<Department> findByDepartmentNameAndBranch_Id(String departmentName, Long branchId);

    /**
     * Check if department exists by name and branch ID
     */
    boolean existsByDepartmentNameAndBranch_Id(String departmentName, Long branchId);

    /**
     * Find all departments with pagination
     */
    @Override
    Page<Department> findAll(Pageable pageable);
    
    /**
     * Find all departments with pagination and descending order by createdAt
     * Using native query to ensure proper sorting at database level
     */
    Page<Department> findAllByOrderByCreatedAtDesc(Pageable pageable);
}