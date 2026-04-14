package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.AppointmentDTO;
import com.sbpl.OPD.dto.BillDTO;
import com.sbpl.OPD.dto.MedicalRecordDTO;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.enums.BillStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Bill;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.model.MedicalRecord;
import com.sbpl.OPD.model.VitalSigns;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BillRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.repository.MedicalRecordRepository;
import com.sbpl.OPD.repository.VitalSignsRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.PatientDashboardService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class PatientDashboardServiceImpl implements PatientDashboardService {

    private static final Logger logger = LoggerFactory.getLogger(PatientDashboardServiceImpl.class);

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private BillRepository billRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private VitalSignsRepository vitalSignsRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Override
    public ResponseEntity<?> getPatientDashboardStatistics(Long patientId) {
        try {
            logger.info("Fetching dashboard statistics for patientId={}", patientId);

            Customer patient = customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> dashboardStats = new HashMap<>();

            // Patient basic info
            dashboardStats.put("patientName", patient.getFirstName() + " " + patient.getLastName());
            dashboardStats.put("patientEmail", patient.getEmail());
            dashboardStats.put("registrationDate", patient.getCreatedAt());

            // Appointment statistics
            long totalAppointments = appointmentRepository.countByPatientId(patientId);
            long upcomingAppointments = appointmentRepository.countByPatientIdAndStatus(patientId,
                AppointmentStatus.CONFIRMED);
            long completedAppointments = appointmentRepository.countByPatientIdAndStatus(patientId,
                AppointmentStatus.COMPLETED);
            long noShowAppointments = appointmentRepository.countByPatientIdAndStatus(patientId,
                AppointmentStatus.NO_SHOW);

            dashboardStats.put("totalAppointments", totalAppointments);
            dashboardStats.put("upcomingAppointments", upcomingAppointments);
            dashboardStats.put("completedAppointments", completedAppointments);
            dashboardStats.put("noShowAppointments", noShowAppointments);

            // No-show rate
            double noShowRate = totalAppointments > 0 ? 
                (double) noShowAppointments / totalAppointments * 100 : 0.0;
            dashboardStats.put("noShowRate", String.format("%.2f%%", noShowRate));

            // Medical records count
            long medicalRecordsCount = medicalRecordRepository.findByPatientId(patientId, PageRequest.of(0, 1))
                    .getTotalElements();
            dashboardStats.put("medicalRecords", medicalRecordsCount);

            // Billing statistics
            long totalBills = billRepository.countByPatientId(patientId);
            long paidBills = billRepository.countByPatientIdAndStatus(patientId, BillStatus.PAID);
            dashboardStats.put("totalBills", totalBills);
            dashboardStats.put("paidBills", paidBills);

            return baseResponse.successResponse("Patient dashboard statistics fetched successfully", dashboardStats);

        } catch (Exception e) {
            logger.error("Error fetching patient dashboard statistics for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching dashboard statistics: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientAppointments(Long patientId) {
        try {
            logger.info("Fetching appointments for patientId={}", patientId);

            // Validate patient exists
            customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> appointmentData = new HashMap<>();

            // Upcoming appointments (next 30 days)
            // Use business timezone (IST) instead of server timezone
            LocalDateTime now = LocalDateTime.now(DateUtils.getBusinessZone());
            LocalDateTime nextMonth = now.plusDays(30);
            PageRequest pageRequest = PageRequest.of(0, 10);

            List<Appointment> upcomingAppointments = appointmentRepository
                    .findByPatientIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
                        patientId, now, nextMonth,
                        AppointmentStatus.CONFIRMED, pageRequest)
                    .getContent();

            List<AppointmentDTO> upcomingDTOs = upcomingAppointments.stream()
                    .map(this::convertAppointmentToDTO)
                    .collect(Collectors.toList());

            // Recent appointments (last 30 days)
            LocalDateTime lastMonth = now.minusDays(30);
            List<Appointment> recentAppointments = appointmentRepository
                    .findByPatientIdAndAppointmentDateBetweenOrderByAppointmentDateDesc(
                        patientId, lastMonth, now, pageRequest)
                    .getContent();

            List<AppointmentDTO> recentDTOs = recentAppointments.stream()
                    .map(this::convertAppointmentToDTO)
                    .collect(Collectors.toList());

            appointmentData.put("upcomingAppointments", upcomingDTOs);
            appointmentData.put("recentAppointments", recentDTOs);
            appointmentData.put("totalUpcoming", upcomingAppointments.size());
            appointmentData.put("totalRecent", recentAppointments.size());

            return baseResponse.successResponse("Patient appointments fetched successfully", appointmentData);

        } catch (Exception e) {
            logger.error("Error fetching patient appointments for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching appointments: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientMedicalRecords(Long patientId) {
        try {
            logger.info("Fetching medical records for patientId={}", patientId);

            // Validate patient exists
            customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<MedicalRecord> medicalRecordsPage = medicalRecordRepository
                    .findByPatientId(patientId, pageRequest);

            List<MedicalRecordDTO> medicalRecordDTOs = medicalRecordsPage.getContent().stream()
                    .map(this::convertMedicalRecordToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(medicalRecordsPage, medicalRecordDTOs);

            return baseResponse.successResponse("Medical records fetched successfully", response);

        } catch (Exception e) {
            logger.error("Error fetching medical records for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching medical records: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientBillingHistory(Long patientId) {
        try {
            logger.info("Fetching billing history for patientId={}", patientId);

            // Validate patient exists
            customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            PageRequest pageRequest = PageRequest.of(0, 20);
            Page<Bill> billsPage = billRepository.findByPatientId(patientId, pageRequest);

            List<BillDTO> billDTOs = billsPage.getContent().stream()
                    .map(this::convertBillToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(billsPage, billDTOs);

            return baseResponse.successResponse("Billing history fetched successfully", response);

        } catch (Exception e) {
            logger.error("Error fetching billing history for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching billing history: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientHealthTrends(Long patientId) {
        try {
            logger.info("Fetching health trends for patientId={}", patientId);

            // Validate patient exists
            customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> healthTrends = new HashMap<>();

            // Get recent vital signs (last 10 records)
            PageRequest pageRequest = PageRequest.of(0, 10);
            Page<VitalSigns> vitalSignsPage = vitalSignsRepository
                    .findByPatientId(patientId, pageRequest);

            List<VitalSigns> recentVitalSigns = vitalSignsPage.getContent();

            // Calculate trends
            if (!recentVitalSigns.isEmpty()) {
                Double avgTemperature = recentVitalSigns.stream()
                        .mapToDouble(VitalSigns::getTemperature)
                        .average().orElse(0.0);

                Double avgBloodPressure = recentVitalSigns.stream()
                        .mapToDouble(vs -> (vs.getBloodPressureSystolic() != null ? vs.getBloodPressureSystolic() : 0) + 
                                         (vs.getBloodPressureDiastolic() != null ? vs.getBloodPressureDiastolic() : 0))
                        .average().orElse(0.0);

                Double avgHeartRate = recentVitalSigns.stream()
                        .mapToDouble(VitalSigns::getHeartRate)
                        .average().orElse(0.0);

                healthTrends.put("averageTemperature", String.format("%.1f", avgTemperature));
                healthTrends.put("averageBloodPressure", String.format("%.1f", avgBloodPressure));
                healthTrends.put("averageHeartRate", String.format("%.0f", avgHeartRate));
                healthTrends.put("totalMeasurements", recentVitalSigns.size());
            }

            healthTrends.put("vitalSignsHistory", recentVitalSigns);

            return baseResponse.successResponse("Health trends fetched successfully", healthTrends);

        } catch (Exception e) {
            logger.error("Error fetching health trends for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching health trends: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientDoctorRelationships(Long patientId) {
        try {
            logger.info("Fetching doctor relationships for patientId={}", patientId);

            // Validate patient exists
            customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> doctorData = new HashMap<>();

            // Get doctors this patient has visited
            PageRequest pageRequest = PageRequest.of(0, 50);
            List<Appointment> patientAppointments = appointmentRepository
                    .findByPatientId(patientId, pageRequest).getContent();

            // Extract unique doctors
            List<Doctor> visitedDoctors = patientAppointments.stream()
                    .map(Appointment::getDoctor)
                    .distinct()
                    .collect(Collectors.toList());

            List<Map<String, Object>> doctorInfo = visitedDoctors.stream()
                    .map(doctor -> {
                        Map<String, Object> info = new HashMap<>();
                        info.put("doctorId", doctor.getId());
                        info.put("doctorName", doctor.getDoctorName());
                        info.put("specialization", doctor.getSpecialization());
                        info.put("visitsCount", patientAppointments.stream()
                                .filter(app -> app.getDoctor().getId().equals(doctor.getId()))
                                .count());
                        return info;
                    })
                    .collect(Collectors.toList());

            doctorData.put("visitedDoctors", doctorInfo);
            doctorData.put("totalDoctorsVisited", visitedDoctors.size());

            return baseResponse.successResponse("Doctor relationships fetched successfully", doctorData);

        } catch (Exception e) {
            logger.error("Error fetching doctor relationships for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching doctor relationships: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientNotifications(Long patientId) {
        try {
            logger.info("Fetching notifications for patientId={}", patientId);

            // Validate patient exists
            customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> notifications = new HashMap<>();

            // Upcoming appointment reminders
            // Use business timezone (IST) instead of server timezone
            LocalDateTime now = LocalDateTime.now(DateUtils.getBusinessZone());
            LocalDateTime nextWeek = now.plusDays(7);
            PageRequest pageRequest = PageRequest.of(0, 10);

            List<Appointment> upcomingAppointments = appointmentRepository
                    .findByPatientIdAndAppointmentDateBetweenAndStatusOrderByAppointmentDateAsc(
                        patientId, now, nextWeek,
                        AppointmentStatus.CONFIRMED, pageRequest)
                    .getContent();

            List<Map<String, Object>> appointmentReminders = upcomingAppointments.stream()
                    .map(appointment -> {
                        Map<String, Object> reminder = new HashMap<>();
                        reminder.put("type", "APPOINTMENT_REMINDER");
                        reminder.put("message", "Appointment with Dr. " +
                            appointment.getDoctor().getDoctorName() +
                            " on " + appointment.getAppointmentDate());
                        reminder.put("appointmentDate", appointment.getAppointmentDate());
                        reminder.put("doctorName", appointment.getDoctor().getDoctorName());
                        return reminder;
                    })
                    .collect(Collectors.toList());

            notifications.put("appointmentReminders", appointmentReminders);
            notifications.put("totalReminders", appointmentReminders.size());

            return baseResponse.successResponse("Notifications fetched successfully", notifications);

        } catch (Exception e) {
            logger.error("Error fetching notifications for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching notifications: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getPatientHealthRecommendations(Long patientId) {
        try {
            logger.info("Fetching health recommendations for patientId={}", patientId);

            Customer patient = customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            Map<String, Object> recommendations = new HashMap<>();

            // Basic health recommendations based on patient data
            recommendations.put("generalRecommendations", List.of(
                "Maintain regular check-ups with your healthcare provider",
                "Follow prescribed medication schedules",
                "Keep track of your symptoms and vital signs",
                "Stay hydrated and maintain a balanced diet"
            ));

            // Age-based recommendations
            if (patient.getAge() != null) {
                if (patient.getAge() > 65) {
                    recommendations.put("ageSpecific", List.of(
                        "Regular cardiovascular check-ups recommended",
                        "Bone density screening consideration",
                        "Medication review with healthcare provider"
                    ));
                } else if (patient.getAge() < 18) {
                    recommendations.put("ageSpecific", List.of(
                        "Regular growth and development monitoring",
                        "Vaccination schedule maintenance",
                        "Healthy lifestyle habits development"
                    ));
                }
            }

            // Appointment-based recommendations
            long completedAppointments = appointmentRepository.countByPatientIdAndStatus(patientId,
                AppointmentStatus.COMPLETED);

            if (completedAppointments > 0) {
                recommendations.put("followUp", "Based on your visit history, consider scheduling regular follow-up appointments");
            } else {
                recommendations.put("firstVisit", "Welcome! Consider scheduling your first comprehensive health check-up");
            }

            return baseResponse.successResponse("Health recommendations fetched successfully", recommendations);

        } catch (Exception e) {
            logger.error("Error fetching health recommendations for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching health recommendations: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getTodaysPatientAppointments(Long patientId) {
        try {
            logger.info("Fetching today's appointments for patientId={}", patientId);

            // Validate patient exists
            Customer patient = customerRepository.findById(patientId)
                    .orElseThrow(() -> new RuntimeException("Patient not found"));

            // Get today's date range in business timezone
            LocalDateTime[] todayRange = DateUtils.getBusinessDayRange();
            LocalDateTime startOfDay = todayRange[0];
            LocalDateTime endOfDay = todayRange[1];

            // Count appointments created today
            long totalAppointments = appointmentRepository.countByPatientIdAndCreatedAtBetween(
                patientId, startOfDay, endOfDay);
            
            long pendingAppointments = appointmentRepository.countByPatientIdAndStatusAndCreatedAtBetween(
                patientId, AppointmentStatus.REQUESTED, startOfDay, endOfDay);
            
            long confirmedAppointments = appointmentRepository.countByPatientIdAndStatusAndCreatedAtBetween(
                patientId, AppointmentStatus.CONFIRMED, startOfDay, endOfDay);
            
            long completedAppointments = appointmentRepository.countByPatientIdAndStatusAndCreatedAtBetween(
                patientId, AppointmentStatus.COMPLETED, startOfDay, endOfDay);
            
            long cancelledAppointments = appointmentRepository.countByPatientIdAndStatusAndCreatedAtBetween(
                patientId, AppointmentStatus.CANCELLED, startOfDay, endOfDay);
            
            long rescheduledAppointments = appointmentRepository.countByPatientIdAndStatusAndCreatedAtBetween(
                patientId, AppointmentStatus.RESCHEDULED, startOfDay, endOfDay);
            
            long noShowAppointments = appointmentRepository.countByPatientIdAndStatusAndCreatedAtBetween(
                patientId, AppointmentStatus.NO_SHOW, startOfDay, endOfDay);

            Map<String, Object> todayStats = new HashMap<>();
            todayStats.put("pending", pendingAppointments);
            todayStats.put("completed", completedAppointments);
            todayStats.put("totalAppointments", totalAppointments);
            todayStats.put("confirmed", confirmedAppointments);
            todayStats.put("cancelled", cancelledAppointments);
            todayStats.put("rescheduled", rescheduledAppointments);
            todayStats.put("noShow", noShowAppointments);
            todayStats.put("date", DateUtils.getBusinessLocalDate());

            return baseResponse.successResponse("Today's appointment statistics fetched successfully", todayStats);

        } catch (Exception e) {
            logger.error("Error fetching today's appointments for patientId={}: {}", patientId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Error fetching today's appointments: " + e.getMessage());
        }
    }

    // Helper methods for DTO conversion
    private AppointmentDTO convertAppointmentToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setDoctorId(appointment.getDoctor().getId());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setScheduledTime(appointment.getScheduledTime());
        dto.setStatus(appointment.getStatus());
        dto.setReason(appointment.getReason());
        dto.setNotes(appointment.getNotes());
        if (appointment.getCreatedAt() != null) {
            dto.setCreatedAt(appointment.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        if (appointment.getUpdatedAt() != null) {
            dto.setUpdatedAt(appointment.getUpdatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        return dto;
    }

    private MedicalRecordDTO convertMedicalRecordToDTO(MedicalRecord record) {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setId(record.getId());
        dto.setPatientId(record.getPatient().getId());
        dto.setDoctorId(record.getDoctor().getId());
        dto.setAppointmentId(record.getAppointment() != null ? record.getAppointment().getId() : null);
        dto.setRecordType(record.getRecordType());
        dto.setFileName(record.getFileName());
        dto.setFilePath(record.getFilePath());
        dto.setMimeType(record.getMimeType());
        dto.setFileSize(record.getFileSize());
        dto.setDescription(record.getDescription());
        if (record.getCreatedAt() != null) {
            dto.setCreatedAt(record.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        }
        dto.setPatientName(record.getPatient().getFirstName() + " " + record.getPatient().getLastName());
        dto.setDoctorName(record.getDoctor().getDoctorName());
        return dto;
    }

    private BillDTO convertBillToDTO(Bill bill) {
        BillDTO dto = new BillDTO();
        dto.setId(bill.getId());
        dto.setPatientId(bill.getPatient().getId());
        dto.setAppointmentId(bill.getAppointment().getId());
        dto.setTotalAmount(bill.getTotalAmount());
        dto.setStatus(bill.getStatus());
//        dto.setCreatedAt(bill.getCreatedAt());
        return dto;
    }
}