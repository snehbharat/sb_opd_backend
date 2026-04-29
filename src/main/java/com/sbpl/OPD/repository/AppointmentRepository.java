package com.sbpl.OPD.repository;


import com.sbpl.OPD.dto.repository.AppointmentStatsProjection;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Repository interface for managing appointment data.
 * <p>
 * This repository provides database access operations for appointment entities,
 * including pagination-based retrieval and existence checks.
 * <p>
 * It supports patient-wise, doctor-wise, and status-based appointment queries
 * commonly used in scheduling and reporting workflows.
 *
 * @author Rahul kumar
 */

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {

    Page<Appointment> findByPatientId(Long patientId, Pageable pageable);

    Page<Appointment> findAll(Pageable pageable);

    boolean existsByDoctorIdAndPatientIdAndAppointmentDate(Long doctorId, Long patientId, LocalDateTime appointmentDate);

    boolean existsByAppointmentNumber(String appointmentNumber);

    List<Appointment> findByStatusAndAppointmentDateBetween(AppointmentStatus status, LocalDateTime startDate, LocalDateTime endDate);

    @Query("SELECT a FROM Appointment a LEFT JOIN FETCH a.patient LEFT JOIN FETCH a.doctor WHERE a.id = :id")
    Optional<Appointment> findByIdWithPatientAndDoctor(@Param("id") Long id);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE a.id = :id")
    Optional<Appointment> findByIdWithAllRelationships(@Param("id") Long id);

    @Query("SELECT a FROM Appointment a " +
            "WHERE a.patient.id = :patientId " +
            "AND a.appointmentDate BETWEEN :start AND :end " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByPatientIdAndTodayWithAllRelationships(
            @Param("patientId") Long patientId,
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE p.id = :patientId " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByPatientIdWithAllRelationships(@Param("patientId") Long patientId, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE d.id = :doctorId " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByDoctorIdWithAllRelationships(@Param("doctorId") Long doctorId, Pageable pageable);

    /**
     * Find appointments by doctor ID and status for a specific date range.
     * Returns appointments sorted by status priority (CONFIRMED > REQUESTED > Others),
     * then by appointmentDate ASC within each status.
     */
    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE d.id = :doctorId " +
            "AND a.status IN :statuses " +
            "AND a.appointmentDate >= :fromDate " +
            "AND a.appointmentDate < :toDate " +
            "ORDER BY " +
            "CASE a.status " +
            "  WHEN 'CONFIRMED' THEN 1 " +
            "  WHEN 'REQUESTED' THEN 2 " +
            "  ELSE 3 END ASC, " +
            "a.appointmentDate ASC")
    Page<Appointment> findByDoctorIdAndStatusesWithDateFilter(
            @Param("doctorId") Long doctorId,
            @Param("statuses") List<AppointmentStatus> statuses,
            @Param("fromDate") LocalDateTime fromDate,
            @Param("toDate") LocalDateTime toDate,
            Pageable pageable);

    /**
     * Find appointments by doctor ID and status without date filter.
     * For REQUESTED status: Today's appointments first, then future.
     * For other statuses: Sorted by createdAt DESC.
     */
    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE d.id = :doctorId " +
            "AND a.status = :status " +
            "ORDER BY " +
            "CASE WHEN :status = 'REQUESTED' " +
            "     AND a.appointmentDate >= :todayStart " +
            "     AND a.appointmentDate < :tomorrowStart " +
            "THEN 0 ELSE 1 END ASC, " +
            "a.appointmentDate DESC")
    Page<Appointment> findByDoctorIdAndStatusWithoutDateFilter(
            @Param("doctorId") Long doctorId,
            @Param("status") AppointmentStatus status,
            @Param("todayStart") LocalDateTime todayStart,
            @Param("tomorrowStart") LocalDateTime tomorrowStart,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findAllWithAllRelationships(Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE a.company.id = :companyId " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByCompanyIdWithAllRelationships(@Param("companyId") Long companyId, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByStatusWithAllRelationships(@Param("status") AppointmentStatus status, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE a.company.id = :companyId AND a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByCompanyIdAndStatusWithAllRelationships(@Param("companyId") Long companyId,
                                                                   @Param("status") AppointmentStatus status,
                                                                   Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE p.id = :patientId AND a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByPatientIdAndStatusWithAllRelationships(@Param("patientId") Long patientId,
                                                                   @Param("status") AppointmentStatus status,
                                                                   Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE d.id = :doctorId AND a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByDoctorIdAndStatusWithAllRelationships(@Param("doctorId") Long doctorId,
                                                                  @Param("status") AppointmentStatus status,
                                                                  Pageable pageable);

    // Custom queries for branch manager access
    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE a.branch.id = :branchId " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByBranchIdWithAllRelationships(@Param("branchId") Long branchId, Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.branch.id = :branchId
            AND a.createdAtMs BETWEEN :startMs AND :endMs
            ORDER BY a.createdAtMs DESC
            """)
    Page<Appointment> findByBranchIdAndCreatedAtMsBetween(
            Long branchId,
            Long startMs,
            Long endMs,
            Pageable pageable
    );

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.branch.id = :branchId
            AND a.appointmentDate BETWEEN :start AND :end
            ORDER BY 
            CASE a.status
                WHEN 'CONFIRMED' THEN 1
                WHEN 'REQUESTED' THEN 2
                WHEN 'RESCHEDULED' THEN 3
                WHEN 'COMPLETED' THEN 4
                WHEN 'CANCELLED' THEN 5
                WHEN 'NO_SHOW' THEN 6
            END
            """)
    Page<Appointment> findByBranchIdOrderByStatusPriority(Long branchId,
                                                          @Param("start") LocalDateTime start,
                                                          @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            WHERE a.company.id = :companyId
            AND a.appointmentDate BETWEEN :start AND :end
            ORDER BY 
            CASE a.status
                WHEN 'CONFIRMED' THEN 1
                WHEN 'REQUESTED' THEN 2
                WHEN 'RESCHEDULED' THEN 3
                WHEN 'COMPLETED' THEN 4
                WHEN 'CANCELLED' THEN 5
                WHEN 'NO_SHOW' THEN 6
            END
            """)
    Page<Appointment> findByCompanyIdOrderByStatusPriority(Long companyId,
                                                           @Param("start") LocalDateTime start,
                                                           @Param("end") LocalDateTime end, Pageable pageable);

    @Query("""
            SELECT a FROM Appointment a
            where a.appointmentDate BETWEEN :start AND :end
            ORDER BY 
            CASE a.status
                WHEN 'CONFIRMED' THEN 1
                WHEN 'REQUESTED' THEN 2
                WHEN 'RESCHEDULED' THEN 3
                WHEN 'COMPLETED' THEN 4
                WHEN 'CANCELLED' THEN 5
                WHEN 'NO_SHOW' THEN 6
            END
            """)
    Page<Appointment> findOrderByStatusPriority(
            @Param("start") LocalDateTime start,
            @Param("end") LocalDateTime end, Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE (d.branch.id = :branchId OR p.branch.id = :branchId) AND a.status = :status " +
            "ORDER BY a.createdAt DESC")
    Page<Appointment> findByBranchIdAndStatusWithAllRelationships(@Param("branchId") Long branchId,
                                                                  @Param("status") AppointmentStatus status, Pageable pageable);

    List<Appointment> findByDoctorIdAndScheduledTime(Long doctorId, LocalDateTime scheduledTime);

    @Query("SELECT a FROM Appointment a WHERE a.notes LIKE %:notesPattern% AND a.reason LIKE %:reasonPattern%")
    List<Appointment> findByNotesContainingAndReasonContaining(@Param("notesPattern") String notesPattern,
                                                               @Param("reasonPattern") String reasonPattern);


    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByBranchIdAndAppointmentDateBetween(@Param("branchId") Long branchId,
                                                  @Param("startDate") LocalDateTime startDate,
                                                  @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.status = :status AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByBranchIdAndStatusAndAppointmentDateBetween(@Param("branchId") Long branchId,
                                                           @Param("status") AppointmentStatus status,
                                                           @Param("startDate") LocalDateTime startDate,
                                                           @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.status = :status")
    long countByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE (d.branch.id = :branchId OR p.branch.id = :branchId) " +
            "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
            "AND a.status = :status " +
            "ORDER BY a.appointmentDate ASC")
    Page<Appointment> findByBranchIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") AppointmentStatus status,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH p.branch " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH d.branch " +
            "LEFT JOIN FETCH a.company " +
            "WHERE (d.branch.id = :branchId OR p.branch.id = :branchId) " +
            "AND a.updatedAt > :updatedAt " +
            "ORDER BY a.updatedAt DESC")
    Page<Appointment> findByBranchIdAndUpdatedAtAfterOrderByUpdatedAtDesc(
            @Param("branchId") Long branchId,
            @Param("updatedAt") LocalDateTime updatedAt,
            Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.createdAt > :createdAt")
    long countByBranchIdAndCreatedAtAfter(@Param("branchId") Long branchId, @Param("createdAt") LocalDateTime createdAt);
    //
    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByDoctorIdAndAppointmentDateBetween(@Param("doctorId") Long doctorId, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByDoctorIdAndStatusAndAppointmentDateBetween(@Param("doctorId") Long doctorId, @Param("status") AppointmentStatus status, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status")
    long countByDoctorIdAndStatus(@Param("doctorId") Long doctorId, @Param("status") AppointmentStatus status);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH a.doctor d " +
            "WHERE d.id = :doctorId " +
            "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
            "AND a.status = :status " +
            "ORDER BY a.appointmentDate ASC")
    Page<Appointment> findByDoctorIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            @Param("status") AppointmentStatus status,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH a.doctor d " +
            "WHERE d.id = :doctorId " +
            "AND a.appointmentDate BETWEEN :startDate AND :endDate " +
            "ORDER BY a.appointmentDate DESC")
    Page<Appointment> findByDoctorIdAndAppointmentDateBetweenOrderByAppointmentDateDesc(
            @Param("doctorId") Long doctorId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH a.doctor d " +
            "WHERE d.id = :doctorId " +
            "AND a.updatedAt > :updatedAt " +
            "ORDER BY a.updatedAt DESC")
    Page<Appointment> findByDoctorIdAndUpdatedAtAfterOrderByUpdatedAtDesc(
            @Param("doctorId") Long doctorId,
            @Param("updatedAt") LocalDateTime updatedAt,
            Pageable pageable);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH a.company c " +
            "WHERE (:doctorId IS NULL OR d.id = :doctorId) " +
            "AND (:companyId IS NULL OR c.id = :companyId) " +
            "AND (:branchId IS NULL OR d.branch.id = :branchId OR p.branch.id = :branchId) " +
            "ORDER BY a.appointmentDate ASC")
    List<Appointment> findByDoctorIdAndCompanyIdAndBranchId(
            @Param("doctorId") Long doctorId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId);

    @Query("SELECT a FROM Appointment a " +
            "LEFT JOIN FETCH a.patient p " +
            "LEFT JOIN FETCH a.doctor d " +
            "LEFT JOIN FETCH a.company c " +
            "WHERE (:doctorId IS NULL OR d.id = :doctorId) " +
            "AND (:companyId IS NULL OR c.id = :companyId) " +
            "AND (:branchId IS NULL OR d.branch.id = :branchId OR p.branch.id = :branchId) " +
            "AND (:startDate IS NULL OR a.appointmentDate >= :startDate) " +
            "AND (:endDate IS NULL OR a.appointmentDate <= :endDate) " +
            "ORDER BY a.appointmentDate ASC")
    List<Appointment> findByDoctorIdAndCompanyIdAndBranchIdAndDateRange(
            @Param("doctorId") Long doctorId,
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.branch.id = :branchId AND a.doctor.department = :department")
    long countByBranchIdAndDepartment(@Param("branchId") Long branchId, @Param("department") String department);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.branch.id = :branchId AND a.doctor.department = :department AND a.status = :status")
    long countByBranchIdAndDepartmentAndStatus(@Param("branchId") Long branchId, @Param("department") String department, @Param("status") AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.branch.id = :branchId AND" +
            " a.doctor.department = :department AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByBranchIdAndDepartmentAndAppointmentDateBetween(
            @Param("branchId") Long branchId,
            @Param("department") String department,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.company.id = :companyId AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByCompanyIdAndAppointmentDateBetween(@Param("companyId") Long companyId,
                                                   @Param("startDate") LocalDateTime startDate,
                                                   @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.company.id = :companyId AND a.status = :status " +
            "AND a.appointmentDate BETWEEN :startDate AND :endDate")
    long countByCompanyIdAndStatusAndAppointmentDateBetween(@Param("companyId") Long companyId,
                                                            @Param("status") AppointmentStatus status,
                                                            @Param("startDate") LocalDateTime startDate,
                                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.company.id = :companyId AND a.status = :status")
    long countByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") AppointmentStatus status);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.company.id = :companyId AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByCompanyIdAndCreatedAtBetween(@Param("companyId") Long companyId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.company.id = :companyId AND a.status = :status " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByCompanyIdAndStatusAndCreatedAtBetween(@Param("companyId") Long companyId,
                                                      @Param("status") AppointmentStatus status,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByBranchIdAndCreatedAtBetween(@Param("branchId") Long branchId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId AND a.status = :status " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByBranchIdAndStatusAndCreatedAtBetween(@Param("branchId") Long branchId,
                                                     @Param("status") AppointmentStatus status,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByDoctorIdAndCreatedAtBetween(@Param("doctorId") Long doctorId,
                                            @Param("startDate") LocalDateTime startDate,
                                            @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByDoctorIdAndStatusAndCreatedAtBetween(@Param("doctorId") Long doctorId,
                                                     @Param("status") AppointmentStatus status,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.branch.id = :branchId")
    long countByBranchId(@Param("branchId") Long id);

    long countByPatientIdAndStatus(Long patientId, AppointmentStatus appointmentStatus);

    long countByPatientId(Long patientId);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByPatientIdAndCreatedAtBetween(@Param("patientId") Long patientId,
                                             @Param("startDate") LocalDateTime startDate,
                                             @Param("endDate") LocalDateTime endDate);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.patient.id = :patientId AND a.status = :status " +
            "AND a.createdAt BETWEEN :startDate AND :endDate")
    long countByPatientIdAndStatusAndCreatedAtBetween(@Param("patientId") Long patientId,
                                                      @Param("status") AppointmentStatus status,
                                                      @Param("startDate") LocalDateTime startDate,
                                                      @Param("endDate") LocalDateTime endDate);

    Page<Appointment> findByPatientIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(Long patientId,
                                                                                                 LocalDateTime startDate,
                                                                                                 LocalDateTime endDate,
                                                                                                 AppointmentStatus status,
                                                                                                 Pageable pageable);

    Page<Appointment> findByPatientIdAndAppointmentDateBetweenOrderByAppointmentDateDesc(Long patientId, LocalDateTime startDate, LocalDateTime endDate, Pageable pageable);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.doctor.branch.id = :branchId AND a.status = :status AND a.createdAt > :createdAt")
    long countByBranchIdAndStatusAndCreatedAtAfter(@Param("branchId") Long id, @Param("status") AppointmentStatus appointmentStatus, @Param("createdAt") LocalDateTime thirtyDaysAgo);

    @Query("SELECT COUNT(a) FROM Appointment a")
    long countTotalAppointments();

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.createdAt BETWEEN :startOfDay AND :endOfDay")
    long countByCreatedAtBetween(@Param("startOfDay") LocalDateTime startOfDay, @Param("endOfDay") LocalDateTime endOfDay);

    @Query("SELECT COUNT(a) FROM Appointment a WHERE a.status = :status")
    long countByStatus(@Param("status") AppointmentStatus status);

    @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.branch.id = :branchId AND" +
            " a.createdAt BETWEEN :startDate AND :endDate")
    long countDistinctPatientsByBranchIdAndDateRange(@Param("branchId") Long branchId,
                                                     @Param("startDate") LocalDateTime startDate,
                                                     @Param("endDate") LocalDateTime endDate);

    @Query("SELECT p.firstName, p.lastName, d.doctorName, a.status, a.appointmentDate " +
            "FROM Appointment a " +
            "JOIN a.patient p " +
            "JOIN a.doctor d " +
            "WHERE (d.branch.id = :branchId OR p.branch.id = :branchId) " +
            "AND a.createdAt > :sinceDate " +
            "ORDER BY a.createdAt DESC")
    List<Object[]> findRecentAppointmentsByBranchId(@Param("branchId") Long branchId,
                                                    @Param("sinceDate") LocalDateTime sinceDate,
                                                    Pageable pageable);

    @Query("""
            SELECT 
            COUNT(a) as totalAppointments,
            SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN a.status IN ('REQUESTED','CONFIRMED') THEN 1 ELSE 0 END) as pendingRequested,
            SUM(CASE WHEN a.status = 'NO_SHOW' THEN 1 ELSE 0 END) as noShow
            FROM Appointment a
            WHERE a.appointmentDate BETWEEN :startMs AND :endMs
            AND (:branchId IS NULL OR a.branch.id = :branchId)
            """)
    AppointmentStatsProjection getAppointmentStats(LocalDateTime startMs, LocalDateTime endMs, Long branchId);

    @Query("""
            SELECT 
            COUNT(a) as totalAppointments,
            SUM(CASE WHEN a.status = 'COMPLETED' THEN 1 ELSE 0 END) as completed,
            SUM(CASE WHEN a.status IN ('REQUESTED','CONFIRMED') THEN 1 ELSE 0 END) as pendingRequested,
            SUM(CASE WHEN a.status = 'NO_SHOW' THEN 1 ELSE 0 END) as noShow
            FROM Appointment a
            WHERE a.appointmentDate BETWEEN :startMs AND :endMs
            AND (:companyId IS NULL OR a.company.id = :companyId)
            """)
    AppointmentStatsProjection getAppointmentStatsByCompanyId(LocalDateTime startMs, LocalDateTime endMs, Long companyId);

    @Query("""
        SELECT a
        FROM Appointment a
        LEFT JOIN FETCH a.patient
        LEFT JOIN FETCH a.doctor
        LEFT JOIN FETCH a.branch
        WHERE 
            a.company.id = :companyId
        AND (:branchId IS NULL OR a.branch.id = :branchId)
        AND a.appointmentDate BETWEEN :startOfDay AND :endOfDay
        AND a.appointmentDate >= :currentTime
        ORDER BY a.appointmentDate ASC
        """)
    List<Appointment> findTodayUpcomingAppointments(
            Long companyId,
            Long branchId,
            LocalDateTime startOfDay,
            LocalDateTime endOfDay,
            LocalDateTime currentTime,
            Pageable pageable
    );

    @Query("""
            SELECT COUNT(a)
            FROM Appointment a
            WHERE 
                a.branch.id = :branchId
            AND a.status = :status
            AND a.createdAtMs BETWEEN :startMillis AND :endMillis
            """)
    long countByBranchIdAndStatusAndMonth(
            Long branchId,
            AppointmentStatus status,
            Long startMillis,
            Long endMillis
    );

    @Query("""
       SELECT COUNT(a)
       FROM Appointment a
       WHERE a.branch.id = :branchId
       AND a.createdAt BETWEEN :start AND :end
       """)
    long countByBranchIdAndCreatedAtBetween(
            @Param("branchId") Long branchId,
            @Param("start") Long start,
            @Param("end") Long end
    );

    @Query("""
       SELECT COUNT(a)
       FROM Appointment a
       WHERE a.branch.id = :branchId
       AND a.status = :status
       AND a.createdAt BETWEEN :start AND :end
       """)
    long countByBranchIdAndStatusAndCreatedAtBetween(
            @Param("branchId") Long branchId,
            @Param("status") AppointmentStatus status,
            @Param("start") Long start,
            @Param("end") Long end
    );
}