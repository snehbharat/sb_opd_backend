package com.sbpl.OPD.repository;

import com.sbpl.OPD.dto.repository.BillStatsProjection;
import com.sbpl.OPD.dto.repository.PaymentTypeStats;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.model.Bill;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

/**
 * Repository interface for Bill entity operations.
 * <p>
 * Provides database access methods for billing records,
 * including custom queries for analytics and reporting.
 *
 * @author Rahul Kumar
 */
@Repository
public interface BillRepository extends JpaRepository<Bill, Long> {

    /**
     * Find bills by patient ID with pagination
     */
    Page<Bill> findByPatientId(Long patientId, Pageable pageable);

    /**
     * Find bills by billing staff ID with pagination
     */
    Page<Bill> findByBillingStaffId(Long staffId, Pageable pageable);

    /**
     * Find bills by status with pagination
     */
    Page<Bill> findByStatus(BillStatus status, Pageable pageable);

    Page<Bill> findByStatusAndBillingStaffId(
            BillStatus status,
            Long billingStaffId,
            Pageable pageable
    );

    Page<Bill> findAll(Pageable pageable);

    @Query("""
            SELECT b
            FROM Bill b
            WHERE b.status <> :excludedStatus
            AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
            """)
    Page<Bill> findAllBillsByDateRangeExcludingStatus(BillStatus excludedStatus,
                                                      long startDateMs,
                                                      long endDateMs,
                                                      Pageable pageable);

    /**
     * Find bills by appointment ID with pagination
     */
    Page<Bill> findByAppointmentId(Long appointmentId, Pageable pageable);

    /**
     * Find bills by branch ID with pagination
     */
    Page<Bill> findByBranchId(Long branchId, Pageable pageable);

    @Query("""
            SELECT b
            FROM Bill b
            WHERE b.branch.id = :branchId
            AND b.status = :status
            AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
            """)
    Page<Bill> findBillsByBranchStatusAndDateRange(Long branchId,
                                                   BillStatus status,
                                                   long startDateMs,
                                                   long endDateMs,
                                                   Pageable pageable);

    @Query("""
            SELECT b
            FROM Bill b
            WHERE b.branch.id = :branchId
            AND b.status <> :excludedStatus
            AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
            """)
    Page<Bill> findBillsByBranchAndDateRangeExcludingStatus(Long branchId,
                                                            BillStatus excludedStatus,
                                                            long startDateMs,
                                                            long endDateMs,
                                                            Pageable pageable);

    /**
     * Find bills by company ID with pagination
     */
    Page<Bill> findByCompanyId(Long companyId, Pageable pageable);

    /**
     * Find bills by company within a date range while excluding a specific status.
     *
     * @param companyId      company ID
     * @param excludedStatus status to exclude (e.g. DELETED)
     * @param startDateMs    start date in epoch milliseconds
     * @param endDateMs      end date in epoch milliseconds
     * @param pageable       pagination information
     * @return paginated list of bills
     */
    @Query("""
            SELECT b
            FROM Bill b
            WHERE b.company.id = :companyId
            AND b.status <> :excludedStatus
            AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
            """)
    Page<Bill> findBillsByCompanyAndDateRangeExcludingStatus(Long companyId,
                                                             BillStatus excludedStatus,
                                                             long startDateMs,
                                                             long endDateMs,
                                                             Pageable pageable);

  @Query("""
      SELECT 
          COUNT(b) as totalBills,
          COALESCE(SUM(COALESCE(b.totalAmount,0) - COALESCE(b.previousDue,0)),0) as totalAmount,
          COALESCE(SUM(COALESCE(b.paidAmount,0)),0) as collectedAmount,
          COALESCE(SUM(COALESCE(b.balanceAmount,0) - COALESCE(b.previousDue,0)),0) as balanceAmount,
          COALESCE(SUM(COALESCE(b.balanceAmount,0)),0) as dueAmountToday
      FROM Bill b
      WHERE b.branch.id = :branchId
      AND b.status <> :excludedStatus
      AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
      """)
  BillStatsProjection getBranchBillStats(
      Long branchId,
      BillStatus excludedStatus,
      long startDateMs,
      long endDateMs
  );

