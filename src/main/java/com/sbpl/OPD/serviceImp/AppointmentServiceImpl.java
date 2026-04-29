package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.AppointmentCalendarDTO;
import com.sbpl.OPD.dto.AppointmentDTO;
import com.sbpl.OPD.dto.AppointmentWithSlotDTO;
import com.sbpl.OPD.dto.VitalSignsDTO;
import com.sbpl.OPD.dto.repository.AppointmentStatsProjection;
import com.sbpl.OPD.enums.AppointmentStatus;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.AppointmentNotificationService;
import com.sbpl.OPD.service.AppointmentNumberService;
import com.sbpl.OPD.service.AppointmentReminderService;
import com.sbpl.OPD.service.AppointmentService;
import com.sbpl.OPD.service.InvoiceService;
import com.sbpl.OPD.service.ScheduleService;
import com.sbpl.OPD.service.VitalSignsService;
import com.sbpl.OPD.utils.DateUtils;
import com.sbpl.OPD.utils.DbUtill;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of appointment service operations.
 * <p>
 * Contains core logic for booking appointments,
 * preventing duplicates, managing status transitions,
 * and enforcing scheduling rules.
 *
 * @author Rahul Kumar
 */


@Service
public class AppointmentServiceImpl implements AppointmentService {

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private VitalSignsService vitalSignsService;
    
    @Autowired
    private DoctorRepository doctorRepository;
    
    @Autowired
    private BranchRepository branchRepository;

    @Autowired
    private InvoiceService invoiceService;

    @Autowired
    private AppointmentNumberService appointmentNumberService;

    @Autowired
    private ScheduleService scheduleService;

    @Autowired
    private AppointmentReminderService appointmentReminderService;

    @Autowired
    private AppointmentNotificationService appointmentNotificationService;

    private static final Logger logger =
            LoggerFactory.getLogger(AppointmentServiceImpl.class);

    @Override
    public ResponseEntity<?> updateAppointment(Long id, AppointmentDTO appointmentDTO) {

        logger.info("Update request received to update appointment [appointmentId={}]", id);

        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            boolean isUpdated = false;

            if (appointmentDTO.getReason() != null) {
                appointment.setReason(appointmentDTO.getReason());
                isUpdated = true;
            }

            if (appointmentDTO.getNotes() != null) {
                appointment.setNotes(appointmentDTO.getNotes());
                isUpdated = true;
            }

            if (appointmentDTO.getScheduledTime() != null) {
                // Validate that the new time slot is available according to the doctor's schedule
                LocalDate appointmentDate = appointmentDTO.getScheduledTime().toLocalDate();
                LocalTime appointmentTime = appointmentDTO.getScheduledTime().toLocalTime();

                ResponseEntity<?> timeSlotValidation = validateAppointmentTimeSlot(
                        appointmentDTO.getDoctorId(), appointmentDate, appointmentTime);

                if (timeSlotValidation.getStatusCode().is2xxSuccessful() && !(Boolean) timeSlotValidation.getBody()) {
                    logger.warn("New time slot is not available [doctorId={}, date={}, time={}]",
                            appointmentDTO.getDoctorId(), appointmentDate, appointmentTime);
                    return baseResponse.errorResponse(HttpStatus.CONFLICT,
                            "The requested time slot is not available. Please select another time.");
                }

                appointment.setScheduledTime(appointmentDTO.getScheduledTime());
                isUpdated = true;
            }

            if (appointmentDTO.getAppointmentDate() != null) {
                appointment.setAppointmentDate(appointmentDTO.getAppointmentDate());
                isUpdated = true;
            }

            if (appointmentDTO.getPatientId() != null) {
                Customer patient = customerRepository.findById(appointmentDTO.getPatientId())
                        .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
                appointment.setPatient(patient);
                isUpdated = true;
            }

            if (appointmentDTO.getDoctorId() != null) {
                Doctor doctor = doctorRepository.findById(appointmentDTO.getDoctorId())
                        .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
                appointment.setDoctor(doctor);
                isUpdated = true;
            }

            if (appointmentDTO.getStatus() != null) {
                appointment.setStatus(appointmentDTO.getStatus());

                if (appointmentDTO.getStatus() == AppointmentStatus.COMPLETED) {
                    appointment.setCompletedAt(LocalDateTime.now());
                }
                isUpdated = true;
            }

            // If date/time is changed and the appointment is not already completed/cancelled, set to RESCHEDULED
            if ((appointmentDTO.getAppointmentDate() != null || appointmentDTO.getScheduledTime() != null) &&
                    appointment.getStatus() != AppointmentStatus.COMPLETED &&
                    appointment.getStatus() != AppointmentStatus.CANCELLED) {
                LocalDateTime originalDateTime = appointment.getAppointmentDate(); // Capture original time
                appointment.setStatus(AppointmentStatus.RESCHEDULED);
                // Set rescheduled tracking fields
                appointment.setRescheduledFrom(originalDateTime);
                appointment.setRescheduledTo(appointment.getAppointmentDate());
                isUpdated = true;
            }

            if (!isUpdated) {
                logger.warn("No fields provided for update [appointmentId={}]", id);
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "No fields provided for update"
                );
            }

            // Store the original status to determine if there was a status transition
            AppointmentStatus originalStatus = appointment.getStatus();
            
