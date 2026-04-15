package com.sbpl.OPD.repository;

import com.sbpl.OPD.dto.Doctor.response.DoctorResponseDTOForSearch;
import com.sbpl.OPD.model.Doctor;
import jakarta.validation.constraints.NotBlank;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface DoctorRepository extends JpaRepository<Doctor, Long> {
    Page<Doctor> findByDepartment(String department, Pageable pageable);

    boolean existsByRegistrationNumber(@NotBlank String registrationNumber);

    Optional<Doctor> findByIdAndCompanyId(Long doctorId, Long companyId);

    @Query("SELECT d FROM Doctor d WHERE d.branch.id = :branchId")
    Page<Doctor> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.branch.id = :branchId")
    List<Doctor> findByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT d FROM Doctor d WHERE d.branch.id = :branchId AND d.department = :department")
    Page<Doctor> findByBranchIdAndDepartment(@Param("branchId") Long branchId, @Param("department") String department, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.company.id = :companyId")
    Page<Doctor> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT d FROM Doctor d WHERE d.company.id = :companyId AND d.department = :department")
    Page<Doctor> findByCompanyIdAndDepartment(@Param("companyId") Long companyId, @Param("department") String department, Pageable pageable);

    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.branch.id = :branchId")
    long countByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(d) FROM Doctor d WHERE d.branch.id = :branchId AND d.isActive = :isActive")
    long countByBranchIdAndIsActive(@Param("branchId") Long branchId, @Param("isActive") Boolean isActive);

    @Query("SELECT DISTINCT d.department FROM Doctor d WHERE d.branch.id = :branchId")
    List<String> findDistinctDepartmentsByBranchId(@Param("branchId") Long branchId);

    @Query("SELECT COUNT(DISTINCT d.branch.clinic.id) FROM Doctor d WHERE d.branch.clinic IS NOT NULL")
    long countTotalCompanies();

    @Query("SELECT COUNT(DISTINCT d.branch.id) FROM Doctor d WHERE d.branch IS NOT NULL")
    long countTotalBranches();

    @Query("SELECT COUNT(d) FROM Doctor d")
    long countTotalDoctors();

    @Query("SELECT COUNT(DISTINCT d.branch.clinic.id) FROM Doctor d WHERE d.branch.clinic IS NOT NULL AND d.branch.clinic.isActive = true")
    long countActiveCompanies();

    @Query("SELECT COUNT(DISTINCT d.branch.id) FROM Doctor d WHERE d.branch IS NOT NULL")
    long countActiveBranches();

    @Query("SELECT COUNT(d) FROM Doctor d")
    long countActiveDoctors();

    @Query("SELECT COUNT(DISTINCT d.branch.clinic.id) FROM Doctor d WHERE d.branch.clinic IS NOT NULL AND d.branch.clinic.createdAt > :startDate")
    long countNewCompaniesThisMonth(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT COUNT(DISTINCT d.branch.id) FROM Doctor d WHERE d.branch IS NOT NULL AND d.branch.createdAt > :startDate")
    long countNewBranchesThisMonth(@Param("startDate") LocalDateTime startDate);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.company.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findDoctorsByNameAndCompanyId(String name, Long companyId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.branch.id = :branchId
            """)
    List<DoctorResponseDTOForSearch> findDoctorsByNameAndBranchId(String name, Long branchId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.doctorEmail) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.company.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findByDoctorEmailAndCompanyId(String name, Long companyId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.doctorEmail) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.branch.id = :branchId
            """)
    List<DoctorResponseDTOForSearch> findByDoctorEmailAndBranchId(String name, Long branchId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.phoneNumber) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.company.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findByPhoneNumberAndCompanyId(String name, Long companyId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.phoneNumber) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.branch.id = :branchId
            """)
    List<DoctorResponseDTOForSearch> findByPhoneNumberAndBranchId(String name, Long branchId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.registrationNumber) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.company.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findByRegistrationNumberAndCompanyId(String name, Long companyId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.registrationNumber) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.branch.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findByRegistrationNumberAndBranchId(String name, Long branchId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.specialization) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.company.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findBySpecializationContainingIgnoreCaseAndCompanyId(String name, Long companyId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE LOWER(d.specialization) LIKE LOWER(CONCAT('%', :name, '%'))
            AND d.branch.id = :companyId
            """)
    List<DoctorResponseDTOForSearch> findBySpecializationContainingIgnoreCaseAndBranchId(String name, Long branchId);

    @Query("""
                SELECT d FROM Doctor d
                WHERE LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :value, '%'))
                   OR LOWER(d.doctorEmail) LIKE LOWER(CONCAT('%', :value, '%'))
                   OR LOWER(d.phoneNumber) LIKE LOWER(CONCAT('%', :value, '%'))
                   OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :value, '%'))
                   OR LOWER(d.registrationNumber) LIKE LOWER(CONCAT('%', :value, '%'))
            """)
    List<DoctorResponseDTOForSearch> searchGlobally(@Param("value") String value);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE d.company.id = :companyId
            AND (
                LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.doctorEmail) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.phoneNumber) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.registrationNumber) LIKE LOWER(CONCAT('%', :value, '%'))
            )
            """)
    List<DoctorResponseDTOForSearch> searchGloballyByCompany(String value, Long companyId);

    @Query("""
            SELECT 
                d.id as id,
                d.prefix as prefix,
                d.doctorName as doctorName,
                d.doctorEmail as doctorEmail,
                d.phoneNumber as phoneNumber,
                d.specialization as specialization,
                d.department as department,
                d.onlineConsultationAvailable as onlineConsultationAvailable,
                d.isActive as isActive,
                d.consultationRoom as consultationRoom,
                d.registrationNumber as registrationNumber,
                d.experienceYears as experienceYears,
                d.company.id as companyId,
                d.branch.id as branchId
            FROM Doctor d
            WHERE d.branch.id = :companyId
            AND (
                LOWER(d.doctorName) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.doctorEmail) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.phoneNumber) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.specialization) LIKE LOWER(CONCAT('%', :value, '%'))
                OR LOWER(d.registrationNumber) LIKE LOWER(CONCAT('%', :value, '%'))
            )
            """)
    List<DoctorResponseDTOForSearch> searchGloballyByBranch(String value, Long branchId);

    @Query("""
         SELECT d
         FROM Doctor d
         WHERE 
             d.company.id = :companyId
         AND (:branchId IS NULL OR d.branch.id = :branchId)
         AND d.createdAtMs BETWEEN :startMillis AND :endMillis
         ORDER BY d.createdAt DESC
    """)
    List<Doctor> getLatestDoctorsForDashboard(
            Long companyId,
            Long branchId,
            Long startMillis,
            Long endMillis,
            Pageable pageable
    );

    @Query("""
       SELECT COUNT(d)
       FROM Doctor d
       WHERE d.branch.id = :branchId
       AND d.createdAtMs BETWEEN :startMs AND :endMs
       """)
    long countByBranchIdAndCreatedAtBetween(
            @Param("branchId") Long branchId,
            @Param("startMs") Long startMs,
            @Param("endMs") Long endMs
    );

    Optional<Doctor> findByUserId(Long userId);

}