  @Query("""
      SELECT 
          COUNT(b) as totalBills,
          COALESCE(SUM(COALESCE(b.totalAmount,0) - COALESCE(b.previousDue,0)),0) as totalAmount,
          COALESCE(SUM(COALESCE(b.paidAmount,0)),0) as collectedAmount,
          COALESCE(SUM(COALESCE(b.balanceAmount,0) - COALESCE(b.previousDue,0)),0) as balanceAmount,
          COALESCE(SUM(COALESCE(b.balanceAmount,0)),0) as dueAmountToday
      FROM Bill b
      WHERE b.company.id = :companyId
      AND b.status <> :excludedStatus
      AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
      """)
  BillStatsProjection getCompanyBillStats(Long companyId,
                                          BillStatus excludedStatus,
                                          long startDateMs,
                                          long endDateMs);

  @Query("""
        SELECT 
            COUNT(b) as totalBills,
            COALESCE(SUM(COALESCE(b.totalAmount,0) - COALESCE(b.previousDue,0)),0) as totalAmount,
            COALESCE(SUM(COALESCE(b.paidAmount,0)),0) as collectedAmount,
            COALESCE(SUM(COALESCE(b.balanceAmount,0) - COALESCE(b.previousDue,0)),0) as balanceAmount,
            COALESCE(SUM(COALESCE(b.balanceAmount,0)),0) as dueAmountToday
        FROM Bill b
        WHERE b.status <> :excludedStatus
        AND b.createdAtMs BETWEEN :startDateMs AND :endDateMs
        """)
  BillStatsProjection getSystemBillStats(BillStatus excludedStatus,
                                         long startDateMs,
                                         long endDateMs);

    /**
     * Find bills by status and branch ID with pagination
     */
    Page<Bill> findByStatusAndBranchId(BillStatus status, Long branchId, Pageable pageable);

    /**
     * Find bills by status and company ID with pagination
     */
    Page<Bill> findByStatusAndCompanyId(BillStatus status, Long companyId, Pageable pageable);

    /**
     * Find bills by patient ID and branch ID with pagination
     */
    Page<Bill> findByPatientIdAndBranchId(Long patientId, Long branchId, Pageable pageable);

    /**
     * Find bills by patient ID and company ID with pagination
     */
    Page<Bill> findByPatientIdAndCompanyId(Long patientId, Long companyId, Pageable pageable);

    /**
     * Check if bill number already exists
     */
    boolean existsByBillNumber(String billNumber);

    /**
     * Find bill by bill number
     */
    java.util.Optional<Bill> findByBillNumber(String billNumber);

    /**
     * Get billing statistics for a specific staff member
     */
    @Query("SELECT COUNT(b.id) as totalBills, " +
            "SUM(b.totalAmount) as totalAmountBilled, " +
            "SUM(b.paidAmount) as totalAmountCollected, " +
            "SUM(b.balanceAmount) as totalAmountPending, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b WHERE b.billingStaff.id = :staffId")
    Object[] getStaffBillingStatistics(@Param("staffId") Long staffId);

    /**
     * Get billing statistics for all staff members
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id) as totalBills, " +
            "SUM(b.totalAmount) as totalAmountBilled, " +
            "SUM(b.paidAmount) as totalAmountCollected, " +
            "SUM(b.balanceAmount) as totalAmountPending, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b GROUP BY b.billingStaff.id")
    List<Object[]> getAllStaffBillingStatistics();

    /**
     * Get staff billing statistics for current month
     */
    @Query("SELECT COUNT(b.id) as billsThisMonth, " +
            "SUM(b.totalAmount) as amountBilledThisMonth, " +
            "SUM(b.paidAmount) as amountCollectedThisMonth " +
            "FROM Bill b WHERE b.billingStaff.id = :staffId " +
            "AND b.createdAt BETWEEN :startOfMonth AND :endOfMonth")
    Object[] getStaffMonthlyStatistics(@Param("staffId") Long staffId,
                                       @Param("startOfMonth") LocalDateTime startOfMonth,
                                       @Param("endOfMonth") LocalDateTime endOfMonth);

