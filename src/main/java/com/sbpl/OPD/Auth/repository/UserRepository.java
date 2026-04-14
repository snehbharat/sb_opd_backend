package com.sbpl.OPD.Auth.repository;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {


    // Removed permission-related query for pure RBAC
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Boolean existsByUsername(String username);
    Boolean existsByEmail(String email);
    List<User> findByRole(UserRole role);
    
    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company LEFT JOIN FETCH u.branch WHERE u.role = :role")
    List<User> findByRoleWithCompanyAndBranch(@Param("role") UserRole role);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.branch b LEFT JOIN FETCH b.clinic WHERE u.id = :id")
    Optional<User> findByIdWithBranch(@Param("id") Long id);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.company c WHERE u.id = :id")
    Optional<User> findByIdWithCompany(@Param("id") Long id);

    boolean existsByUsernameAndIdNot(String username, Long id);

    boolean existsByEmailAndIdNot(String email, Long id);

    boolean existsByPhoneNumberAndIdNot(String phoneNumber, Long id);

    boolean existsByEmployeeIdAndIdNot(String employeeId, Long id);

    // Staff metrics methods
    @Query("SELECT COUNT(u) FROM User u WHERE u.branch.id = :branchId AND u.role IN :staffRoles")
    long countStaffByBranchIdAndRoles(@Param("branchId") Long branchId, @Param("staffRoles") List<UserRole> staffRoles);

    @Query("SELECT COUNT(u) FROM User u WHERE u.branch.id = :branchId AND u.role IN :staffRoles AND u.isActive = true")
    long countActiveStaffByBranchIdAndRoles(@Param("branchId") Long branchId, @Param("staffRoles") List<UserRole> staffRoles);

    @Query("SELECT COUNT(u) FROM User u WHERE u.branch.id = :branchId")
    long countAllUsersByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(u) FROM User u WHERE u.branch.id = :branchId AND u.isActive = true")
    long countActiveUsersByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT u FROM User u WHERE u.role IN :roles AND u.isActive = true")
    List<User> findAllManagementAndStaff(@Param("roles") List<UserRole> roles);

    @Query("""
                SELECT u
                FROM User u
                WHERE 
                    u.company.id = :companyId
                AND (:branchId IS NULL OR u.branch.id = :branchId)
                AND u.role <> UserRole.DOCTOR
                ORDER BY u.id DESC
            """)
    List<User> getLatestEmployeesForDashboard(
            Long companyId,
            Long branchId,
            Long startMillis,
            Long endMillis,
            Pageable pageable
    );
}