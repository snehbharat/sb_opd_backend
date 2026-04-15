package com.sbpl.OPD.repository;


import com.sbpl.OPD.dto.customer.response.CustomerListView;
import com.sbpl.OPD.model.Customer;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {

  @Query("SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName, c.phoneNumber as phoneNumber, c.email as email, c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender, c.address as address, c.uhid as uhid, c.company.id as companyId, c.branch.id as branchId, CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId FROM Customer c WHERE c.email = :email")
  Optional<CustomerListView> findProjectedByEmail(String email);

  @Query("SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName, c.phoneNumber as phoneNumber, c.email as email, c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender, c.address as address, c.uhid as uhid, c.company.id as companyId, c.branch.id as branchId, CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId FROM Customer c")
  Page<CustomerListView> findAllProjectedBy(Pageable pageable);

  /**
   * Find all customers with pagination and descending order by createdAt
   * Using native query to ensure proper sorting at database level
   */
  @Query("""
      SELECT c.id as id,
      c.prefix as prefix,
      c.firstName as firstName,
      c.lastName as lastName,
      c.phoneNumber as phoneNumber,
      c.email as email,
      c.dateOfBirth as dateOfBirth,
      c.age as age,
      c.gender as gender,
      c.address as address,
      c.uhid as uhid,
      c.company.id as companyId,
      c.branch.id as branchId,
      c.totalPaidAmount as totalPaidAmount,
      c.totalDueAmount as totalDueAmount,
      c.totalBillAmount as totalBillAmount,
      CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
      (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
      (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
      (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
      (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      ORDER BY c.createdAt DESC
      """)
  Page<CustomerListView> findAllProjectedByOrderByCreatedAtDesc(Pageable pageable);

  @Query("SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName, c.phoneNumber as phoneNumber, c.email as email, c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender, c.address as address, c.uhid as uhid, c.company.id as companyId, c.branch.id as branchId, c.totalPaidAmount as totalPaidAmount, c.totalDueAmount as totalDueAmount, c.totalBillAmount, CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId FROM Customer c WHERE c.phoneNumber = :phoneNumber")
  Optional<CustomerListView> findProjectedByPhoneNumber(String phoneNumber);


  Optional<CustomerListView> findByUhid(String uhid);

  Optional<CustomerListView> findByEmail(String email);

  List<CustomerListView> findByFirstNameContainingIgnoreCaseOrLastNameContainingIgnoreCase(
      String firstName, String lastName);

  @Query("""
          SELECT
              c.id as id,
              c.prefix as prefix,
              c.firstName as firstName,
              c.lastName as lastName,
              c.phoneNumber as phoneNumber,
              c.email as email,
              c.dateOfBirth as dateOfBirth,
              c.age as age,
              c.gender as gender,
              c.address as address,
              c.uhid as uhid,
              c.company.id as companyId,
              c.branch.id as branchId,
              c.department.id as departmentId
          FROM Customer c
          WHERE 
              c.uhid = :value
              OR c.phoneNumber = :value
              OR c.email = :value
              OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :value, '%'))
              OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :value, '%'))
      """)
  List<CustomerListView> searchGlobally(@Param("value") String value);


  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.phoneNumber = :phoneNumber
      AND c.company.id = :companyId
      """)
  List<CustomerListView> findByPhoneNumberAndCompanyId(
      String phoneNumber,
      Long companyId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.phoneNumber = :phoneNumber
      AND c.branch.id = :branchId
      """)
  List<CustomerListView> findByPhoneNumberAndBranchId(
      String phoneNumber,
      Long branchId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.uhid = :uhid
      AND c.company.id = :companyId
      """)
  List<CustomerListView> findByUhidAndCompanyId(
      String uhid,
      Long companyId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.uhid = :uhid
      AND c.branch.id = :branchId
      """)
  List<CustomerListView> findByUhidAndBranchId(
      String uhid,
      Long branchId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.email = :email
      AND c.company.id = :companyId
      """)
  List<CustomerListView> findByEmailAndCompanyId(
      String email,
      Long companyId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.email = :email
      AND c.branch.id = :branchId
      """)
  List<CustomerListView> findByEmailAndBranchId(
      String email,
      Long branchId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
          OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%')))
      AND c.company.id = :companyId
      """)
  List<CustomerListView> findByNameAndCompanyId(
      String name,
      Long companyId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id, c.prefix as prefix, c.firstName as firstName, c.lastName as lastName,
             c.phoneNumber as phoneNumber, c.email as email,
             c.dateOfBirth as dateOfBirth, c.age as age, c.gender as gender,
             c.address as address, c.uhid as uhid,
             c.company.id as companyId, c.branch.id as branchId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             CASE WHEN c.department IS NOT NULL THEN c.department.id ELSE NULL END as departmentId,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE (LOWER(c.firstName) LIKE LOWER(CONCAT('%', :name, '%'))
          OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :name, '%')))
      AND c.branch.id = :branchId
      """)
  List<CustomerListView> findByNameAndBranchId(
      String name,
      Long branchId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id,
             c.prefix as prefix,
             c.firstName as firstName,
             c.lastName as lastName,
             c.phoneNumber as phoneNumber,
             c.email as email,
             c.dateOfBirth as dateOfBirth,
             c.age as age,
             c.gender as gender,
             c.address as address,
             c.uhid as uhid,
             c.company.id as companyId,
             c.branch.id as branchId,
             c.department.id as departmentId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.company.id = :companyId
      AND (
          c.uhid = :value
          OR c.phoneNumber = :value
          OR c.email = :value
          OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :value, '%'))
          OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :value, '%'))
      )
      """)
  List<CustomerListView> searchGloballyByCompany(
      String value,
      Long companyId,
      Pageable pageable);

  @Query("""
      SELECT c.id as id,
             c.prefix as prefix,
             c.firstName as firstName,
             c.lastName as lastName,
             c.phoneNumber as phoneNumber,
             c.email as email,
             c.dateOfBirth as dateOfBirth,
             c.age as age,
             c.gender as gender,
             c.address as address,
             c.uhid as uhid,
             c.company.id as companyId,
             c.branch.id as branchId,
             c.department.id as departmentId,
             c.totalPaidAmount as totalPaidAmount,
             c.totalDueAmount as totalDueAmount,
             c.totalBillAmount as totalBillAmount,
             (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
             (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
             (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
             (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
      FROM Customer c
      WHERE c.branch.id = :branchId
      AND (
          c.uhid = :value
          OR c.phoneNumber = :value
          OR c.email = :value
          OR LOWER(c.firstName) LIKE LOWER(CONCAT('%', :value, '%'))
          OR LOWER(c.lastName) LIKE LOWER(CONCAT('%', :value, '%'))
      )
      """)
  List<CustomerListView> searchGloballyByBranch(
      String value,
      Long branchId,
      Pageable pageable);


  boolean existsByPhoneNumber(String phoneNumber);

  boolean existsByEmail(String email);

  @Query("SELECT COUNT(c) FROM Customer c WHERE c.branch.id = :branchId")
  long countByBranchId(@Param("branchId") Long branchId);

  @Query("SELECT COUNT(c) FROM Customer c WHERE c.branch.id = :branchId AND c.createdAt > :createdAt")
  long countByBranchIdAndCreatedAtAfter(@Param("branchId") Long branchId, @Param("createdAt") java.time.LocalDateTime createdAt);

  // Doctor-specific analytics methods
  @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId")
  long countByDoctorId(@Param("doctorId") Long doctorId);

  @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId AND a.createdAt > :sinceDate")
  long countNewPatientsByDoctorIdAndDate(@Param("doctorId") Long doctorId, @Param("sinceDate") java.time.LocalDateTime sinceDate);

  @Query("SELECT COUNT(DISTINCT a.patient.id) FROM Appointment a WHERE a.doctor.id = :doctorId AND " +
      "a.patient.id IN (SELECT ap.patient.id FROM Appointment ap WHERE ap.doctor.id = :doctorId GROUP BY ap.patient.id HAVING COUNT(ap.id) > 1)")
  long countRepeatPatientsByDoctorId(@Param("doctorId") Long doctorId);


  @Query("SELECT COUNT(DISTINCT c.id) FROM Customer c " +
      "JOIN Appointment a ON a.patient.id = c.id " +
      "WHERE c.branch.id = :branchId AND a.appointmentDate > :appointmentDate")
  long countActivePatientsByBranch(@Param("branchId") Long branchId, @Param("appointmentDate") java.time.LocalDateTime appointmentDate);

  // Patient dashboard methods
  @Query("SELECT COUNT(b) FROM Bill b WHERE b.patient.id = :patientId")
  long countByPatientId(@Param("patientId") Long patientId);

  @Query("SELECT COUNT(b) FROM Bill b WHERE b.patient.id = :patientId AND b.status = :status")
  long countByPatientIdAndPaymentStatus(@Param("patientId") Long patientId, @Param("status") String status);

  // Super admin analytics methods
  @Query("SELECT COUNT(c) FROM Customer c")
  long countTotalPatients();

  @Query("SELECT COUNT(c) FROM Customer c")
  long countActivePatients();

  @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt > :todayStart")
  long countNewPatientsToday(@Param("todayStart") java.time.LocalDateTime todayStart);

  @Query("SELECT COUNT(c) FROM Customer c WHERE c.createdAt > :startDate")
  long countNewPatientsThisMonth(@Param("startDate") java.time.LocalDateTime startDate);

  // SAAS admin recent activities methods
  @Query("SELECT c FROM Customer c WHERE c.branch.id = :branchId AND c.createdAt > :sinceDate ORDER BY c.createdAt DESC")
  List<Customer> findRecentCustomersByBranchId(@Param("branchId") Long branchId,
                                               @Param("sinceDate") java.time.LocalDateTime sinceDate,
                                               Pageable pageable);

  @Query("SELECT c FROM Customer c WHERE c.branch.clinic.id = :companyId AND c.createdAt > :sinceDate ORDER BY c.createdAt DESC")
  List<Customer> findRecentCustomersByCompanyId(@Param("companyId") Long companyId,
                                                @Param("sinceDate") java.time.LocalDateTime sinceDate,
                                                Pageable pageable);

  @Query("""
          SELECT
              c.id as id,
              c.prefix as prefix,
              c.firstName as firstName,
              c.lastName as lastName,
              c.phoneNumber as phoneNumber,
              c.email as email,
              c.dateOfBirth as dateOfBirth,
              c.age as age,
              c.gender as gender,
              c.address as address,
              c.uhid as uhid,
              c.company.id as companyId,
              c.branch.id as branchId,
              c.department.id as departmentId,
              c.totalPaidAmount as totalPaidAmount,
              c.totalDueAmount as totalDueAmount,
              c.totalBillAmount as totalBillAmount,
              (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
              (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
              (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
              (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
          FROM Customer c
          WHERE c.company.id = :companyId
          ORDER BY c.createdAt DESC
      """)
  Page<CustomerListView> findByCompanyIdProjected(@Param("companyId") Long companyId, Pageable pageable);

  /**
   * Find customers by branch ID with pagination
   */
  @Query("""
          SELECT
              c.id as id,
              c.prefix as prefix,
              c.firstName as firstName,
              c.lastName as lastName,
              c.phoneNumber as phoneNumber,
              c.email as email,
              c.dateOfBirth as dateOfBirth,
              c.age as age,
              c.gender as gender,
              c.address as address,
              c.uhid as uhid,
              c.company.id as companyId,
              c.branch.id as branchId,
              c.department.id as departmentId,
              c.totalPaidAmount as totalPaidAmount,
              c.totalDueAmount as totalDueAmount,
              c.totalBillAmount as totalBillAmount,
              (SELECT a.appointmentNumber FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentNumber,
              (SELECT a.appointmentDate FROM Appointment a WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastAppointmentDate,
              (SELECT d.doctorName FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorName,
              (SELECT d.id FROM Appointment a JOIN a.doctor d WHERE a.patient.id = c.id ORDER BY a.appointmentDate DESC LIMIT 1) as lastDoctorId
          FROM Customer c
          WHERE c.branch.id = :branchId
          ORDER BY c.createdAt DESC
      """)
  Page<CustomerListView> findByBranchId(@Param("branchId") Long branchId, Pageable pageable);


  @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.department LEFT JOIN FETCH c.company LEFT JOIN FETCH c.branch WHERE c.id = :id")
  Optional<Customer> findByIdWithAssociations(@Param("id") Long id);

  @Query("SELECT c FROM Customer c LEFT JOIN FETCH c.department LEFT JOIN FETCH c.company LEFT JOIN FETCH c.branch WHERE c.id = :id AND c.company.id = :companyId")
  Optional<Customer> findByIdAndCompanyIdWithAssociations(@Param("id") Long id, @Param("companyId") Long companyId);

  boolean existsByUhid(String uhid);

    @Query("""
            SELECT c
            FROM Customer c
            WHERE c.company.id = :companyId
            ORDER BY c.createdAt DESC
            """)
    List<Customer> findLatestPatients(@Param("companyId") Long companyId, Pageable pageable);
}