    /**
     * Get staff billing statistics for a specific date range
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id) as totalBills, " +
            "SUM(b.totalAmount) as totalAmountBilled, " +
            "SUM(b.paidAmount) as totalAmountCollected, " +
            "SUM(b.balanceAmount) as totalAmountPending, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b WHERE b.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsByDateRange(@Param("startDate") LocalDateTime startDate,
                                                        @Param("endDate") LocalDateTime endDate);

    /**
     * Get staff billing statistics for today
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.createdAt BETWEEN :startOfDay AND :endOfDay " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsForToday(
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Get staff billing statistics for current month
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.createdAt BETWEEN :startOfMonth AND :endOfMonth " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsForCurrentMonth(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth);

    /**
     * Get detailed bills for a specific staff member with date filtering
     */
    @Query("SELECT b FROM Bill b WHERE b.billingStaff.id = :staffId " +
            "AND b.createdAt BETWEEN :startDate AND :endDate")
    Page<Bill> findBillsByStaffIdAndDateRange(@Param("staffId") Long staffId,
                                              @Param("startDate") LocalDateTime startDate,
                                              @Param("endDate") LocalDateTime endDate,
                                              Pageable pageable);

    /**
     * Get bills for a specific staff member for today
     */
    @Query("SELECT b FROM Bill b WHERE b.billingStaff.id = :staffId " +
            "AND b.createdAt BETWEEN :startOfDay AND :endOfDay")
    Page<Bill> findBillsByStaffIdForToday(
            @Param("staffId") Long staffId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay,
            Pageable pageable);

    /**
     * Get bills for a specific staff member for current month
     */
    @Query("SELECT b FROM Bill b WHERE b.billingStaff.id = :staffId " +
            "AND b.createdAt BETWEEN :startOfMonth AND :endOfMonth")
    Page<Bill> findBillsByStaffIdForCurrentMonth(@Param("staffId") Long staffId,
                                                 @Param("startOfMonth") LocalDateTime startOfMonth,
                                                 @Param("endOfMonth") LocalDateTime endOfMonth,
                                                 Pageable pageable);

    /**
     * Get pending bills for a specific staff member
     */
    Page<Bill> findByBillingStaffIdAndStatus(Long staffId, BillStatus status, Pageable pageable);

    /**
     * Get top performing staff by collection rate
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id) as totalBills, " +
            "SUM(b.paidAmount) as totalCollected, " +
            "SUM(b.totalAmount) as totalBilled, " +
            "(SUM(b.paidAmount) / SUM(b.totalAmount) * 100) as collectionRate " +
            "FROM Bill b " +
            "WHERE b.status = 'PAID' OR b.status = 'PARTIALLY_PAID' " +
            "GROUP BY b.billingStaff.id " +
            "ORDER BY collectionRate DESC")
    List<Object[]> getTopPerformingStaffByCollectionRate();

    /**
     * Count bills by patient ID
     */
    long countByPatientId(Long patientId);

    /**
     * Count bills by patient ID and status
     */
    long countByPatientIdAndStatus(Long patientId, BillStatus status);

    /**
     * Calculate total system revenue
     */
    @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.status = 'PAID'")
    BigDecimal calculateTotalSystemRevenue();

    /**
     * Count total bills
     */
    @Query("SELECT COUNT(b) FROM Bill b")
    long countTotalBills();

    /**
     * Count paid bills
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.status = 'PAID'")
    long countPaidBills();

    /**
     * Count total bills by status
     */
    long countByStatus(BillStatus status);

    /**
     * Count bills by billing staff ID
     */
    long countByBillingStaffId(Long staffId);

    /**
     * Count bills by billing staff ID and status
     */
    long countByBillingStaffIdAndStatus(Long staffId, BillStatus status);

    /**
     * Count bills by billing staff ID and date range
     */
    long countByBillingStaffIdAndCreatedAtBetween(Long staffId, Date startDate, Date endDate);

    /**
     * Count bills by billing staff ID, status, and date range
     */
    long countByBillingStaffIdAndStatusAndCreatedAtBetween(Long staffId, BillStatus status, Date startDate, Date endDate);

    /**
     * Calculate total revenue by billing staff ID
     */
    @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.billingStaff.id = :staffId")
    BigDecimal calculateTotalRevenueByStaff(@Param("staffId") Long staffId);