            // Log the state before saving
            logger.info("About to save appointment [id={}, status={}, patientId={}, doctorId={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId());

            // The @PreUpdate annotation in BaseEntity will automatically set updatedAt
            Appointment updatedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment saved successfully [id={}]", appointment.getId());

            logger.info("Appointment partially updated successfully [appointmentId={}]", id);

            // Send appropriate notification based on the status change
            AppointmentStatus newStatus = updatedAppointment.getStatus();
            if (newStatus == AppointmentStatus.RESCHEDULED) {
                appointmentNotificationService.sendRescheduleNotification(updatedAppointment);
            } else if (newStatus == AppointmentStatus.CONFIRMED && originalStatus != AppointmentStatus.CONFIRMED) {
                appointmentNotificationService.sendConfirmationNotification(updatedAppointment);
            } else if (newStatus == AppointmentStatus.CANCELLED && originalStatus != AppointmentStatus.CANCELLED) {
                appointmentNotificationService.sendCancellationNotification(updatedAppointment);
            } else if (newStatus == AppointmentStatus.COMPLETED && originalStatus != AppointmentStatus.COMPLETED) {
                appointmentNotificationService.sendCompletionNotification(updatedAppointment);
            }

            return baseResponse.successResponse(
                    "Appointment updated successfully"
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for PATCH update [appointmentId={}] | {}", id, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error during PATCH update for appointmentId={}", id, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update appointment at the moment"
            );
        }
    }


    @Override
    public ResponseEntity<?> getAppointmentById(Long id) {

        logger.info("Request received to fetch appointment [appointmentId={}]", id);

        try {
            Appointment appointment = appointmentRepository.findByIdWithAllRelationships(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            logger.info("Appointment fetched successfully [appointmentId={}]", id);

            return baseResponse.successResponse(
                    "Appointment fetched successfully",
                    convertToDTO(appointment)
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Appointment not found [appointmentId={}]", id);
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error(
                    "Error while fetching appointment [appointmentId={}]",
                    id, e
            );
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch appointment at the moment"
            );
        }
    }


    @Override
    public ResponseEntity<?> getAllAppointments(Integer pageNo, Integer pageSize, Long branchId,
                                                String startDate, String endDate) {

        logger.info(
            "Request received to fetch all appointments [pageNo={}, pageSize={}, branchId={}]",
            pageNo, pageSize, branchId
        );

        try {

            LocalDateTime[] dateRangeInMilli = new LocalDateTime[2];

            if (startDate != null && !startDate.isBlank() &&
                endDate != null && !endDate.isBlank()) {
                if (DateUtils.isValidDate(startDate) && DateUtils.isValidDate(endDate)) {
                    dateRangeInMilli = DateUtils.getStartAndEndDateTime(startDate, endDate);
                } else {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Date must be in format yyyy-MM-dd");
                }
            } else {
                dateRangeInMilli = DateUtils.getStartAndEndDateTime(startDate, endDate);
            }

            User currentUser = DbUtill.getCurrentUser();

            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                pageNo != null ? pageNo : 0,
                pageSize != null ? pageSize : 10
            );

            Page<Appointment> page;

            // Determine if user has access based on role and optional branch filter
            if (currentUser.getRole() == UserRole.SUPER_ADMIN ||
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                // High-level admins can see all appointments, optionally filter by branch
                if (branchId != null) {
                    page = appointmentRepository.findByBranchIdOrderByStatusPriority(
                        branchId, dateRangeInMilli[0],
                        dateRangeInMilli[1],
                        pageRequest);
                } else {
                    page = appointmentRepository.findOrderByStatusPriority(dateRangeInMilli[0],
                        dateRangeInMilli[1], pageRequest);
                }
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN ||
                currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                // SAAS admins can optionally filter by branch
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                            "You don't have permission to access this branch's appointments");
                    }
                    page = appointmentRepository.findByBranchIdOrderByStatusPriority(
                        branchId, dateRangeInMilli[0],
                        dateRangeInMilli[1],
                        pageRequest);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    page = appointmentRepository.findByCompanyIdOrderByStatusPriority(companyId,
                        dateRangeInMilli[0],
                        dateRangeInMilli[1], pageRequest);
                }
            } else if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                // Branch manager can see appointments in their branch
                // If branchId is provided, validate it matches their branch
                if (branchId != null) {
                    if (currentUser.getBranch() != null && currentUser.getBranch().getId().equals(branchId)) {
                        page = appointmentRepository.findByBranchIdOrderByStatusPriority(
                            branchId, dateRangeInMilli[0],
                            dateRangeInMilli[1],
                            pageRequest);
                    } else {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                            "You don't have permission to access this branch's appointments");
                    }
                } else if (currentUser.getBranch() != null) {
                    page = appointmentRepository.findByBranchIdOrderByStatusPriority(
                        currentUser.getBranch().getId(), dateRangeInMilli[0],
                        dateRangeInMilli[1],
                        pageRequest);
                } else {
                    page = Page.empty(pageRequest);
                }
            } else {
                // Other users (DOCTOR, RECEPTIONIST, PATIENT) see appointments based on their assignment
                // They cannot use branchId parameter to filter
                if (currentUser.getBranch() != null) {
                    page = appointmentRepository.findByBranchIdOrderByStatusPriority(
                        currentUser.getBranch().getId(), dateRangeInMilli[0],
                        dateRangeInMilli[1],
                        pageRequest);
                } else {
                    page = Page.empty(pageRequest);
                }
            }

            List<AppointmentDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

            Map<String, Object> response =
                DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                "Fetched {} appointments on page {}",
                page.getNumberOfElements(),
                page.getNumber()
            );

            return baseResponse.successResponse(
                "Appointments fetched successfully",
                response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(
                HttpStatus.BAD_REQUEST,
                e.getMessage()
            );

        } catch (Exception e) {
            logger.error("Error while fetching appointments", e);
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to fetch appointments at the moment. Please try again later."
            );
        }
    }

    @Override
    public ResponseEntity<?> getAllAppointmentsStats(Long branchId, String startDate, String endDate) {

        logger.info("Fetching appointment stats for branchId={}", branchId);

        try {

            LocalDateTime[] dateRange = DateUtils.getStartAndEndDateTime(startDate, endDate);
            User currentUser = DbUtill.getCurrentUser();

            // Role validation similar to your existing method
            if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
                branchId = currentUser.getBranch().getId();
            }

            AppointmentStatsProjection stats;

            if (branchId != null) {
                stats = appointmentRepository.getAppointmentStats(
                    dateRange[0], dateRange[1], branchId);
            } else {
                logger.info("Fetching appointment stats for companyId={}", currentUser.getCompany().getId());
                stats = appointmentRepository.getAppointmentStatsByCompanyId(
                    dateRange[0], dateRange[1], currentUser.getCompany().getId());
            }

            Map<String, Object> response = getStringObjectMap(stats);

            logger.info("Appointment stats fetched successfully");

            return baseResponse.successResponse(
                "Appointment stats fetched successfully",
                response
            );

        } catch (Exception e) {
            logger.error("Error fetching appointment stats", e);
            return baseResponse.errorResponse(
                HttpStatus.INTERNAL_SERVER_ERROR,
                "Unable to fetch stats"
            );
        }
    }

    private static @NonNull Map<String, Object> getStringObjectMap(AppointmentStatsProjection stats) {
        long totalAppointments = stats != null && stats.getTotalAppointments() != null ? stats.getTotalAppointments() : 0;
        long completed = stats != null && stats.getCompleted() != null ? stats.getCompleted() : 0;
        long pendingRequested = stats != null && stats.getPendingRequested() != null ? stats.getPendingRequested() : 0;
        long noShow = stats != null && stats.getNoShow() != null ? stats.getNoShow() : 0;

        Map<String, Object> response = new HashMap<>();
        response.put("totalAppointments", totalAppointments);
        response.put("completed", completed);
        response.put("pendingRequested", pendingRequested);
        response.put("noShow", noShow);
        return response;
    }

    @Override
    public ResponseEntity<?> getAppointmentsByPatientId(
            Long patientId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Fetching appointments for patient [patientId={}, pageNo={}, pageSize={}]",
                patientId, pageNo, pageSize
        );

        try {
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            Page<Appointment> page =
                    appointmentRepository.findByPatientIdWithAllRelationships(patientId, pageRequest);

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Patient appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching appointments for patientId={}", patientId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch appointments"
            );
        }
    }

    @Override
    public ResponseEntity<?> getTodayAppointmentsByPatientId(
            Long patientId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Fetching today's appointments for patient [patientId={}, pageNo={}, pageSize={}]",
                patientId, pageNo, pageSize
        );

        try {
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 1000
            );

            LocalDateTime[] dateRange = DateUtils.getStartAndEndDateTime(null, null);
            logger.info("Fetching appointments between {} and {}", dateRange[0], dateRange[1]);
            Page<Appointment> page =
                appointmentRepository.findByPatientIdAndTodayWithAllRelationships(
                    patientId,
                    dateRange[0],
                    dateRange[1],
                    pageRequest
                );

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Today's patient appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching today's appointments for patientId={}", patientId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch today's appointments"
            );
        }
    }


    @Override
    public ResponseEntity<?> getAppointmentsByDoctorId(
            Long doctorId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Fetching appointments for doctor [doctorId={}, pageNo={}, pageSize={}]",
                doctorId, pageNo, pageSize
        );

        try {
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            Page<Appointment> page =
                    appointmentRepository.findByDoctorIdWithAllRelationships(doctorId, pageRequest);

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Doctor appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching appointments for doctorId={}", doctorId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch appointments"
            );
        }
    }

    @Override
    public ResponseEntity<?> getAppointmentsByDoctorIdWithDateRange(
            Long doctorId, List<AppointmentStatus> statuses, LocalDate startDate, LocalDate endDate,
            Integer pageNo, Integer pageSize) {

        logger.info(
                "Fetching appointments for doctor with date range [doctorId={}, statuses={}, startDate={}, endDate={}, pageNo={}, pageSize={}]",
                doctorId, statuses, startDate, endDate, pageNo, pageSize
        );

        try {
            if (doctorId == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Doctor ID is required");
            }
            if (statuses == null || statuses.isEmpty()) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "At least one appointment status is required");
            }
            if (startDate == null || endDate == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Start date and end date are required");
            }

            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            LocalDateTime fromDateTime = DateUtils.getStartOfBusinessDay(startDate);
            LocalDateTime toDateTime = DateUtils.getStartOfBusinessDay(endDate.plusDays(1));

            logger.info("Date range (IST): {} to {}", startDate, endDate);
            logger.info("DateTime range: {} to {}", fromDateTime, toDateTime);
            logger.info("Statuses: {}", statuses);

            Page<Appointment> page = appointmentRepository.findByDoctorIdAndStatusesWithDateFilter(
                    doctorId, statuses, fromDateTime, toDateTime, pageRequest
            );

            logger.info("Found {} appointments", page.getTotalElements());

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            String message = "Appointments from " + startDate + " to " + endDate +
                    " fetched successfully (CONFIRMED first, then REQUESTED, sorted by time)";

            return baseResponse.successResponse(message, response);

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching appointments for doctorId={}, statuses={}", doctorId, statuses, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch appointments"
            );
        }
    }


    @Override
    public ResponseEntity<?> getAppointmentsByStatus(
            AppointmentStatus status, Integer pageNo, Integer pageSize, Long branchId) {

        logger.info(
                "Fetching appointments by status [status={}, pageNo={}, pageSize={}, branchId={}]",
                status, pageNo, pageSize, branchId
        );

        try {
            User currentUser = DbUtill.getCurrentUser();
            
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            Page<Appointment> page;
            
            if (currentUser.getRole() == UserRole.SUPER_ADMIN ||
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                // Super admins can see all appointments with given status
                page = appointmentRepository.findByStatusWithAllRelationships(status, pageRequest);
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN ||
                       currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                // SAAS admins can optionally filter by branch
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's appointments");
                    }
                    page = appointmentRepository.findByBranchIdAndStatusWithAllRelationships(branchId, status, pageRequest);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    page = appointmentRepository.findByCompanyIdAndStatusWithAllRelationships(companyId, status, pageRequest);
                }
            } else {
                // Other users can only see appointments with given status in their company/branch
                if (currentUser.getCompany() != null) {
                    page = appointmentRepository.findByCompanyIdAndStatusWithAllRelationships(
                            currentUser.getCompany().getId(), status, pageRequest);
                } else if (currentUser.getBranch() != null) {
                    page = appointmentRepository.findByBranchIdAndStatusWithAllRelationships(
                            currentUser.getBranch().getId(), status, pageRequest);
                } else {
                    page = Page.empty(pageRequest);
                }
            }

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching appointments by status={}", status, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch appointments"
            );
        }
    }


    @Override
    public ResponseEntity<?> deleteAppointment(Long id) {

        logger.info("Request received to delete appointment [appointmentId={}]", id);

        try {
            if (!appointmentRepository.existsById(id)) {
                logger.warn("Appointment not found for deletion [appointmentId={}]", id);
                return baseResponse.errorResponse(
                        HttpStatus.NOT_FOUND,
                        "Appointment not found"
                );
            }

            appointmentRepository.deleteById(id);

            logger.info("Appointment deleted successfully [appointmentId={}]", id);
            return baseResponse.successResponse("Appointment deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting appointment [appointmentId={}]", id, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete appointment"
            );
        }
    }


    @Override
    public ResponseEntity<?> updateAppointmentStatus(
            Long id, AppointmentStatus status) {

        logger.info(
                "Request received to update appointment status [appointmentId={}, status={}]",
                id, status
        );

        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            appointment.setStatus(status);
            appointment.setUpdatedAt(new Date());

            if (status == AppointmentStatus.COMPLETED) {
                appointment.setCompletedAt(LocalDateTime.now());
            }

            Appointment updatedAppointment = appointmentRepository.save(appointment);

            logger.info(
                    "Appointment status updated successfully [appointmentId={}, status={}]",
                    id, status
            );

            // Send appropriate notification based on the new status
            if (status == AppointmentStatus.CANCELLED) {
                appointmentNotificationService.sendCancellationNotification(updatedAppointment);
            } else if (status == AppointmentStatus.COMPLETED) {
                appointmentNotificationService.sendCompletionNotification(updatedAppointment);
            }

            return baseResponse.successResponse(
                    "Appointment status updated successfully",
                    convertToDTO(updatedAppointment)
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Status update failed | {}", e.getMessage());
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error(
                    "Unexpected error updating appointment status [appointmentId={}]",
                    id, e
            );
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update appointment status"
            );
        }
    }

    @Override
    public ResponseEntity<?> updateAppointmentTypeWithReasonAndNotes(
            Long id, String appointmentType, String notes) {

        logger.info(
                "Request received to update appointment Type [appointmentId={}, status={}]",
                id, appointmentType
        );

        try {
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            appointment.setAppointmentType(appointmentType);
            appointment.setUpdatedAt(new Date());
            appointment.setNotes(notes);

            appointmentRepository.save(appointment);

            logger.info(
                    "Appointment type updated successfully [appointmentId={}, status={}]",
                    id, appointmentType
            );

            return baseResponse.successResponse(
                    "Appointment Type updated successfully");

        } catch (IllegalArgumentException e) {
            logger.warn("Type update failed | {}", e.getMessage());
            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error(
                    "Unexpected error updating appointment Type [appointmentId={}]",
                    id, e
            );
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update appointment type"
            );
        }
    }


    private AppointmentDTO convertToDTO(Appointment appointment) {
        AppointmentDTO dto = new AppointmentDTO();
        dto.setId(appointment.getId());
        dto.setPatientId(appointment.getPatient().getId());
        dto.setDoctorId(appointment.getDoctor().getId());
        dto.setStatus(appointment.getStatus());
        dto.setAppointmentDate(appointment.getAppointmentDate());
        dto.setReason(appointment.getReason());
        dto.setNotes(appointment.getNotes());

        dto.setScheduledTime(appointment.getScheduledTime());
        dto.setCompletedAt(appointment.getCompletedAt());
        dto.setConsultationNotes(appointment.getConsultationNotes());

        dto.setAppointmentType(appointment.getAppointmentType());

        dto.setCancellationReason(appointment.getCancellationReason());
        dto.setRescheduledFrom(appointment.getRescheduledFrom());
        dto.setRescheduledTo(appointment.getRescheduledTo());
        dto.setIsEmergency(appointment.getIsEmergency());
        dto.setPriority(appointment.getPriority());
        dto.setFollowUpRequired(appointment.getFollowUpRequired());
        dto.setFollowUpDate(appointment.getFollowUpDate());
        dto.setIsActive(appointment.isActive());

        // Set names for display
        dto.setPatientName(appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName());
        dto.setDoctorName(appointment.getDoctor().getDoctorName());
        dto.setAppointmentNumber(appointment.getAppointmentNumber());

        // Set company and branch information
        if (appointment.getCompany() != null) {
            dto.setCompanyId(appointment.getCompany().getId());
        }

        // Set branch from appointment's actual branch (patient's branch used during creation)
        if (appointment.getBranch() != null) {
            dto.setBranchId(appointment.getBranch().getId());
            dto.setBranchName(appointment.getBranch().getBranchName());
        } else if (appointment.getPatient() != null && appointment.getPatient().getBranch() != null) {
            // Fallback to patient's branch
            dto.setBranchId(appointment.getPatient().getBranch().getId());
            dto.setBranchName(appointment.getPatient().getBranch().getBranchName());
        } else if (appointment.getDoctor() != null && appointment.getDoctor().getBranch() != null) {
            // Last fallback to doctor's branch
            dto.setBranchId(appointment.getDoctor().getBranch().getId());
            dto.setBranchName(appointment.getDoctor().getBranch().getBranchName());
        }

        // Set timestamp fields - convert Date to LocalDateTime
        if (appointment.getCreatedAt() != null) {
            dto.setCreatedAt(appointment.getCreatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        if (appointment.getUpdatedAt() != null) {
            dto.setUpdatedAt(appointment.getUpdatedAt().toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime());
        }
        
        // Set no-show fields
        dto.setNoShowReason(appointment.getNoShowReason());
        dto.setNoShowRecordedAt(appointment.getNoShowRecordedAt());
        dto.setNoShowRecordedBy(appointment.getNoShowRecordedBy());
        
        // Set no-show recorded by name if available
        if (appointment.getNoShowRecordedBy() != null) {
            try {
                User recordedByUser = userRepository.findById(appointment.getNoShowRecordedBy()).orElse(null);
                if (recordedByUser != null) {
                    dto.setNoShowRecordedByName(recordedByUser.getFirstName() + " " + recordedByUser.getLastName());
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch user name for no-show recorded by [userId={}]", 
                           appointment.getNoShowRecordedBy());
            }
        }

        // Set workflow state
//        dto.setWorkflowState("APPOINTMENT_" + appointment.getStatus().toString());

        return dto;
    }
    
    private AppointmentCalendarDTO convertToCalendarDTO(Appointment appointment) {
        // Calculate end time (assuming 30 minutes duration for appointments)
        LocalDateTime endTime = appointment.getScheduledTime().plusMinutes(30);
        
        AppointmentCalendarDTO calendarDTO = new AppointmentCalendarDTO(
            appointment.getId(),
            appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName() + " - " + appointment.getReason(),
            appointment.getPatient().getFirstName() + " " + appointment.getPatient().getLastName(),
            appointment.getDoctor().getDoctorName(),
            appointment.getReason(),
            appointment.getStatus(),
            appointment.getScheduledTime(),
            endTime,
            appointment.getAppointmentNumber()
        );
        
        return calendarDTO;
    }
    
    /**
     * Checks if a branch is accessible to the current user
     * @param branch The branch to check
     * @param user The current user
     * @return true if accessible, false otherwise
     */
    private boolean isBranchAccessibleToUser(Branch branch, User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
            return true;
        }
        
        if (user.getRole() == UserRole.BRANCH_MANAGER) {
            return user.getBranch() != null && user.getBranch().getId().equals(branch.getId());
        }
        Long userCompanyId = DbUtill.getLoggedInCompanyId();
        return branch.getClinic() != null && branch.getClinic().getId().equals(userCompanyId);
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> getMyAppointments(Integer pageNo, Integer pageSize) {
        User currentUser = DbUtill.getCurrentUser();
        PageRequest pageRequest = DbUtill.buildPageRequest(pageNo, pageSize);

        Page<Appointment> page;
        if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
            // Branch manager sees all appointments in their branch
            if (currentUser.getBranch() != null) {
                page = appointmentRepository.findByBranchIdWithAllRelationships(currentUser.getBranch().getId(), pageRequest);
            } else {
                page = Page.empty();
            }
        } else if (currentUser.getRole().toString().contains("DOCTOR")) {
            // Doctor sees appointments with their patients
            page = appointmentRepository.findByDoctorIdWithAllRelationships(currentUser.getId(), pageRequest);
        } else if (currentUser.getRole().toString().contains("RECEPTIONIST")) {
            // Receptionist can see appointments in their company or branch
            if (currentUser.getCompany() != null) {
                page = appointmentRepository.findByCompanyIdWithAllRelationships(
                        currentUser.getCompany().getId(), pageRequest);
            } else if (currentUser.getBranch() != null) {
                page = appointmentRepository.findByBranchIdWithAllRelationships(
                        currentUser.getBranch().getId(), pageRequest);
            } else {
                page = Page.empty();
            }
        } else {
            // Patient sees their own appointments
            page = appointmentRepository.findByPatientIdWithAllRelationships(currentUser.getId(), pageRequest);
        }

        List<AppointmentDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

        return baseResponse.successResponse(
                "My appointments fetched successfully",
                response
        );
    }

    @Transactional(readOnly = true)
    @Override
    public ResponseEntity<?> getMyAppointmentsByStatus(AppointmentStatus status, Integer pageNo, Integer pageSize) {
        User currentUser = DbUtill.getCurrentUser();
        PageRequest pageRequest = DbUtill.buildPageRequest(pageNo, pageSize);

        Page<Appointment> page;
        if (currentUser.getRole() == UserRole.BRANCH_MANAGER) {
            // Branch manager sees all appointments in their branch with specific status
            if (currentUser.getBranch() != null) {
                page = appointmentRepository.findByBranchIdAndStatusWithAllRelationships(currentUser.getBranch().getId(), status, pageRequest);
            } else {
                page = Page.empty();
            }
        } else if (currentUser.getRole().toString().contains("DOCTOR")) {
            page = appointmentRepository.findByDoctorIdAndStatusWithAllRelationships(currentUser.getId(), status, pageRequest);
        } else if (currentUser.getRole().toString().contains("RECEPTIONIST")) {
            // Receptionist can see appointments with specific status in their company or branch
            if (currentUser.getCompany() != null) {
                page = appointmentRepository.findByCompanyIdAndStatusWithAllRelationships(
                        currentUser.getCompany().getId(), status, pageRequest);
            } else if (currentUser.getBranch() != null) {
                page = appointmentRepository.findByBranchIdAndStatusWithAllRelationships(
                        currentUser.getBranch().getId(), status, pageRequest);
            } else {
                page = Page.empty();
            }
        } else {
            page = appointmentRepository.findByPatientIdAndStatusWithAllRelationships(currentUser.getId(), status, pageRequest);
        }

        List<AppointmentDTO> dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

        return baseResponse.successResponse(
                "My appointments by status fetched successfully",
                response
        );
    }


    @Transactional(timeout = 30)
    @Override
    public ResponseEntity<?> updateMyAppointment(Long id, AppointmentDTO appointmentDTO) {
        try {
            User currentUser = DbUtill.getCurrentUser();

            // Verify the user has rights to update this appointment
            Appointment existingAppointment = appointmentRepository.findByIdWithPatientAndDoctor(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            boolean hasPermission = false;
            if (currentUser.getRole().toString().contains("DOCTOR")) {
                hasPermission = existingAppointment.getDoctor().getId().equals(currentUser.getId());
            } else if (currentUser.getRole().toString().contains("RECEPTIONIST")) {
                hasPermission = true; // Receptionists can update appointments
            } else {
                hasPermission = existingAppointment.getPatient().getId().equals(currentUser.getId());
            }

            if (!hasPermission) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN, "You don't have permission to update this appointment");
            }

            return updateAppointment(id, appointmentDTO);

        } catch (Exception e) {
            logger.error("Error updating appointment [appointmentId={}]", id, e);
            // Check if it's a transaction-related exception
            if (e instanceof TransactionSystemException) {
                return baseResponse.errorResponse(
                        HttpStatus.CONFLICT,
                        "Unable to update appointment due to data integrity issues. Please try again."
                );
            }
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update appointment: " + e.getMessage()
            );
        }
    }

    @Transactional
    @Override
    public ResponseEntity<?> cancelMyAppointment(Long id) {
        try {
            User currentUser = DbUtill.getCurrentUser();

            // Fetch appointment normally - let's test if the basic transaction works
            Appointment appointment = appointmentRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            boolean hasPermission = false;

            if (currentUser.getRole() == UserRole.SUPER_ADMIN ||
                    currentUser.getRole() == UserRole.SAAS_ADMIN ||
                    currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER ||
                    currentUser.getRole() == UserRole.BRANCH_MANAGER ||
                    currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                hasPermission = true; // Higher roles can cancel any appointment
            } else if (currentUser.getRole().toString().contains("DOCTOR")) {
                // Use ID comparison instead of accessing the lazy-loaded entity
                hasPermission = appointment.getDoctor().getId().equals(currentUser.getId());
            } else if (currentUser.getRole().toString().contains("RECEPTIONIST")) {
                hasPermission = true; // Receptionists can cancel appointments
            } else {
                // Use ID comparison instead of accessing the lazy-loaded entity
                hasPermission = appointment.getPatient().getId().equals(currentUser.getId());
            }

            if (!hasPermission) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN, "You don't have permission to cancel this appointment");
            }

            appointment.setStatus(AppointmentStatus.CANCELLED);

            // Log the state before saving
            logger.info("About to save appointment [id={}, status={}, patientId={}, doctorId={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId());

            // Ensure all required fields are properly set
            if (appointment.getPatient() == null) {
                throw new IllegalArgumentException("Appointment patient cannot be null");
            }
            if (appointment.getDoctor() == null) {
                throw new IllegalArgumentException("Appointment doctor cannot be null");
            }
            if (appointment.getAppointmentDate() == null) {
                throw new IllegalArgumentException("Appointment date cannot be null");
            }
            if (appointment.getReason() == null || appointment.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Appointment reason cannot be null or empty");
            }

            Appointment savedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment saved successfully [id={}]", savedAppointment.getId());

            // Send cancellation notification to patient
            appointmentNotificationService.sendCancellationNotification(savedAppointment);

            return baseResponse.successResponse("Appointment cancelled successfully");
        } catch (Exception e) {
            logger.error("Error cancelling appointment [appointmentId={}]", id, e);
            // Check if it's a transaction-related exception
            if (e instanceof TransactionSystemException) {
                return baseResponse.errorResponse(
                        HttpStatus.CONFLICT,
                        "Unable to cancel appointment due to data integrity issues. Please try again."
                );
            }
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to cancel appointment: " + e.getMessage()
            );
        }
    }

    @Transactional(timeout = 30)
    @Override
    public ResponseEntity<?> rescheduleMyAppointment(Long id, AppointmentDTO appointmentDTO) {
        try {
            User currentUser = DbUtill.getCurrentUser();

            Appointment appointment = appointmentRepository.findByIdWithPatientAndDoctor(id)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            boolean hasPermission = false;
            if (currentUser.getRole().toString().contains("DOCTOR")) {
                hasPermission = appointment.getDoctor().getId().equals(currentUser.getId());
            } else if (currentUser.getRole().toString().contains("RECEPTIONIST")) {
                hasPermission = true; // Receptionists can reschedule appointments
            } else {
                hasPermission = appointment.getPatient().getId().equals(currentUser.getId());
            }

            if (!hasPermission) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN, "You don't have permission to reschedule this appointment");
            }

            // Update the appointment date/time
            boolean isUpdated = false;
            LocalDateTime originalDateTime = appointment.getAppointmentDate(); // Capture original time for rescheduledFrom

            if (appointmentDTO.getAppointmentDate() != null) {
                appointment.setAppointmentDate(appointmentDTO.getAppointmentDate());
                isUpdated = true;
            }
            if (appointmentDTO.getScheduledTime() != null) {
                // Validate that the new time slot is available according to the doctor's schedule
                LocalDate appointmentDate = appointmentDTO.getScheduledTime().toLocalDate();
                LocalTime appointmentTime = appointmentDTO.getScheduledTime().toLocalTime();

                ResponseEntity<?> timeSlotValidation = validateAppointmentTimeSlot(
                        appointment.getDoctor().getId(), appointmentDate, appointmentTime);

                if (timeSlotValidation.getStatusCode().is2xxSuccessful() && !(Boolean) timeSlotValidation.getBody()) {
                    logger.warn("New time slot is not available for rescheduling [doctorId={}, date={}, time={}]",
                            appointment.getDoctor().getId(), appointmentDate, appointmentTime);
                    return baseResponse.errorResponse(HttpStatus.CONFLICT,
                            "The requested time slot is not available for rescheduling. Please select another time.");
                }

                appointment.setScheduledTime(appointmentDTO.getScheduledTime());
                isUpdated = true;
            }
            if (appointmentDTO.getNotes() != null) {
                appointment.setNotes(appointmentDTO.getNotes());
                isUpdated = true;
            }

            // Update status to RESCHEDULED if date/time changes were made and it's not already completed/cancelled
            if (isUpdated && appointment.getStatus() != AppointmentStatus.COMPLETED &&
                    appointment.getStatus() != AppointmentStatus.CANCELLED) {
                appointment.setStatus(AppointmentStatus.RESCHEDULED);
                // Set rescheduled tracking fields
                appointment.setRescheduledFrom(originalDateTime);
                appointment.setRescheduledTo(appointment.getAppointmentDate());
            }

            // Log the state before saving
            logger.info("About to save appointment [id={}, status={}, patientId={}, doctorId={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId());

            // Ensure all required fields are properly set
            if (appointment.getPatient() == null) {
                throw new IllegalArgumentException("Appointment patient cannot be null");
            }
            if (appointment.getDoctor() == null) {
                throw new IllegalArgumentException("Appointment doctor cannot be null");
            }
            if (appointment.getAppointmentDate() == null) {
                throw new IllegalArgumentException("Appointment date cannot be null");
            }
            if (appointment.getReason() == null || appointment.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Appointment reason cannot be null or empty");
            }

            // The @PreUpdate annotation in BaseEntity will automatically set updatedAt
            Appointment updatedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment rescheduled successfully [id={}]", updatedAppointment.getId());

            // Send reschedule notification to patient if status is RESCHEDULED
            if (updatedAppointment.getStatus() == AppointmentStatus.RESCHEDULED) {
                appointmentNotificationService.sendRescheduleNotification(updatedAppointment);
            }

            return baseResponse.successResponse(
                    "Appointment rescheduled successfully"
            );

        } catch (Exception e) {
            logger.error("Error rescheduling appointment [appointmentId={}]", id, e);
            // Check if it's a transaction-related exception
            if (e instanceof TransactionSystemException) {
                return baseResponse.errorResponse(
                        HttpStatus.CONFLICT,
                        "Unable to reschedule appointment due to data integrity issues. Please try again."
                );
            }
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to reschedule appointment: " + e.getMessage()
            );
        }
    }

    @Override
    public ResponseEntity<?> raiseInvoiceFromAppointment(Long appointmentId) {
        return invoiceService.raiseInvoiceFromAppointment(appointmentId);
    }

    @Override
    public ResponseEntity<?> createMyVitalSigns(VitalSignsDTO vitalSignsDTO) {
        return vitalSignsService.createMyVitalSigns(vitalSignsDTO);
    }

    /**
     * Utility method to populate company information for existing appointments
     * This should be called once to fix existing data
     */
    public void populateCompanyInformationForExistingAppointments() {
        logger.info("Starting to populate company information for existing appointments");

        try {
            List<Appointment> appointments = appointmentRepository.findAll();
            int updatedCount = 0;

            for (Appointment appointment : appointments) {
                // Only update appointments that don't have company set
                if (appointment.getCompany() == null && appointment.getPatient() != null) {
                    Customer patient = appointment.getPatient();
                    if (patient.getCompany() != null) {
                        appointment.setCompany(patient.getCompany());
                        appointmentRepository.save(appointment);
                        updatedCount++;
                        logger.info("Updated appointment {} with company {}",
                                appointment.getId(), patient.getCompany().getId());
                    }
                }
            }

            logger.info("Completed populating company information. Updated {} appointments", updatedCount);

        } catch (Exception e) {
            logger.error("Error populating company information for existing appointments", e);
        }
    }

    @Override
    public ResponseEntity<?> getVitalSignsByAppointment(Long appointmentId, Integer pageNo, Integer pageSize) {
        return vitalSignsService.getVitalSignsByAppointment(appointmentId, pageNo, pageSize);
    }

    @Override
    public ResponseEntity<?> validateAppointmentTimeSlot(Long doctorId, LocalDate date, LocalTime time) {
        try {
            logger.info("Validating appointment time slot for doctorId: {}, date: {}, time: {}",
                    doctorId, date, time);

            // Check if the time slot is available in the schedule
            ResponseEntity<?> scheduleCheck = scheduleService.checkAvailability(doctorId, date, time);

            if (!scheduleCheck.getStatusCode().is2xxSuccessful()) {
                return scheduleCheck; // Return the error response from schedule service
            }

            // Handle the Map response from schedule service
            Object scheduleResponseBody = scheduleCheck.getBody();
            Boolean scheduleAvailable = null;
            String scheduleMessage = null;
            String scheduleReason = null;

            if (scheduleResponseBody instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> scheduleResponse = (Map<String, Object>) scheduleResponseBody;
                scheduleAvailable = (Boolean) scheduleResponse.get("available");
                scheduleMessage = (String) scheduleResponse.get("message");
                scheduleReason = (String) scheduleResponse.get("reason");
            } else {
                logger.error("Unexpected response type from schedule service: {}",
                        scheduleResponseBody != null ? scheduleResponseBody.getClass().getName() : "null");
                return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to validate time slot availability");
            }

            if (Boolean.TRUE.equals(scheduleAvailable)) {
                // Also check if there's no existing appointment at this time
                LocalDateTime dateTime = LocalDateTime.of(date, time);
                List<Appointment> existingAppointments = appointmentRepository
                        .findByDoctorIdAndScheduledTime(doctorId, dateTime);

                if (existingAppointments.isEmpty()) {
                    logger.info("Time slot is available for appointment [doctorId={}, date={}, time={}]",
                            doctorId, date, time);
                    Map<String, Object> response = new HashMap<>();
                    response.put("available", true);
                    response.put("message", "Time slot is available");
                    response.put("doctorId", doctorId);
                    response.put("date", date);
                    response.put("time", time);
                    return ResponseEntity.ok(response);
                } else {
                    logger.warn("Time slot already has an appointment [doctorId={}, date={}, time={}]",
                            doctorId, date, time);
                    Map<String, Object> response = new HashMap<>();
                    response.put("available", false);
                    response.put("message", "This time slot is already booked. Please select another time.");
                    response.put("reason", "ALREADY_BOOKED");
                    response.put("doctorId", doctorId);
                    response.put("date", date);
                    response.put("time", time);
                    response.put("existingAppointments", existingAppointments.size());
                    return ResponseEntity.ok(response);
                }
            } else {
                logger.warn("Time slot is not available according to schedule [doctorId={}, date={}, time={}]",
                        doctorId, date, time);
                Map<String, Object> response = new HashMap<>();
                response.put("available", false);
                response.put("message", "This time slot is not available in the doctor's schedule. Please select another time.");
                response.put("reason", "SCHEDULE_UNAVAILABLE");
                response.put("doctorId", doctorId);
                response.put("date", date);
                response.put("time", time);
                return ResponseEntity.ok(response);
            }
        } catch (Exception e) {
            logger.error("Error validating appointment time slot [doctorId={}, date={}, time={}]",
                    doctorId, date, time, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to validate appointment time slot: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> createAppointmentWithSlot(AppointmentWithSlotDTO appointmentWithSlotDTO) {
        logger.info(
                "Appointment creation with slot validation request received [doctorId={}, patientId={}, date={}, time={}]",
                appointmentWithSlotDTO.getDoctorId(),
                appointmentWithSlotDTO.getPatientId(),
                appointmentWithSlotDTO.getAppointmentDate(),
                appointmentWithSlotDTO.getAppointmentTime()
        );

        try {
            // Validate doctor exists
            if (!doctorRepository.existsById(appointmentWithSlotDTO.getDoctorId())) {
                logger.warn("Doctor not found [doctorId={}]", appointmentWithSlotDTO.getDoctorId());
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Selected doctor does not exist");
            }

            // Validate patient exists
            if (!customerRepository.existsById(appointmentWithSlotDTO.getPatientId())) {
                logger.warn("Patient not found [patientId={}]", appointmentWithSlotDTO.getPatientId());
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Selected patient does not exist");
            }

            // Check for duplicate appointments
            LocalDateTime appointmentDateTime = LocalDateTime.of(
                    appointmentWithSlotDTO.getAppointmentDate(),
                    appointmentWithSlotDTO.getAppointmentTime()
            );

            // Check for any existing appointment with the same doctor, patient, and exact date/time
            boolean appointmentExists = appointmentRepository
                    .existsByDoctorIdAndPatientIdAndAppointmentDate(
                            appointmentWithSlotDTO.getDoctorId(),
                            appointmentWithSlotDTO.getPatientId(),
                            appointmentDateTime
                    );

            if (appointmentExists) {
                logger.warn("Duplicate appointment attempt [doctorId={}, patientId={}, appointmentDateTime={}]",
                        appointmentWithSlotDTO.getDoctorId(),
                        appointmentWithSlotDTO.getPatientId(),
                        appointmentDateTime);
                return baseResponse.errorResponse(HttpStatus.CONFLICT,
                        "You already have an appointment with this doctor for the selected date and time");
            }

            // Validate that the requested time slot is available according to the doctor's schedule
            ResponseEntity<?> timeSlotValidation = validateAppointmentTimeSlot(
                    appointmentWithSlotDTO.getDoctorId(),
                    appointmentWithSlotDTO.getAppointmentDate(),
                    appointmentWithSlotDTO.getAppointmentTime()
            );

            if (!timeSlotValidation.getStatusCode().is2xxSuccessful()) {
                return timeSlotValidation;
            }

            // Handle the new Map response format
            Object responseBody = timeSlotValidation.getBody();
            Boolean isAvailable = null;
            String message = null;
            String reason = null;

            if (responseBody instanceof Map) {
                Map<String, Object> validationResponse = (Map<String, Object>) responseBody;
                isAvailable = (Boolean) validationResponse.get("available");
                message = (String) validationResponse.get("message");
                reason = (String) validationResponse.get("reason");
            } else {
                logger.error("Unexpected response type from validateAppointmentTimeSlot: {}",
                        responseBody != null ? responseBody.getClass().getName() : "null");
                return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to validate time slot availability");
            }

            if (Boolean.FALSE.equals(isAvailable)) {
                logger.warn("Requested time slot is not available [doctorId={}, date={}, time={}, reason={}]",
                        appointmentWithSlotDTO.getDoctorId(),
                        appointmentWithSlotDTO.getAppointmentDate(),
                        appointmentWithSlotDTO.getAppointmentTime(),
                        reason != null ? reason : "UNKNOWN");

                String errorMessage = message != null ? message : "The requested time slot is not available. Please select another time.";
                HttpStatus statusCode = "ALREADY_BOOKED".equals(reason) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

                return baseResponse.errorResponse(statusCode, errorMessage);
            }

            // Create the appointment
            Appointment appointment = new Appointment();
            Doctor doctor = doctorRepository.findById(appointmentWithSlotDTO.getDoctorId()).orElse(null);
            Customer patient = customerRepository.findById(appointmentWithSlotDTO.getPatientId()).orElse(null);

            if (doctor == null || patient == null) {
                return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to retrieve doctor or patient information");
            }

            appointment.setDoctor(doctor);
            appointment.setPatient(patient);
            appointment.setStatus(appointmentWithSlotDTO.getStatus() != null ?
                    appointmentWithSlotDTO.getStatus() : AppointmentStatus.REQUESTED);
            appointment.setAppointmentDate(appointmentDateTime);
            appointment.setScheduledTime(appointmentDateTime);
            appointment.setReason(appointmentWithSlotDTO.getReason());
            appointment.setNotes(appointmentWithSlotDTO.getNotes());
            appointment.setConsultationNotes(appointmentWithSlotDTO.getConsultationNotes());
            appointment.setIsEmergency(appointmentWithSlotDTO.getIsEmergency());
            appointment.setPriority(appointmentWithSlotDTO.getPriority());
            appointment.setFollowUpRequired(appointmentWithSlotDTO.getFollowUpRequired());
            appointment.setAppointmentType(appointmentWithSlotDTO.getAppointmentType());
            // Determine and set appointment type if not explicitly provided
//            String appointmentType = determineAppointmentType(appointmentWithSlotDTO, doctor, patient);
//            appointment.setAppointmentType(appointmentType);
            
            // Set company and branch with proper priority
            // Priority 1: Use branchId from DTO if provided
            // Priority 2: Use patient's branch
            // Priority 3: Use doctor's branch
            if (appointmentWithSlotDTO.getBranchId() != null) {
                User currentUser = DbUtill.getCurrentUser();
                Branch branch = branchRepository.findById(appointmentWithSlotDTO.getBranchId())
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + appointmentWithSlotDTO.getBranchId()));
                
                // Validate branch access for non-super-admin users
                if (currentUser.getRole() != UserRole.SUPER_ADMIN && 
                    currentUser.getRole() != UserRole.SUPER_ADMIN_MANAGER) {
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        logger.warn("User attempted to create appointment for inaccessible branch [userId={}, branchId={}]", 
                                   currentUser.getId(), appointmentWithSlotDTO.getBranchId());
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to create appointments for this branch");
                    }
                }
                
                appointment.setBranch(branch);
                if (branch.getClinic() != null) {
                    appointment.setCompany(branch.getClinic());
                }
            }
            String appointmentNumber;
            Long apptCompanyId = appointment.getCompany() != null ? appointment.getCompany().getId() : null;
            Long apptBranchId = null;

            if (apptCompanyId != null) {
                appointmentNumber = appointmentNumberService.generateAppointmentNumber(apptCompanyId, apptBranchId);
            } else {
                Long patientCompanyId = patient.getCompany() != null ? patient.getCompany().getId() : null;
                Long patientBranchId = patient.getBranch() != null ? patient.getBranch().getId() : null;

                if (patientBranchId != null || patientCompanyId != null) {
                    appointmentNumber = appointmentNumberService.generateAppointmentNumber(patientCompanyId, patientBranchId);
                } else {
                    appointmentNumber = appointmentNumberService.generateAppointmentNumberForDoctor(doctor.getId());
                }
            }

            appointment.setAppointmentNumber(appointmentNumber);

            Appointment savedAppointment = appointmentRepository.save(appointment);

            logger.info("Appointment created successfully with slot validation [appointmentId={}, doctorId={}, patientId={}, appointmentNumber={}]",
                    savedAppointment.getId(),
                    appointmentWithSlotDTO.getDoctorId(),
                    appointmentWithSlotDTO.getPatientId(),
                    savedAppointment.getAppointmentNumber());

            // Send creation notification to patient
            // appointmentNotificationService.sendCreationNotification(savedAppointment);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointmentId", savedAppointment.getId());
            responseData.put("appointmentNumber", savedAppointment.getAppointmentNumber());
            responseData.put("doctorId", savedAppointment.getDoctor().getId());
            responseData.put("patientId", savedAppointment.getPatient().getId());
            responseData.put("appointmentDate", savedAppointment.getAppointmentDate());
            responseData.put("scheduledTime", savedAppointment.getScheduledTime());
            responseData.put("status", savedAppointment.getStatus());
            responseData.put("reason", savedAppointment.getReason());
            responseData.put("isEmergency", savedAppointment.getIsEmergency());
            responseData.put("priority", savedAppointment.getPriority());

            return baseResponse.successResponse("Appointment created successfully with slot validation", responseData);

        } catch (Exception e) {
            logger.error("Unexpected error while creating appointment with slot validation", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong while booking the appointment. Please try again later.");
        }
    }

    /**
     * Determine the appointment type based on various factors.
     * 
     * @param dto The appointment DTO
     * @param doctor The doctor entity
     * @param patient The patient entity
     * @return The determined appointment type
     * @author Rahul Kumar
     */
    private String determineAppointmentType(AppointmentWithSlotDTO dto, Doctor doctor, Customer patient) {
        // Priority 1: Use explicitly provided appointment type
        if (dto.getAppointmentType() != null && !dto.getAppointmentType().trim().isEmpty()) {
            logger.debug("Using provided appointment type: {}", dto.getAppointmentType());
            return dto.getAppointmentType().toUpperCase();
        }

        // Priority 2: Check if it's an emergency appointment
        if (Boolean.TRUE.equals(dto.getIsEmergency())) {
            logger.debug("Setting appointment type to EMERGENCY");
            return "EMERGENCY";
        }

        // Priority 3: Check if it's a follow-up appointment
        if (Boolean.TRUE.equals(dto.getFollowUpRequired()) || 
            (dto.getReason() != null && dto.getReason().toLowerCase().contains("follow-up"))) {
            logger.debug("Setting appointment type to FOLLOW_UP");
            return "FOLLOW_UP";
        }

        // Priority 4: Determine based on reason keywords
        if (dto.getReason() != null) {
            String reasonLower = dto.getReason().toLowerCase();
            
            if (reasonLower.contains("checkup") || reasonLower.contains("routine") || 
                reasonLower.contains("general") || reasonLower.contains("regular")) {
                logger.debug("Setting appointment type to ROUTINE_CHECKUP");
                return "ROUTINE_CHECKUP";
            }
            
            if (reasonLower.contains("consult") || reasonLower.contains("consultation") ||
                reasonLower.contains("new symptom") || reasonLower.contains("new problem")) {
                logger.debug("Setting appointment type to CONSULTATION");
                return "CONSULTATION";
            }
            
            if (reasonLower.contains("procedure") || reasonLower.contains("surgery") ||
                reasonLower.contains("operation")) {
                logger.debug("Setting appointment type to PROCEDURE");
                return "PROCEDURE";
            }
            
            if (reasonLower.contains("review") || reasonLower.contains("follow up") ||
                reasonLower.contains("progress")) {
                logger.debug("Setting appointment type to REVIEW");
                return "REVIEW";
            }
        }

        // Priority 5: Check if patient has previous appointments (returning patient)
        long previousAppointments = appointmentRepository.countByPatientId(patient.getId());
        if (previousAppointments == 0) {
            logger.debug("Setting appointment type to NEW_PATIENT (first appointment)");
            return "NEW_PATIENT";
        }

        // Default: Regular consultation
        logger.debug("Setting default appointment type to CONSULTATION");
        return "CONSULTATION";
    }

    @Override
    public ResponseEntity<?> getAvailableTimeSlots(Long doctorId, LocalDate date) {
        try {
            logger.info("Getting available time slots for doctorId: {}, date: {}", doctorId, date);

            // Get available slots from schedule
            ResponseEntity<?> scheduleSlots = scheduleService.getAvailableSlots(doctorId, date);
            if (!scheduleSlots.getStatusCode().is2xxSuccessful()) {
                logger.warn("Failed to get schedule slots for doctorId: {}, date: {}", doctorId, date);
                return ResponseEntity.ok(Collections.emptyList());
            }

            @SuppressWarnings("unchecked")
            List<LocalTime> availableSlots = (List<LocalTime>) scheduleSlots.getBody();

            // Filter out slots that already have appointments
            List<LocalTime> availableSlotsWithNoAppointments = new ArrayList<>();

            for (LocalTime slot : availableSlots) {
                LocalDateTime slotDateTime = LocalDateTime.of(date, slot);
                List<Appointment> existingAppointments = appointmentRepository
                        .findByDoctorIdAndScheduledTime(doctorId, slotDateTime);

                if (existingAppointments.isEmpty()) {
                    availableSlotsWithNoAppointments.add(slot);
                }
            }

            logger.info("Found {} available time slots for doctorId: {}, date: {}",
                    availableSlotsWithNoAppointments.size(), doctorId, date);
            return ResponseEntity.ok(availableSlotsWithNoAppointments);
        } catch (Exception e) {
            logger.error("Error getting available time slots [doctorId={}, date={}]", doctorId, date, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get available time slots: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> markAppointmentAsConfirmed(Long appointmentId, String confirmationNotes) {
        logger.info("Mark appointment as confirmed request received [appointmentId={}]", appointmentId);

        try {
            // Fetch the appointment
            Appointment appointment = appointmentRepository.findByIdWithAllRelationships(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Validate current status - can only confirm appointments that are REQUESTED
            if (appointment.getStatus() != AppointmentStatus.REQUESTED) {
                logger.warn("Cannot confirm appointment with status: {} [appointmentId={}]",
                        appointment.getStatus(), appointmentId);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Appointment must be REQUESTED to be marked as confirmed. Current status: " +
                                appointment.getStatus());
            }

            // Update appointment status to CONFIRMED
            appointment.setStatus(AppointmentStatus.CONFIRMED);

            // Add confirmation notes if provided
            if (confirmationNotes != null && !confirmationNotes.trim().isEmpty()) {
                appointment.setNotes(confirmationNotes);
            }

            // Validate required fields before saving
            if (appointment.getPatient() == null) {
                throw new IllegalArgumentException("Appointment patient cannot be null");
            }
            if (appointment.getDoctor() == null) {
                throw new IllegalArgumentException("Appointment doctor cannot be null");
            }
            if (appointment.getAppointmentDate() == null) {
                throw new IllegalArgumentException("Appointment date cannot be null");
            }
            if (appointment.getReason() == null || appointment.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Appointment reason cannot be null or empty");
            }

            // Log the state before saving
            logger.info("About to save confirmed appointment [id={}, status={}, patientId={}, doctorId={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId());

            Appointment confirmedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment marked as confirmed successfully [id={}]", confirmedAppointment.getId());

            // Send confirmation notification to patient
            appointmentNotificationService.sendConfirmationNotification(confirmedAppointment);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointmentId", confirmedAppointment.getId());
            responseData.put("appointmentNumber", confirmedAppointment.getAppointmentNumber());
            responseData.put("doctorId", confirmedAppointment.getDoctor().getId());
            responseData.put("patientId", confirmedAppointment.getPatient().getId());
            responseData.put("appointmentDate", confirmedAppointment.getAppointmentDate());
            responseData.put("scheduledTime", confirmedAppointment.getScheduledTime());
            responseData.put("status", confirmedAppointment.getStatus());
            responseData.put("reason", confirmedAppointment.getReason());
            responseData.put("notes", confirmedAppointment.getNotes());

            return baseResponse.successResponse("Appointment marked as confirmed successfully", responseData);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for marking appointment as confirmed [appointmentId={}] | {}", appointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error during marking appointment as confirmed [appointmentId={}]", appointmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to mark appointment as confirmed at the moment: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> markAppointmentAsComplete(Long appointmentId, String consultationNotes) {
        logger.info("Mark appointment as complete request received [appointmentId={}]", appointmentId);

        try {
            // Fetch the appointment
            Appointment appointment = appointmentRepository.findByIdWithAllRelationships(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Validate current status - can only complete appointments that are CONFIRMED, REQUESTED, or RESCHEDULED
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED &&
                    appointment.getStatus() != AppointmentStatus.REQUESTED &&
                    appointment.getStatus() != AppointmentStatus.RESCHEDULED) {
                logger.warn("Cannot complete appointment with status: {} [appointmentId={}]",
                        appointment.getStatus(), appointmentId);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Appointment must be CONFIRMED, REQUESTED, or RESCHEDULED to be marked as complete. Current status: " +
                                appointment.getStatus());
            }

            // Validate that appointment date is not in the future
//            if (appointment.getAppointmentDate().isAfter(LocalDateTime.now())) {
//                logger.warn("Cannot complete future appointment [appointmentId={}, appointmentDate={}]",
//                           appointmentId, appointment.getAppointmentDate());
//                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
//                        "Cannot mark future appointments as complete. Appointment date: " +
//                        appointment.getAppointmentDate().toLocalDate());
//            }

            // Update appointment status to COMPLETED
            appointment.setStatus(AppointmentStatus.COMPLETED);
            appointment.setCompletedAt(LocalDateTime.now());

            // Add consultation notes if provided
            if (consultationNotes != null && !consultationNotes.trim().isEmpty()) {
                appointment.setConsultationNotes(consultationNotes);
            }

            // Validate required fields before saving
            if (appointment.getPatient() == null) {
                throw new IllegalArgumentException("Appointment patient cannot be null");
            }
            if (appointment.getDoctor() == null) {
                throw new IllegalArgumentException("Appointment doctor cannot be null");
            }
            if (appointment.getAppointmentDate() == null) {
                throw new IllegalArgumentException("Appointment date cannot be null");
            }
            if (appointment.getReason() == null || appointment.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Appointment reason cannot be null or empty");
            }

            // Log the state before saving
            logger.info("About to save completed appointment [id={}, status={}, patientId={}, doctorId={}, completedAt={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId(),
                    appointment.getCompletedAt());

            Appointment completedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment marked as complete successfully [id={}]", completedAppointment.getId());

            // Send completion notification to patient
            appointmentNotificationService.sendCompletionNotification(completedAppointment);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointmentId", completedAppointment.getId());
            responseData.put("appointmentNumber", completedAppointment.getAppointmentNumber());
            responseData.put("doctorId", completedAppointment.getDoctor().getId());
            responseData.put("patientId", completedAppointment.getPatient().getId());
            responseData.put("appointmentDate", completedAppointment.getAppointmentDate());
            responseData.put("scheduledTime", completedAppointment.getScheduledTime());
            responseData.put("status", completedAppointment.getStatus());
            responseData.put("completedAt", completedAppointment.getCompletedAt());
            responseData.put("consultationNotes", completedAppointment.getConsultationNotes());
            responseData.put("reason", completedAppointment.getReason());

            return baseResponse.successResponse("Appointment marked as complete successfully", responseData);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for marking appointment as complete [appointmentId={}] | {}", appointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error during marking appointment as complete [appointmentId={}]", appointmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to mark appointment as complete at the moment: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> rescheduleAppointmentWithSlot(Long appointmentId, AppointmentWithSlotDTO appointmentWithSlotDTO) {
        logger.info(
                "Appointment rescheduling with slot validation request received [appointmentId={}, doctorId={}, date={}, time={}]",
                appointmentId,
                appointmentWithSlotDTO.getDoctorId(),
                appointmentWithSlotDTO.getAppointmentDate(),
                appointmentWithSlotDTO.getAppointmentTime()
        );

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            if (appointmentWithSlotDTO.getDoctorId() != null) {
                if (!doctorRepository.existsById(appointmentWithSlotDTO.getDoctorId())) {
                    logger.warn("Doctor not found [doctorId={}]", appointmentWithSlotDTO.getDoctorId());
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Selected doctor does not exist");
                }
            }
            if (appointmentWithSlotDTO.getPatientId() != null) {
                if (!customerRepository.existsById(appointmentWithSlotDTO.getPatientId())) {
                    logger.warn("Patient not found [patientId={}]", appointmentWithSlotDTO.getPatientId());
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Selected patient does not exist");
                }
            }
            if (appointmentWithSlotDTO.getAppointmentDate() != null && appointmentWithSlotDTO.getAppointmentTime() != null) {
                LocalDateTime newAppointmentDateTime = LocalDateTime.of(
                        appointmentWithSlotDTO.getAppointmentDate(),
                        appointmentWithSlotDTO.getAppointmentTime()
                );

                if (!newAppointmentDateTime.equals(appointment.getAppointmentDate())) {
                    Long doctorId = appointmentWithSlotDTO.getDoctorId() != null ?
                            appointmentWithSlotDTO.getDoctorId() : appointment.getDoctor().getId();
                    Long patientId = appointmentWithSlotDTO.getPatientId() != null ?
                            appointmentWithSlotDTO.getPatientId() : appointment.getPatient().getId();

                    boolean appointmentExists = appointmentRepository
                            .existsByDoctorIdAndPatientIdAndAppointmentDate(
                                    doctorId,
                                    patientId,
                                    newAppointmentDateTime
                            );

                    if (appointmentExists) {
                        logger.warn("Duplicate appointment attempt [doctorId={}, patientId={}, appointmentDateTime={}]",
                                doctorId,
                                patientId,
                                newAppointmentDateTime);
                        return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                                "You already have an appointment with this doctor for the selected date and time");
                    }
                }
            }

            if (appointmentWithSlotDTO.getAppointmentDate() != null && appointmentWithSlotDTO.getAppointmentTime() != null) {
                Long doctorId = appointmentWithSlotDTO.getDoctorId() != null ?
                        appointmentWithSlotDTO.getDoctorId() : appointment.getDoctor().getId();

                ResponseEntity<?> timeSlotValidation = validateAppointmentTimeSlot(
                        doctorId,
                        appointmentWithSlotDTO.getAppointmentDate(),
                        appointmentWithSlotDTO.getAppointmentTime()
                );

                if (!timeSlotValidation.getStatusCode().is2xxSuccessful()) {
                    return timeSlotValidation;
                }

                // Handle the new Map response format
                Object responseBody = timeSlotValidation.getBody();
                Boolean isAvailable = null;
                String message = null;
                String reason = null;

                if (responseBody instanceof Map) {
                    @SuppressWarnings("unchecked")
                    Map<String, Object> validationResponse = (Map<String, Object>) responseBody;
                    isAvailable = (Boolean) validationResponse.get("available");
                    message = (String) validationResponse.get("message");
                    reason = (String) validationResponse.get("reason");
                } else {
                    logger.error("Unexpected response type from validateAppointmentTimeSlot: {}",
                            responseBody != null ? responseBody.getClass().getName() : "null");
                    return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                            "Failed to validate time slot availability");
                }

                if (Boolean.FALSE.equals(isAvailable)) {
                    logger.warn("Requested time slot is not available [doctorId={}, date={}, time={}, reason={}]",
                            doctorId,
                            appointmentWithSlotDTO.getAppointmentDate(),
                            appointmentWithSlotDTO.getAppointmentTime(),
                            reason != null ? reason : "UNKNOWN");

                    String errorMessage = message != null ? message : "The requested time slot is not available. Please select another time.";
                    HttpStatus statusCode = "ALREADY_BOOKED".equals(reason) ? HttpStatus.CONFLICT : HttpStatus.BAD_REQUEST;

                    return baseResponse.errorResponse(statusCode, errorMessage);
                }
            }

            boolean isUpdated = false;
            LocalDateTime originalDateTime = appointment.getAppointmentDate(); // Capture original time for rescheduledFrom

            if (appointmentWithSlotDTO.getDoctorId() != null) {
                Doctor doctor = doctorRepository.findById(appointmentWithSlotDTO.getDoctorId())
                        .orElseThrow(() -> new IllegalArgumentException("Doctor not found"));
                appointment.setDoctor(doctor);
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getPatientId() != null) {
                Customer patient = customerRepository.findById(appointmentWithSlotDTO.getPatientId())
                        .orElseThrow(() -> new IllegalArgumentException("Patient not found"));
                appointment.setPatient(patient);
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getAppointmentDate() != null && appointmentWithSlotDTO.getAppointmentTime() != null) {
                LocalDateTime newDateTime = LocalDateTime.of(
                        appointmentWithSlotDTO.getAppointmentDate(),
                        appointmentWithSlotDTO.getAppointmentTime()
                );
                appointment.setAppointmentDate(newDateTime);
                appointment.setScheduledTime(newDateTime);
                isUpdated = true;
            } else if (appointmentWithSlotDTO.getAppointmentDate() != null) {
                // Update only date, keep existing time
                LocalDateTime newDateTime = LocalDateTime.of(
                        appointmentWithSlotDTO.getAppointmentDate(),
                        appointment.getAppointmentDate().toLocalTime()
                );
                appointment.setAppointmentDate(newDateTime);
                appointment.setScheduledTime(newDateTime);
                isUpdated = true;
            } else if (appointmentWithSlotDTO.getAppointmentTime() != null) {
                // Update only time, keep existing date
                LocalDateTime newDateTime = LocalDateTime.of(
                        appointment.getAppointmentDate().toLocalDate(),
                        appointmentWithSlotDTO.getAppointmentTime()
                );
                appointment.setScheduledTime(newDateTime);
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getReason() != null) {
                appointment.setReason(appointmentWithSlotDTO.getReason());
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getNotes() != null) {
                appointment.setNotes(appointmentWithSlotDTO.getNotes());
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getConsultationNotes() != null) {
                appointment.setConsultationNotes(appointmentWithSlotDTO.getConsultationNotes());
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getIsEmergency() != null) {
                appointment.setIsEmergency(appointmentWithSlotDTO.getIsEmergency());
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getPriority() != null) {
                appointment.setPriority(appointmentWithSlotDTO.getPriority());
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getFollowUpRequired() != null) {
                appointment.setFollowUpRequired(appointmentWithSlotDTO.getFollowUpRequired());
                isUpdated = true;
            }

            if (appointmentWithSlotDTO.getStatus() != null) {
                appointment.setStatus(appointmentWithSlotDTO.getStatus());
                if (appointmentWithSlotDTO.getStatus() == AppointmentStatus.COMPLETED) {
                    appointment.setCompletedAt(LocalDateTime.now());
                }
                isUpdated = true;
            }

            // If changes were made and no explicit completed/cancelled status was set, set to RESCHEDULED
            if (isUpdated && appointment.getStatus() != AppointmentStatus.COMPLETED &&
                    appointment.getStatus() != AppointmentStatus.CANCELLED) {
                appointment.setStatus(AppointmentStatus.RESCHEDULED);
                // Set rescheduled tracking fields
                appointment.setRescheduledFrom(originalDateTime);
                appointment.setRescheduledTo(appointment.getAppointmentDate());
            }

            if (!isUpdated) {
                logger.warn("No fields provided for rescheduling [appointmentId={}]", appointmentId);
                return baseResponse.errorResponse(
                        HttpStatus.BAD_REQUEST,
                        "No fields provided for rescheduling"
                );
            }

            // Log the state before saving
            logger.info("About to save rescheduled appointment [id={}, status={}, patientId={}, doctorId={}, newDateTime={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId(),
                    appointment.getAppointmentDate());

            Appointment updatedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment rescheduled successfully [id={}]", updatedAppointment.getId());

            // Send reschedule notification to patient if status is RESCHEDULED
            if (updatedAppointment.getStatus() == AppointmentStatus.RESCHEDULED) {
                appointmentNotificationService.sendRescheduleNotification(updatedAppointment);
            }

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointmentId", updatedAppointment.getId());
            responseData.put("appointmentNumber", updatedAppointment.getAppointmentNumber());
            responseData.put("doctorId", updatedAppointment.getDoctor().getId());
            responseData.put("patientId", updatedAppointment.getPatient().getId());
            responseData.put("appointmentDate", updatedAppointment.getAppointmentDate());
            responseData.put("scheduledTime", updatedAppointment.getScheduledTime());
            responseData.put("status", updatedAppointment.getStatus());
            responseData.put("reason", updatedAppointment.getReason());
            responseData.put("isEmergency", updatedAppointment.getIsEmergency());
            responseData.put("priority", updatedAppointment.getPriority());

            return baseResponse.successResponse("Appointment Reschedule Successfully",  responseData);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for rescheduling [appointmentId={}] | {}", appointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error during rescheduling for appointmentId={}", appointmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to reschedule appointment at the moment: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> scheduleFollowUpAppointment(Long originalAppointmentId, AppointmentDTO followUpDTO) {
        logger.info("Scheduling follow-up appointment request received [originalAppointmentId={}]", originalAppointmentId);

        try {
            // Verify the original appointment exists
            Appointment originalAppointment = appointmentRepository.findById(originalAppointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Original appointment not found"));

            // Validate that follow-up is required for the original appointment
            if (!Boolean.TRUE.equals(originalAppointment.getFollowUpRequired())) {
                logger.warn("Cannot schedule follow-up: Original appointment does not require follow-up [appointmentId={}]", originalAppointmentId);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Original appointment must have followUpRequired set to true");
            }

            // Validate doctor exists
            if (!userRepository.existsById(followUpDTO.getDoctorId())) {
                logger.warn("Doctor not found [doctorId={}]", followUpDTO.getDoctorId());
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Selected doctor does not exist");
            }

            // Validate patient exists
            if (!userRepository.existsById(followUpDTO.getPatientId())) {
                logger.warn("Patient not found [patientId={}]", followUpDTO.getPatientId());
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Selected patient does not exist");
            }

            // Check for duplicate appointments
            boolean appointmentExists = appointmentRepository
                    .existsByDoctorIdAndPatientIdAndAppointmentDate(
                            followUpDTO.getDoctorId(),
                            followUpDTO.getPatientId(),
                            followUpDTO.getAppointmentDate()
                    );

            if (appointmentExists) {
                logger.warn("Duplicate follow-up appointment attempt [doctorId={}, patientId={}, appointmentDate={}]",
                        followUpDTO.getDoctorId(),
                        followUpDTO.getPatientId(),
                        followUpDTO.getAppointmentDate());
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "You already have an appointment with this doctor for the selected date");
            }

            // Validate that the requested time slot is available according to the doctor's schedule
            LocalDate appointmentDate = followUpDTO.getAppointmentDate().toLocalDate();
            LocalTime appointmentTime = followUpDTO.getScheduledTime().toLocalTime();

            ResponseEntity<?> timeSlotValidation = validateAppointmentTimeSlot(
                    followUpDTO.getDoctorId(), appointmentDate, appointmentTime);

            if (timeSlotValidation.getStatusCode().is2xxSuccessful() && !(Boolean) timeSlotValidation.getBody()) {
                logger.warn("Requested time slot is not available for follow-up [doctorId={}, date={}, time={}]",
                        followUpDTO.getDoctorId(), appointmentDate, appointmentTime);
                return baseResponse.errorResponse(HttpStatus.CONFLICT,
                        "The requested time slot is not available for follow-up. Please select another time.");
            }

            // Create the follow-up appointment
            Appointment followUpAppointment = new Appointment();
            Doctor doctor = doctorRepository.findById(followUpDTO.getDoctorId()).orElse(null);
            Customer patient = customerRepository.findById(followUpDTO.getPatientId()).orElse(null);

            if (doctor == null || patient == null) {
                return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                        "Failed to retrieve doctor or patient information");
            }

            followUpAppointment.setDoctor(doctor);
            followUpAppointment.setPatient(patient);
            followUpAppointment.setStatus(AppointmentStatus.REQUESTED);
            followUpAppointment.setAppointmentDate(followUpDTO.getAppointmentDate());
            followUpAppointment.setScheduledTime(followUpDTO.getScheduledTime());
            followUpAppointment.setReason("Follow-up appointment" + (followUpDTO.getReason() != null ? ": " + followUpDTO.getReason() : ""));
            followUpAppointment.setNotes(followUpDTO.getNotes() != null ? followUpDTO.getNotes() : "Follow-up from appointment #" + originalAppointmentId);
            followUpAppointment.setIsEmergency(followUpDTO.getIsEmergency() != null ? followUpDTO.getIsEmergency() : false);
            followUpAppointment.setPriority(followUpDTO.getPriority() != null ? followUpDTO.getPriority() : "NORMAL");
            followUpAppointment.setFollowUpRequired(false); // Follow-ups don't need follow-ups
            followUpAppointment.setFollowUpDate(null); // No nested follow-ups

            // Set company from patient if available
            if (patient.getCompany() != null) {
                followUpAppointment.setCompany(patient.getCompany());
            }

            // Generate appointment number
            String appointmentNumber;
            Long apptCompanyId = followUpAppointment.getCompany() != null ? followUpAppointment.getCompany().getId() : null;
            Long apptBranchId = null;

            if (apptCompanyId != null) {
                appointmentNumber = appointmentNumberService.generateAppointmentNumber(apptCompanyId, apptBranchId);
            } else {
                Long patientCompanyId = patient.getCompany() != null ? patient.getCompany().getId() : null;
                Long patientBranchId = patient.getBranch() != null ? patient.getBranch().getId() : null;

                if (patientBranchId != null || patientCompanyId != null) {
                    appointmentNumber = appointmentNumberService.generateAppointmentNumber(patientCompanyId, patientBranchId);
                } else {
                    appointmentNumber = appointmentNumberService.generateAppointmentNumberForDoctor(doctor.getId());
                }
            }

            followUpAppointment.setAppointmentNumber(appointmentNumber);

            Appointment savedFollowUp = appointmentRepository.save(followUpAppointment);

            logger.info("Follow-up appointment created successfully [followUpId={}, originalId={}, doctorId={}, patientId={}, appointmentNumber={}]",
                    savedFollowUp.getId(),
                    originalAppointmentId,
                    followUpDTO.getDoctorId(),
                    followUpDTO.getPatientId(),
                    savedFollowUp.getAppointmentNumber());

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("followUpAppointmentId", savedFollowUp.getId());
            responseData.put("followUpAppointmentNumber", savedFollowUp.getAppointmentNumber());
            responseData.put("originalAppointmentId", originalAppointmentId);
            responseData.put("doctorId", savedFollowUp.getDoctor().getId());
            responseData.put("patientId", savedFollowUp.getPatient().getId());
            responseData.put("appointmentDate", savedFollowUp.getAppointmentDate());
            responseData.put("scheduledTime", savedFollowUp.getScheduledTime());
            responseData.put("status", savedFollowUp.getStatus());
            responseData.put("reason", savedFollowUp.getReason());
            responseData.put("isEmergency", savedFollowUp.getIsEmergency());
            responseData.put("priority", savedFollowUp.getPriority());

            return baseResponse.successResponse("Follow-up appointment scheduled successfully", responseData);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for follow-up scheduling [originalAppointmentId={}] | {}", originalAppointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Unexpected error while scheduling follow-up appointment [originalAppointmentId={}]", originalAppointmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong while scheduling the follow-up appointment. Please try again later.");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getFollowUpAppointments(Long originalAppointmentId) {
        logger.info("Fetching follow-up appointments request received [originalAppointmentId={}]", originalAppointmentId);

        try {
            // Verify the original appointment exists
            Appointment originalAppointment = appointmentRepository.findById(originalAppointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Original appointment not found"));

            // Find all appointments that reference this original appointment in their notes
            // This is a simple approach - in production you might want a proper foreign key relationship
            String searchPattern = "%" + originalAppointmentId + "%";
            List<Appointment> potentialFollowUps = appointmentRepository
                    .findByNotesContainingAndReasonContaining(searchPattern, "Follow-up");

            // Filter to only include actual follow-ups (where notes contain the original appointment reference)
            List<Appointment> actualFollowUps = potentialFollowUps.stream()
                    .filter(appointment -> appointment.getNotes() != null &&
                            appointment.getNotes().contains("appointment #" + originalAppointmentId))
                    .toList();

            List<AppointmentDTO> followUpDTOs = actualFollowUps.stream()
                    .map(this::convertToDTO)
                    .toList();

            logger.info("Found {} follow-up appointments for original appointment [id={}]",
                    followUpDTOs.size(), originalAppointmentId);

            Map<String, Object> response = new HashMap<>();
            response.put("originalAppointmentId", originalAppointmentId);
            response.put("originalAppointmentNumber", originalAppointment.getAppointmentNumber());
            response.put("followUpCount", followUpDTOs.size());
            response.put("followUpAppointments", followUpDTOs);

            return baseResponse.successResponse(
                    "Follow-up appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Original appointment not found [appointmentId={}] | {}", originalAppointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());

        } catch (Exception e) {
            logger.error("Error while fetching follow-up appointments [originalAppointmentId={}]", originalAppointmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch follow-up appointments at the moment");
        }
    }

    @Override
    @Transactional
    public ResponseEntity<?> markAsFollowUpRequired(Long appointmentId, LocalDateTime followUpDate) {
        logger.info("Mark as follow-up required request received [appointmentId={}, followUpDate={}]",
                appointmentId, followUpDate);

        try {
            Appointment appointment = appointmentRepository.findById(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Validate that followUpDate is in the future
            if (followUpDate.isBefore(LocalDateTime.now())) {
                logger.warn("Cannot set past follow-up date [appointmentId={}, followUpDate={}]",
                        appointmentId, followUpDate);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Follow-up date must be in the future");
            }

            // Update the appointment
            appointment.setFollowUpRequired(true);
            appointment.setFollowUpDate(followUpDate);
            appointment.setUpdatedAt(new Date());

            // Log the state before saving
            logger.info("About to save appointment with follow-up required [id={}, followUpRequired={}, followUpDate={}]",
                    appointment.getId(), appointment.getFollowUpRequired(), appointment.getFollowUpDate());

            Appointment updatedAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment marked as follow-up required successfully [id={}]", updatedAppointment.getId());

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointmentId", updatedAppointment.getId());
            responseData.put("appointmentNumber", updatedAppointment.getAppointmentNumber());
            responseData.put("followUpRequired", updatedAppointment.getFollowUpRequired());
            responseData.put("followUpDate", updatedAppointment.getFollowUpDate());
            responseData.put("doctorId", updatedAppointment.getDoctor().getId());
            responseData.put("patientId", updatedAppointment.getPatient().getId());
            responseData.put("status", updatedAppointment.getStatus());

            return baseResponse.successResponse("Appointment marked as follow-up required successfully", responseData);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for marking follow-up required [appointmentId={}] | {}", appointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error during marking appointment as follow-up required [appointmentId={}]", appointmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to mark appointment as follow-up required at the moment: " + e.getMessage()
            );
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAppointmentsForCalendar(Long doctorId, Long companyId, Long branchId) {
        logger.info("Fetching appointments for calendar view [doctorId={}, companyId={}, branchId={}]",
                doctorId, companyId, branchId);
        
        try {
            List<Appointment> appointments = appointmentRepository.findByDoctorIdAndCompanyIdAndBranchId(
                doctorId, companyId, branchId);
            
            // Convert appointments to calendar DTOs
            List<AppointmentCalendarDTO> calendarDTOs = appointments.stream()
                    .map(this::convertToCalendarDTO)
                    .collect(Collectors.toList());
            
            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointments", calendarDTOs);
            responseData.put("totalAppointments", calendarDTOs.size());
            responseData.put("filtersApplied", Map.of(
                "doctorId", doctorId,
                "companyId", companyId,
                "branchId", branchId
            ));
            
            logger.info("Retrieved {} appointments for calendar view", calendarDTOs.size());
            
            return baseResponse.successResponse(
                    "Appointments retrieved successfully for calendar view",
                    responseData
            );
            
        } catch (Exception e) {
            logger.error("Error retrieving appointments for calendar view", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve appointments for calendar view: " + e.getMessage()
            );
        }
    }
    
    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getAppointmentsForCalendarWithDateRange(Long doctorId, Long companyId, Long branchId, 
            LocalDateTime startDate, LocalDateTime endDate) {
        logger.info("Fetching appointments for calendar view with date range [doctorId={}, companyId={}, branchId={}, startDate={}, endDate={}]",
                doctorId, companyId, branchId, startDate, endDate);
        
        try {
            List<Appointment> appointments;
            
            if (startDate != null && endDate != null) {
                // Filter by date range
                appointments = appointmentRepository.findByDoctorIdAndCompanyIdAndBranchIdAndDateRange(
                    doctorId, companyId, branchId, startDate, endDate);
            } else {
                // Use existing method without date range
                appointments = appointmentRepository.findByDoctorIdAndCompanyIdAndBranchId(
                    doctorId, companyId, branchId);
            }
            
            // Convert appointments to calendar DTOs
            List<AppointmentCalendarDTO> calendarDTOs = appointments.stream()
                    .map(this::convertToCalendarDTO)
                    .collect(Collectors.toList());
            
            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointments", calendarDTOs);
            responseData.put("totalAppointments", calendarDTOs.size());
            responseData.put("dateRange", Map.of(
                "startDate", startDate,
                "endDate", endDate
            ));
            responseData.put("filtersApplied", Map.of(
                "doctorId", doctorId,
                "companyId", companyId,
                "branchId", branchId
            ));
            
            logger.info("Retrieved {} appointments for calendar view with date range", calendarDTOs.size());
            
            return baseResponse.successResponse(
                    "Appointments retrieved successfully for calendar view with date range",
                    responseData
            );
            
        } catch (Exception e) {
            logger.error("Error retrieving appointments for calendar view with date range", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to retrieve appointments for calendar view with date range: " + e.getMessage()
            );
        }
    }
    
    @Override
    @Transactional
    public ResponseEntity<?> markAppointmentAsNoShow(Long appointmentId, String noShowReason) {
        logger.info("Mark appointment as no-show request received [appointmentId={}]", appointmentId);

        try {
            // Fetch the appointment
            Appointment appointment = appointmentRepository.findByIdWithAllRelationships(appointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            // Validate current status - can only mark as no-show appointments that are CONFIRMED or REQUESTED
            if (appointment.getStatus() != AppointmentStatus.CONFIRMED && 
                appointment.getStatus() != AppointmentStatus.REQUESTED) {
                logger.warn("Cannot mark appointment as no-show with status: {} [appointmentId={}]",
                        appointment.getStatus(), appointmentId);
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Appointment must be CONFIRMED or REQUESTED to be marked as no-show. Current status: " +
                                appointment.getStatus());
            }

            // Validate that appointment date is in the past (can't mark future appointments as no-show)
            if (appointment.getAppointmentDate().isAfter(LocalDateTime.now())) {
                logger.warn("Cannot mark future appointment as no-show [appointmentId={}, appointmentDate={}]",
                           appointmentId, appointment.getAppointmentDate());
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST,
                        "Cannot mark future appointments as no-show. Appointment date: " +
                        appointment.getAppointmentDate().toLocalDate());
            }

            User currentUser = DbUtill.getCurrentUser();
            
            // Update appointment status to NO_SHOW
            appointment.setStatus(AppointmentStatus.NO_SHOW);
            appointment.setNoShowReason(noShowReason);
            appointment.setNoShowRecordedAt(LocalDateTime.now());
            appointment.setNoShowRecordedBy(currentUser.getId());

            // Validate required fields before saving
            if (appointment.getPatient() == null) {
                throw new IllegalArgumentException("Appointment patient cannot be null");
            }
            if (appointment.getDoctor() == null) {
                throw new IllegalArgumentException("Appointment doctor cannot be null");
            }
            if (appointment.getAppointmentDate() == null) {
                throw new IllegalArgumentException("Appointment date cannot be null");
            }
            if (appointment.getReason() == null || appointment.getReason().trim().isEmpty()) {
                throw new IllegalArgumentException("Appointment reason cannot be null or empty");
            }

            // Log the state before saving
            logger.info("About to save no-show appointment [id={}, status={}, patientId={}, doctorId={}, noShowReason={}]",
                    appointment.getId(), appointment.getStatus(),
                    appointment.getPatient().getId(), appointment.getDoctor().getId(),
                    noShowReason);

            Appointment noShowAppointment = appointmentRepository.save(appointment);
            logger.info("Appointment marked as no-show successfully [id={}]", noShowAppointment.getId());

            // Send no-show notification to patient
            appointmentNotificationService.sendNoShowNotification(noShowAppointment);

            // Prepare response data
            Map<String, Object> responseData = new HashMap<>();
            responseData.put("appointmentId", noShowAppointment.getId());
            responseData.put("appointmentNumber", noShowAppointment.getAppointmentNumber());
            responseData.put("doctorId", noShowAppointment.getDoctor().getId());
            responseData.put("patientId", noShowAppointment.getPatient().getId());
            responseData.put("appointmentDate", noShowAppointment.getAppointmentDate());
            responseData.put("scheduledTime", noShowAppointment.getScheduledTime());
            responseData.put("status", noShowAppointment.getStatus());
            responseData.put("noShowReason", noShowAppointment.getNoShowReason());
            responseData.put("noShowRecordedAt", noShowAppointment.getNoShowRecordedAt());
            responseData.put("noShowRecordedBy", noShowAppointment.getNoShowRecordedBy());
            responseData.put("reason", noShowAppointment.getReason());

            return baseResponse.successResponse("Appointment marked as no-show successfully", responseData);

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed for marking appointment as no-show [appointmentId={}] | {}", appointmentId, e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error during marking appointment as no-show [appointmentId={}]", appointmentId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to mark appointment as no-show at the moment: " + e.getMessage()
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getNoShowAppointments(Integer pageNo, Integer pageSize) {
        logger.info("Fetching no-show appointments [pageNo={}, pageSize={}]", pageNo, pageSize);

        try {
            User currentUser = DbUtill.getCurrentUser();
            
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            Page<Appointment> page;
            
            // Determine if user has access to all no-show appointments or company-specific no-show appointments
            if (currentUser.getRole() == UserRole.SUPER_ADMIN ||
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER ||
                currentUser.getRole() == UserRole.SAAS_ADMIN ||
                currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                // High-level admins can see all no-show appointments
                page = appointmentRepository.findByStatusWithAllRelationships(AppointmentStatus.NO_SHOW, pageRequest);
            } else {
                // Other users can only see no-show appointments related to their company
                if (currentUser.getCompany() != null) {
                    // Fetch no-show appointments by company
                    page = appointmentRepository.findByCompanyIdAndStatusWithAllRelationships(
                            currentUser.getCompany().getId(), AppointmentStatus.NO_SHOW, pageRequest);
                } else if (currentUser.getBranch() != null) {
                    // If no company but branch exists, fetch by branch
                    page = appointmentRepository.findByBranchIdAndStatusWithAllRelationships(
                            currentUser.getBranch().getId(), AppointmentStatus.NO_SHOW, pageRequest);
                } else {
                    // No company or branch association, return empty page
                    page = Page.empty(pageRequest);
                }
            }

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info("Fetched {} no-show appointments on page {}", page.getNumberOfElements(), page.getNumber());

            return baseResponse.successResponse(
                    "No-show appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(
                    HttpStatus.BAD_REQUEST,
                    e.getMessage()
            );

        } catch (Exception e) {
            logger.error("Error while fetching no-show appointments", e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch no-show appointments at the moment. Please try again later."
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getNoShowAppointmentsByDoctor(Long doctorId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching no-show appointments for doctor [doctorId={}, pageNo={}, pageSize={}]", 
                doctorId, pageNo, pageSize);

        try {
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            Page<Appointment> page = appointmentRepository.findByDoctorIdAndStatusWithAllRelationships(
                    doctorId, AppointmentStatus.NO_SHOW, pageRequest);

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Doctor no-show appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching no-show appointments for doctorId={}", doctorId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch no-show appointments"
            );
        }
    }

    @Override
    @Transactional(readOnly = true)
    public ResponseEntity<?> getNoShowAppointmentsByPatient(Long patientId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching no-show appointments for patient [patientId={}, pageNo={}, pageSize={}]", 
                patientId, pageNo, pageSize);

        try {
            // Use unsorted Pageable since the query already has ORDER BY a.createdAt DESC
            PageRequest pageRequest = PageRequest.of(
                    pageNo != null ? pageNo : 0,
                    pageSize != null ? pageSize : 10
            );

            Page<Appointment> page = appointmentRepository.findByPatientIdAndStatusWithAllRelationships(
                    patientId, AppointmentStatus.NO_SHOW, pageRequest);

            List<AppointmentDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Patient no-show appointments fetched successfully",
                    response
            );

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());

        } catch (Exception e) {
            logger.error("Error fetching no-show appointments for patientId={}", patientId, e);
            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch no-show appointments"
            );
        }
    }
}
