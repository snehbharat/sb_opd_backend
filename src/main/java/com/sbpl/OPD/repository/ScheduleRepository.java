package com.sbpl.OPD.repository;

import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.model.Schedule;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.List;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {
    

    Page<Schedule> findAll(Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId")
    Page<Schedule> findByDoctorId(Long doctorId, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId AND s.status = :status")
    Page<Schedule> findByDoctorIdAndStatus(Long doctorId, ScheduleStatus status, Pageable pageable);
    
    List<Schedule> findByDoctorIdAndDayOfWeek(Long doctorId, DayOfWeek dayOfWeek);
    
    @Query("SELECT s FROM Schedule s WHERE s.status = :status")
    Page<Schedule> findByStatus(ScheduleStatus status, Pageable pageable);
    
    // Added methods for company and branch filtering with status
    @Query("SELECT s FROM Schedule s WHERE s.doctor.company.id = :companyId AND s.status = :status")
    Page<Schedule> findByCompanyIdAndStatus(@Param("companyId") Long companyId, @Param("status") ScheduleStatus status, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.branch.id = :branchId AND s.status = :status")
    Page<Schedule> findByBranchIdAndStatus(@Param("branchId") Long branchId, @Param("status") ScheduleStatus status, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.startDate BETWEEN :startDate AND :endDate")
    Page<Schedule> findByStartDateBetween(LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId AND s.startDate BETWEEN :startDate AND :endDate")
    Page<Schedule> findByDoctorIdAndStartDateBetween(Long doctorId, LocalDate startDate, LocalDate endDate, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId AND s.dayOfWeek = :dayOfWeek AND s.startDate <= :date AND (s.endDate IS NULL OR s.endDate >= :date)")
    List<Schedule> findActiveScheduleByDoctorAndDayOfWeek(@Param("doctorId") Long doctorId, 
                                                          @Param("dayOfWeek") DayOfWeek dayOfWeek, 
                                                          @Param("date") LocalDate date);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.id = :doctorId AND s.startDate <= :date AND (s.endDate IS NULL OR s.endDate >= :date) AND s.status = :status")
    List<Schedule> findActiveSchedulesByDoctor(@Param("doctorId") Long doctorId, 
                                               @Param("date") LocalDate date, 
                                               @Param("status") ScheduleStatus status);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.company.id = :companyId")
    Page<Schedule> findByCompanyId(@Param("companyId") Long companyId, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.branch.id = :branchId")
    Page<Schedule> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.company.id = :companyId AND s.doctor.id = :doctorId")
    Page<Schedule> findByCompanyIdAndDoctorId(@Param("companyId") Long companyId, @Param("doctorId") Long doctorId, Pageable pageable);
    
    @Query("SELECT s FROM Schedule s WHERE s.doctor.branch.id = :branchId AND s.doctor.id = :doctorId")
    Page<Schedule> findByBranchIdAndDoctorId(@Param("branchId") Long branchId, @Param("doctorId") Long doctorId, Pageable pageable);

    @Query("""
                SELECT DISTINCT s.dayOfWeek 
                FROM Schedule s 
                WHERE s.doctor.id = :doctorId 
                  AND s.isAvailable = true 
                  AND s.status <> ScheduleStatus.INACTIVE
            """)
    List<DayOfWeek> findActiveScheduleDays(Long doctorId);
}