    /**
     * Calculate payments by billing staff ID and date range
     */
    @Query("SELECT SUM(b.paidAmount) FROM Bill b WHERE b.billingStaff.id = :staffId AND b.updatedAt BETWEEN :startDate AND :endDate")
    BigDecimal calculatePaymentsByStaffAndDateRange(@Param("staffId") Long staffId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * Calculate total revenue by billing staff ID and date range
     */
    @Query("SELECT SUM(b.totalAmount) FROM Bill b WHERE b.billingStaff.id = :staffId AND b.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal calculateTotalRevenueByStaffAndDateRange(@Param("staffId") Long staffId, @Param("startDate") Date startDate, @Param("endDate") Date endDate);

    /**
     * Calculate pending amount by billing staff ID
     */
    @Query("SELECT SUM(b.balanceAmount) FROM Bill b WHERE b.billingStaff.id = :staffId AND (b.status = 'PENDING' OR b.status = 'PARTIALLY_PAID')")
    BigDecimal calculatePendingAmountByStaff(@Param("staffId") Long staffId);

    /**
     * Count today's pending bills by billing staff ID
     */
    @Query("SELECT COUNT(b) FROM Bill b WHERE b.billingStaff.id = :staffId AND b.createdAt >= :today AND (b.status = 'PENDING' OR b.status = 'PARTIALLY_PAID')")
    long countTodayPendingBillsByStaff(@Param("staffId") Long staffId, @Param("today") Date today);

    /**
     * Find recent bills by billing staff ID
     */
    @Query("SELECT b FROM Bill b WHERE b.billingStaff.id = :staffId ORDER BY b.createdAt DESC")
    List<Bill> findRecentBillsByStaffId(@Param("staffId") Long staffId, org.springframework.data.domain.Pageable pageable);

    /**
     * Get staff billing statistics for today filtered by branch
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.branch.id = :branchId AND b.createdAt BETWEEN :startOfDay AND :endOfDay " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsForTodayByBranch(
            @Param("branchId") Long branchId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Get staff billing statistics for current month filtered by branch
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.branch.id = :branchId AND b.createdAt BETWEEN :startOfMonth AND :endOfMonth " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsForCurrentMonthByBranch(
            @Param("branchId") Long branchId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth);

    /**
     * Get staff billing statistics by date range and branch
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.branch.id = :branchId AND b.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsByDateRangeAndBranch(
            @Param("branchId") Long branchId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * Get all staff billing statistics filtered by branch
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id) as totalBills, " +
            "SUM(b.totalAmount) as totalAmountBilled, " +
            "SUM(b.paidAmount) as totalAmountCollected, " +
            "SUM(b.balanceAmount) as totalAmountPending, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b WHERE b.branch.id = :branchId " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getAllStaffBillingStatisticsByBranch(@Param("branchId") Long branchId);

    /**
     * Get all staff billing statistics filtered by company
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id) as totalBills, " +
            "SUM(b.totalAmount) as totalAmountBilled, " +
            "SUM(b.paidAmount) as totalAmountCollected, " +
            "SUM(b.balanceAmount) as totalAmountPending, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b WHERE b.company.id = :companyId " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getAllStaffBillingStatisticsByCompany(@Param("companyId") Long companyId);

    /**
     * Get staff billing statistics for today filtered by company
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.company.id = :companyId AND b.createdAt BETWEEN :startOfDay AND :endOfDay " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsForTodayByCompany(
            @Param("companyId") Long companyId,
            @Param("startOfDay") LocalDateTime startOfDay,
            @Param("endOfDay") LocalDateTime endOfDay);

    /**
     * Get staff billing statistics for current month filtered by company
     */
    @Query("SELECT b.billingStaff.id as staffId, " +
            "COUNT(b.id), " +
            "SUM(b.totalAmount), " +
            "SUM(b.paidAmount), " +
            "SUM(b.balanceAmount), " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END), " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) " +
            "FROM Bill b WHERE b.company.id = :companyId AND b.createdAt BETWEEN :startOfMonth AND :endOfMonth " +
            "GROUP BY b.billingStaff.id")
    List<Object[]> getStaffBillingStatisticsForCurrentMonthByCompany(
            @Param("companyId") Long companyId,
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth);

    /**
     * Get staff billing statistics by date range and company
     */
    @Query(value = """
        SELECT 
            b.billing_staff_id AS staffId,
            COUNT(b.id) AS totalBills,
            COALESCE(SUM(b.total_amount),0) AS totalAmount,
            COALESCE(SUM(b.paid_amount),0) AS paidAmount,
            COALESCE(SUM(b.balance_amount),0) AS balanceAmount,
            SUM(CASE WHEN b.status = 'PAID' THEN 1 ELSE 0 END) AS paidCount,
            SUM(CASE WHEN b.status = 'PENDING' THEN 1 ELSE 0 END) AS pendingCount,
            SUM(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 ELSE 0 END) AS partiallyPaidCount,
            SUM(CASE WHEN b.status = 'CANCELLED' THEN 1 ELSE 0 END) AS cancelledCount
        FROM aestheticq.bills b
        WHERE b.company_id = :companyId
        AND b.created_at BETWEEN :startDate AND :endDate
        GROUP BY b.billing_staff_id
        """, nativeQuery = true)
    List<Object[]> getStaffBillingStatisticsByDateRangeAndCompany(
        @Param("companyId") Long companyId,
        @Param("startDate") LocalDateTime startDate,
        @Param("endDate") LocalDateTime endDate);

    /**
     * Get comprehensive billing summary for the entire system
     */
    @Query("SELECT " +
            "SUM(b.totalAmount) as totalBilled, " +
            "SUM(b.paidAmount) as totalCollected, " +
            "SUM(b.balanceAmount) as totalDue, " +
            "COUNT(b.id) as totalBills, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b")
    Object[] getComprehensiveBillingSummary();

    /**
     * Get comprehensive billing summary filtered by branch
     */
    @Query("SELECT " +
            "SUM(b.totalAmount) as totalBilled, " +
            "SUM(b.paidAmount) as totalCollected, " +
            "SUM(b.balanceAmount) as totalDue, " +
            "COUNT(b.id) as totalBills, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b WHERE b.branch.id = :branchId")
    Object[] getComprehensiveBillingSummaryByBranch(@Param("branchId") Long branchId);

    /**
     * Get comprehensive billing summary for a specific staff
     */
    @Query("SELECT " +
            "SUM(b.totalAmount) as totalBilled, " +
            "SUM(b.paidAmount) as totalCollected, " +
            "SUM(b.balanceAmount) as totalDue, " +
            "COUNT(b.id) as totalBills, " +
            "COUNT(CASE WHEN b.status = 'PAID' THEN 1 END) as paidBills, " +
            "COUNT(CASE WHEN b.status = 'PENDING' THEN 1 END) as pendingBills, " +
            "COUNT(CASE WHEN b.status = 'PARTIALLY_PAID' THEN 1 END) as partiallyPaidBills, " +
            "COUNT(CASE WHEN b.status = 'CANCELLED' THEN 1 END) as cancelledBills " +
            "FROM Bill b WHERE b.billingStaff.id = :staffId")
    Object[] getComprehensiveBillingSummaryByStaff(@Param("staffId") Long staffId);

    /**
     * Get top performing staff by collection amount
     */
    @Query("SELECT b.billingStaff.id, " +
            "b.billingStaff.firstName, " +
            "b.billingStaff.lastName, " +
            "b.billingStaff.role, " +
            "b.billingStaff.branch.branchName, " +
            "SUM(b.paidAmount), " +
            "COUNT(b.id), " +
            "(SUM(b.paidAmount) / SUM(b.totalAmount) * 100) " +
            "FROM Bill b WHERE b.status IN ('PAID', 'PARTIALLY_PAID') " +
            "GROUP BY b.billingStaff.id, b.billingStaff.firstName, b.billingStaff.lastName, " +
            "b.billingStaff.role, b.billingStaff.branch.branchName " +
            "ORDER BY SUM(b.paidAmount) DESC")
    List<Object[]> getTopPerformingStaffByCollection();

    /**
     * Get most frequent services/bill items
     */
    @Query("SELECT bi.itemName, " +
            "COUNT(bi.id), " +
            "SUM(bi.totalPrice), " +
            "AVG(bi.totalPrice) " +
            "FROM BillItem bi JOIN bi.bill b WHERE b.status != 'CANCELLED' " +
            "GROUP BY bi.itemName ORDER BY COUNT(bi.id) DESC")
    List<Object[]> getMostFrequentServices();

    // High-value patients analysis removed as per user request

    /**
     * Get daily billing summary for last 30 days
     */
    @Query(value = "SELECT DATE(created_at) as date, " +
            "COUNT(id), " +
            "SUM(total_amount), " +
            "SUM(paid_amount), " +
            "SUM(balance_amount) " +
            "FROM aestheticq.bills WHERE created_at >= CURRENT_DATE - INTERVAL '30 days' " +
            "GROUP BY DATE(created_at) ORDER BY DATE(created_at)", nativeQuery = true)
    List<Object[]> getDailyBillingSummary();

    /**
     * Get monthly billing summary for last 12 months
     */
    @Query(value = "SELECT CONCAT(EXTRACT(MONTH FROM created_at), '-', EXTRACT(YEAR FROM created_at)), " +
            "COUNT(id), " +
            "SUM(total_amount), " +
            "SUM(paid_amount), " +
            "SUM(balance_amount) " +
            "FROM aestheticq.bills WHERE created_at >= CURRENT_DATE - INTERVAL '12 months' " +
            "GROUP BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at) " +
            "ORDER BY EXTRACT(YEAR FROM created_at), EXTRACT(MONTH FROM created_at)", nativeQuery = true)
    List<Object[]> getMonthlyBillingSummary();

    /**
     * Get bills with aging analysis (outstanding dues by age)
     */
    @Query(value = "SELECT " +
            "SUM(CASE WHEN b.created_at > CURRENT_DATE - INTERVAL '7 days' AND b.balance_amount > 0 THEN b.balance_amount ELSE 0 END), " +
            "SUM(CASE WHEN b.created_at BETWEEN CURRENT_DATE - INTERVAL '30 days' AND CURRENT_DATE - INTERVAL '7 days' AND b.balance_amount > 0 THEN b.balance_amount ELSE 0 END), " +
            "SUM(CASE WHEN b.created_at BETWEEN CURRENT_DATE - INTERVAL '90 days' AND CURRENT_DATE - INTERVAL '31 days' AND b.balance_amount > 0 THEN b.balance_amount ELSE 0 END), " +
            "SUM(CASE WHEN b.created_at <= CURRENT_DATE - INTERVAL '90 days' AND b.balance_amount > 0 THEN b.balance_amount ELSE 0 END) " +
            "FROM aestheticq.bills b WHERE b.status IN ('PENDING', 'PARTIALLY_PAID')", nativeQuery = true)
    Object[] getAgingAnalysis();

    /**
     * Get payment type breakdown for all bills
     */
    @Query("SELECT b.paymentType, " +
            "SUM(b.paidAmount), " +
            "COUNT(b.id) " +
            "FROM Bill b WHERE b.paymentType IS NOT NULL AND b.paidAmount > 0 " +
            "GROUP BY b.paymentType")
    List<Object[]> getPaymentTypeBreakdown();

    /**
     * Get payment type breakdown for a specific staff member
     */
    @Query("SELECT b.paymentType, " +
            "SUM(b.paidAmount), " +
            "COUNT(b.id) " +
            "FROM Bill b WHERE b.billingStaff.id = :staffId AND b.paymentType IS NOT NULL AND b.paidAmount > 0 " +
            "GROUP BY b.paymentType")
    List<Object[]> getPaymentTypeBreakdownByStaff(@Param("staffId") Long staffId);

    /**
     * Get payment type breakdown for a specific staff member with date range filter
     */
    @Query("SELECT b.paymentType, " +
            "SUM(b.paidAmount), " +
            "COUNT(b.id) " +
            "FROM Bill b WHERE b.billingStaff.id = :staffId " +
            "AND b.paymentType IS NOT NULL AND b.paidAmount > 0 " +
            "AND b.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY b.paymentType")
    List<Object[]> getPaymentTypeBreakdownByStaffAndDateRange(
            @Param("staffId") Long staffId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);


    @Query("""
            SELECT 
            COALESCE(SUM(CASE WHEN b.status='PAID' THEN b.totalAmount WHEN b.status='PARTIALLY_PAID' THEN b.paidAmount ELSE 0 END),0) as paidAmount,
            COALESCE(SUM(CASE WHEN b.status='PENDING' THEN b.totalAmount WHEN b.status='PARTIALLY_PAID' THEN b.balanceAmount ELSE 0 END),0) as pendingAmount,
            COUNT(CASE WHEN b.status='PAID' THEN 1 END) as paidCount,
            COUNT(CASE WHEN b.status='PENDING' THEN 1 END) as pendingCount
            FROM Bill b
            WHERE (:companyId IS NULL OR b.company.id = :companyId)
            AND (:branchId IS NULL OR b.branch.id = :branchId)
            AND b.createdAtMs BETWEEN :startDate AND :endDate
            """)
    Object[] getBillingSummary(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("startDate") Long startDate,
            @Param("endDate") Long endDate
    );

    @Query("SELECT " +
            "COALESCE(SUM(b.totalAmount), 0), " +
            "COALESCE(SUM(b.paidAmount), 0), " +
            "COALESCE(SUM(b.balanceAmount), 0) " +
            "FROM Bill b WHERE b.patient.id = :patientId")
    Object[] getBillingSummaryByPatientId(@Param("patientId") Long patientId);


    @Query("""
            SELECT COALESCE(SUM(b.balanceAmount),0)
            FROM Bill b
            WHERE b.billingStaff.id = :staffId
            AND b.status IN ('PENDING','PARTIALLY_PAID')
            AND b.createdAt BETWEEN :start AND :end
            """)
    BigDecimal calculatePendingAmountByStaffAndDateRange(
            Long staffId,
            Date start,
            Date end
    );

    @Query("""
            SELECT 
            b.paymentType as paymentType,
            COUNT(b) as totalTransactions,
            SUM(b.paidAmount) as totalPaidAmount
            FROM Bill b
            WHERE b.branch.id = :branchId
            AND b.createdAtMs BETWEEN :startMs AND :endMs
            GROUP BY b.paymentType
            """)
    List<PaymentTypeStats> getBranchPaymentStats(
            Long branchId,
            Long startMs,
            Long endMs);

    @Query("""
            SELECT 
            b.paymentType as paymentType,
            COUNT(b) as totalTransactions,
            SUM(b.paidAmount) as totalPaidAmount
            FROM Bill b
            WHERE b.company.id = :companyId
            AND b.createdAtMs BETWEEN :startMs AND :endMs
            GROUP BY b.paymentType
            """)
    List<PaymentTypeStats> getCompanyPaymentStats(
            Long companyId,
            Long startMs,
            Long endMs);

    @Query("""
            SELECT 
            b.paymentType as paymentType,
            COUNT(b) as totalTransactions,
            SUM(b.paidAmount) as totalPaidAmount
            FROM Bill b
            WHERE b.createdAtMs BETWEEN :startMs AND :endMs
            GROUP BY b.paymentType
            """)
    List<PaymentTypeStats> getPaymentStats(
            Long startMs,
            Long endMs);

    @Query("""
            SELECT 
                u.id,
                CONCAT(u.firstName,' ',u.lastName),
                br.branchName,
                u.role,
                COUNT(b.id),
                COALESCE(SUM(b.paidAmount),0)
            FROM Bill b
            JOIN b.billingStaff u
            JOIN b.branch br
            WHERE 
                b.company.id = :companyId
            AND (:branchId IS NULL OR b.branch.id = :branchId)
            AND b.createdAtMs BETWEEN :startMillis AND :endMillis
            GROUP BY u.id, u.firstName, u.lastName, br.branchName
            ORDER BY SUM(b.paidAmount) DESC
            """)
    List<Object[]> getStaffPerformanceRankingForDashboard(
            Long companyId,
            Long branchId,
            Long startMillis,
            Long endMillis,
            Pageable pageable
    );

    @Query("""
            SELECT 
                COALESCE(SUM(b.totalAmount),0),
                COALESCE(SUM(b.paidAmount),0),
                COALESCE(SUM(b.balanceAmount),0)
            FROM Bill b
            WHERE 
                b.company.id = :companyId
            AND (:branchId IS NULL OR b.branch.id = :branchId)
            AND b.createdAtMs BETWEEN :startMillis AND :endMillis
            """)
    List<Object[]> getRevenueSummary(
            Long companyId,
            Long branchId,
            Long startMillis,
            Long endMillis
    );

    @Query("""
            SELECT 
                b.paymentType,
                COUNT(b.id)
            FROM Bill b
            WHERE 
                b.company.id = :companyId
            AND (:branchId IS NULL OR b.branch.id = :branchId)
            AND b.createdAtMs BETWEEN :startMillis AND :endMillis
            GROUP BY b.paymentType
            """)
    List<Object[]> countBillsByPaymentType(
            @Param("companyId") Long companyId,
            @Param("branchId") Long branchId,
            @Param("startMillis") Long startMillis,
            @Param("endMillis") Long endMillis
    );

  @Query(
      value = "SELECT * FROM aestheticq.bills " +
          "WHERE patient_id = :patientId " +
          "ORDER BY created_at_ms DESC",
      countQuery = "SELECT COUNT(*) FROM aestheticq.bills WHERE patient_id = :patientId",
      nativeQuery = true
  )
  Page<Bill> findBillsByPatientIdNative(@Param("patientId") Long patientId, Pageable pageable);
}