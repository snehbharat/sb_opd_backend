package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.utils.DateUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class DashboardCommonService {

    private final BillRepository billRepository;
    private final UserRepository userRepository;
    private final DoctorRepository doctorRepository;
    private final CustomerRepository customerRepository;
    private final AppointmentRepository appointmentRepository;
    private final BranchRepository branchRepository;
    public List<Map<String, Object>> getStaffPerformanceRanking(Long companyId, Long branchId) {

        List<Map<String, Object>> rankingList = new ArrayList<>();

        try {

            Pageable pageable = PageRequest.of(0, 6);
            Long[] range = DateUtils.getMonthlyRangeInMilli();

            List<Object[]> results =
                    billRepository.getStaffPerformanceRankingForDashboard(
                            companyId,
                            branchId,
                            range[0],
                            range[1],
                            pageable
                    );

            int rank = 1;

            for (Object[] row : results) {

                Map<String, Object> staffData = new HashMap<>();

                staffData.put("rank", rank++);
                staffData.put("staffId", row[0]);
                staffData.put("staffName", row[1]);
                staffData.put("branchName", row[2]);
                staffData.put("role", row[3]);
                staffData.put("totalBillsGenerated", row[4]);
                staffData.put("totalAmountCollected", row[5]);

                rankingList.add(staffData);
            }

        } catch (Exception e) {
            e.getMessage();
        }

        return rankingList;
    }
    public List<Map<String, Object>> getEmployees(Long companyId, Long branchId) {

        List<Map<String, Object>> employees = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, 10);
        Long[] range = DateUtils.getMonthlyRangeInMilli();

        List<User> staffList = userRepository.getLatestEmployeesForDashboard(
                companyId,
                branchId,
                range[0],
                range[1],
                pageable
        );

        for (User staff : staffList) {

            Map<String, Object> emp = new HashMap<>();

            String fullName = Stream.of(staff.getFirstName(), staff.getLastName())
                    .filter(Objects::nonNull)
                    .collect(Collectors.joining(" "));

            emp.put("employeeId", staff.getEmployeeId());
            emp.put("name", fullName);
            emp.put("email", staff.getEmail());
            emp.put("phone", staff.getPhoneNumber());
            emp.put("role", staff.getRole());
            emp.put("department", staff.getDepartment());
            emp.put("branchName", staff.getBranch() != null ? staff.getBranch().getBranchName() : null);
            emp.put("active", staff.getIsActive());

            employees.add(emp);
        }

        return employees;
    }
    public List<Map<String, Object>> getDoctors(Long companyId, Long branchId) {

        List<Map<String, Object>> doctorsList = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, 10);
        Long[] range = DateUtils.getMonthlyRangeInMilli();

        List<Doctor> doctors = doctorRepository.getLatestDoctorsForDashboard(
                companyId,
                branchId,
                range[0],
                range[1],
                pageable
        );

        for (Doctor doctor : doctors) {

            Map<String, Object> doc = new HashMap<>();

            doc.put("doctorId", doctor.getId());
            doc.put("name", doctor.getDoctorName());
            doc.put("specialization", doctor.getSpecialization());
            doc.put("department", doctor.getDepartment());
            doc.put("branchName", doctor.getBranch() != null ? doctor.getBranch().getBranchName() : null);
            doc.put("dateOfJoining", doctor.getCreatedAt());
            doc.put("active", doctor.getIsActive());

            doctorsList.add(doc);
        }

        return doctorsList;
    }
    public List<Map<String, Object>> getLatestPatients(Long companyId) {

        List<Map<String, Object>> patients = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, 10);

        List<Customer> customerList = customerRepository.findLatestPatients(companyId, pageable);

        for (Customer patient : customerList) {

            Map<String, Object> p = new HashMap<>();

            p.put("patientId", patient.getId());
            p.put("name", (patient.getFirstName() != null ? patient.getFirstName() : "") + " " +
                    (patient.getLastName() != null ? patient.getLastName() : ""));
            p.put("phone", patient.getPhoneNumber());
            p.put("gender", patient.getGender());
            p.put("branchName", patient.getBranch() != null ? patient.getBranch().getBranchName() : null);
            p.put("registeredDate", patient.getCreatedAt());
            patients.add(p);
        }

        return patients;
    }

    public Map<String, Object> getRevenue(Long companyId, Long branchId) {

        Map<String, Object> revenue = new HashMap<>();

        Long[] range = DateUtils.getMonthlyRangeInMilli();

        List<Object[]> results = billRepository.getRevenueSummary(
                companyId,
                branchId,
                range[0],
                range[1]
        );

        BigDecimal totalGenerated = BigDecimal.ZERO;
        BigDecimal totalCollected = BigDecimal.ZERO;
        BigDecimal totalDue = BigDecimal.ZERO;

        if (!results.isEmpty()) {

            Object[] row = results.get(0);

            if (row[0] != null) totalGenerated = (BigDecimal) row[0];
            if (row[1] != null) totalCollected = (BigDecimal) row[1];
            if (row[2] != null) totalDue = (BigDecimal) row[2];
        }

        revenue.put("totalGenerated", totalGenerated);
        revenue.put("totalCollected", totalCollected);
        revenue.put("totalDue", totalDue);

        return revenue;
    }
    public Map<String, Object> getBillPaymentTypeStats(Long companyId, Long branchId) {

        Map<String, Object> stats = new HashMap<>();

        Long[] range = DateUtils.getMonthlyRangeInMilli();

        List<Object[]> results = billRepository.countBillsByPaymentType(
                companyId,
                branchId,
                range[0],
                range[1]
        );

        for (Object[] row : results) {

            String paymentType = row[0] != null ? row[0].toString() : "UNKNOWN";
            Long count = ((Number) row[1]).longValue();

            stats.put(paymentType, count);
        }

        return stats;
    }

    public List<Map<String, Object>> getLatestAppointments(Long companyId, Long branchId) {

        List<Map<String, Object>> appointments = new ArrayList<>();

        Pageable pageable = PageRequest.of(0, 10);

        LocalDateTime[] range = DateUtils.getBusinessDayRange();

        LocalDateTime now = DateUtils.getCurrentBusinessDateTime();

        List<Appointment> list =
                appointmentRepository.findTodayUpcomingAppointments(
                        companyId,
                        branchId,
                        range[0],   // start of business day
                        range[1],   // end of business day
                        now,        // current business time
                        pageable
                );

        for (Appointment a : list) {

            Map<String, Object> appt = new HashMap<>();

            String patientName = Stream.of(
                    a.getPatient().getFirstName(),
                    a.getPatient().getLastName()
            ).filter(Objects::nonNull).collect(Collectors.joining(" "));

            appt.put("appointmentId", a.getId());
            appt.put("patientName", patientName);
            appt.put("doctorName", a.getDoctor() != null ? a.getDoctor().getDoctorName() : null);
            appt.put("appointmentDate", a.getAppointmentDate());
            appt.put("appointmentTime",
                    a.getAppointmentDate() != null ? a.getAppointmentDate().toLocalTime() : null);
            appt.put("status", a.getStatus());
            appt.put("branchName",
                    a.getBranch() != null ? a.getBranch().getBranchName() : null);

            appointments.add(appt);
        }

        return appointments;
    }

    public Map<String, Object> getWeeklyBranchStats(Long branchId) {

        LocalDateTime[] weekRange = DateUtils.getBusinessWeekRange();
        LocalDateTime weekStartDateTime = weekRange[0];
        LocalDateTime weekEndDateTime = weekRange[1];

        Map<String, Object> weeklyStats = new HashMap<>();

        // Appointment date based statistics
        long weeklyTotal = appointmentRepository.countByBranchIdAndAppointmentDateBetween(
                branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalAppointments", weeklyTotal);

        long weeklyCompleted = appointmentRepository.countByBranchIdAndStatusAndAppointmentDateBetween(
                branchId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("completed", weeklyCompleted);

        double completionRate = weeklyTotal > 0 ?
                (double) weeklyCompleted / weeklyTotal * 100 : 0.0;

        completionRate = Math.min(completionRate, 100.0);

        weeklyStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        // Creation date based statistics
        long weeklyCreatedTotal = appointmentRepository.countByBranchIdAndCreatedAtBetween(
                branchId, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("totalCreated", weeklyCreatedTotal);

        long weeklyCreatedCompleted = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId, AppointmentStatus.COMPLETED, weekStartDateTime, weekEndDateTime);
        weeklyStats.put("createdCompleted", weeklyCreatedCompleted);

        double createdCompletionRate = weeklyCreatedTotal > 0 ?
                (double) weeklyCreatedCompleted / weeklyCreatedTotal * 100 : 0.0;

        createdCompletionRate = Math.min(createdCompletionRate, 100.0);

        weeklyStats.put("createdCompletionRate", Math.round(createdCompletionRate * 100.0) / 100.0);

        return weeklyStats;
    }


    public Map<String, Object> getCompanyStaffMetrics(Long companyId) {
        Map<String, Object> staffMetrics = new HashMap<>();

        List<Branch> branches = branchRepository.findByClinicId(companyId);

        long totalDoctors = 0;
        long activeDoctors = 0;
        long totalStaff = 0;
        long activeStaff = 0;

        // Define staff roles (excluding admin roles)
        List<UserRole> staffRoles = Arrays.asList(
                UserRole.RECEPTIONIST,
                UserRole.BILLING_STAFF,
                UserRole.BRANCH_MANAGER
        );

        for (Branch branch : branches) {
            totalDoctors += doctorRepository.countByBranchId(branch.getId());
            activeDoctors += doctorRepository.countByBranchIdAndIsActive(branch.getId(), true);
            totalStaff += userRepository.countStaffByBranchIdAndRoles(branch.getId(), staffRoles);
            activeStaff += userRepository.countActiveStaffByBranchIdAndRoles(branch.getId(), staffRoles);
        }

        staffMetrics.put("totalDoctors", totalDoctors);
        staffMetrics.put("activeDoctors", activeDoctors);
//        staffMetrics.put("totalBranches", branches.size());
        staffMetrics.put("totalStaff", totalStaff);
        staffMetrics.put("activeStaff", activeStaff);

        return staffMetrics;
    }

    public Map<String, Object> getBranchPerformance(Long companyId, Long branchId) {

        Map<String, Object> branchPerformance = new HashMap<>();
        Long[] monthRange = DateUtils.getMonthlyRangeInMilli();

        Branch branch = branchRepository.findByIdAndClinic_id(branchId, companyId);

        if (branch == null) {
            throw new RuntimeException("Branch not found");
        }

        Map<String, Object> branchStats = new HashMap<>();

        // Total appointments
        long totalAppointments = appointmentRepository.countByBranchIdAndCreatedAtBetween(
                branchId,
                monthRange[0],
                monthRange[1]);

        branchStats.put("totalAppointments", totalAppointments);

        // Completed appointments
        long completedAppointments = appointmentRepository.countByBranchIdAndStatusAndCreatedAtBetween(
                branchId,
                AppointmentStatus.COMPLETED,
                monthRange[0],
                monthRange[1]);

        branchStats.put("completedAppointments", completedAppointments);

        // Doctor count
        long doctorCount = doctorRepository.countByBranchId(branchId);
        branchStats.put("doctorCount", doctorCount);

        // Completion rate
        double completionRate = totalAppointments > 0
                ? (double) completedAppointments / totalAppointments * 100
                : 0.0;

        branchStats.put("completionRate", Math.round(completionRate * 100.0) / 100.0);

        branchPerformance.put(branch.getBranchName(), branchStats);

        return branchPerformance;
    }


}
