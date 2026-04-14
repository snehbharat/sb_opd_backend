package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.enums.UserRole;
import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.dto.BulkScheduleDTO;
import com.sbpl.OPD.dto.ScheduleDTO;
import com.sbpl.OPD.dto.WeekTemplateDTO;
import com.sbpl.OPD.enums.ScheduleStatus;
import com.sbpl.OPD.enums.ScheduleType;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Branch;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.model.Schedule;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.BranchRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.repository.ScheduleRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.ScheduleService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Implementation of schedule service operations.
 *
 * Contains core logic for managing doctor schedules,
 * checking availability, and validating appointment timing
 * against schedule constraints.
 *
 * @author Rahul Kumar
 */
@Service
@Transactional
public class ScheduleServiceImpl implements ScheduleService {

    @Autowired
    private ScheduleRepository scheduleRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;
    
    @Autowired
    private BranchRepository branchRepository;
    
    @Autowired
    private BaseResponse baseResponse;

    private static final Logger logger = LoggerFactory.getLogger(ScheduleServiceImpl.class);

    @Override
    public ResponseEntity<?> createSchedule(ScheduleDTO scheduleDTO) {
        try {
            logger.info("Creating schedule for doctorId: {}, dayOfWeek: {}", 
                       scheduleDTO.getDoctorId(), scheduleDTO.getDayOfWeek());

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = validateDoctorAndBranchAccess(scheduleDTO.getDoctorId(), currentUser);
            
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to create schedule for this doctor");
            }

            List<Schedule> existingSchedules = scheduleRepository
                    .findByDoctorIdAndDayOfWeek(scheduleDTO.getDoctorId(), scheduleDTO.getDayOfWeek());
            
            if (!existingSchedules.isEmpty() && scheduleDTO.getStartDate() != null && scheduleDTO.getEndDate() != null) {
                for (Schedule existing : existingSchedules) {
                    if (isOverlapping(existing, scheduleDTO)) {
                        return baseResponse.errorResponse(HttpStatus.CONFLICT,
                                "Schedule overlaps with existing schedule for doctor on the same day");
                    }
                }
            }

            Schedule schedule = new Schedule();
            BeanUtils.copyProperties(scheduleDTO, schedule);
            schedule.setDoctor(doctor);
            if (currentUser.getRole() == UserRole.SAAS_ADMIN ||
                currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER || 
                currentUser.getRole() == UserRole.SUPER_ADMIN || 
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                if (scheduleDTO.getBranchId() != null) {
                    Branch branch = branchRepository.findById(scheduleDTO.getBranchId())
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + scheduleDTO.getBranchId()));
                    // Additional validation to ensure the user has access to this branch
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to assign schedule to this branch");
                    }
                }
            } else {
                if (currentUser.getBranch() != null) {
                    scheduleDTO.setBranchId(currentUser.getBranch().getId());
                }
            }

            Schedule savedSchedule = scheduleRepository.save(schedule);
            savedSchedule.setId(savedSchedule.getId()); // Ensure ID is set

            logger.info("Schedule created successfully [scheduleId={}]", savedSchedule.getId());
            return baseResponse.successResponse("Schedule created successfully", convertToDTO(savedSchedule));

        } catch (Exception e) {
            logger.error("Error creating schedule", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create schedule: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateSchedule(Long id, ScheduleDTO scheduleDTO) {
        try {
            logger.info("Updating schedule with id: {}", id);

            Schedule existingSchedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = validateDoctorAndBranchAccess(scheduleDTO.getDoctorId(), currentUser);
            
            // Additional validation: Ensure doctor belongs to user's branch/company context
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to update schedule for this doctor");
            }
            
            // Validate that user can modify this schedule (belongs to their branch/company)
            if (!isScheduleAccessibleToUser(existingSchedule, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to update this schedule");
            }

            List<Schedule> existingSchedules = scheduleRepository
                    .findByDoctorIdAndDayOfWeek(scheduleDTO.getDoctorId(), scheduleDTO.getDayOfWeek());
            
            for (Schedule existing : existingSchedules) {
                if (!existing.getId().equals(id) && isOverlapping(existing, scheduleDTO)) {
                    return baseResponse.errorResponse(HttpStatus.CONFLICT,
                            "Updated schedule overlaps with existing schedule for doctor on the same day");
                }
            }

            BeanUtils.copyProperties(scheduleDTO, existingSchedule, "id");
            existingSchedule.setDoctor(doctor);
            if (currentUser.getRole() == UserRole.SAAS_ADMIN ||
                currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER || 
                currentUser.getRole() == UserRole.SUPER_ADMIN || 
                currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                if (scheduleDTO.getBranchId() != null) {
                    Branch branch = branchRepository.findById(scheduleDTO.getBranchId())
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + scheduleDTO.getBranchId()));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to assign schedule to this branch");
                    }
                }
            } else {
                if (currentUser.getBranch() != null) {
                    scheduleDTO.setBranchId(currentUser.getBranch().getId());
                }
            }

            Schedule updatedSchedule = scheduleRepository.save(existingSchedule);

            logger.info("Schedule updated successfully [scheduleId={}]", updatedSchedule.getId());
            return baseResponse.successResponse("Schedule updated successfully");

        } catch (Exception e) {
            logger.error("Error updating schedule [scheduleId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update schedule: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getScheduleById(Long id) {
        try {
            logger.info("Fetching schedule with id: {}", id);

            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));
            
            // Validate user access to this schedule
            User currentUser = DbUtill.getCurrentUser();
            if (!isScheduleAccessibleToUser(schedule, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access this schedule");
            }

            return baseResponse.successResponse("Schedule fetched successfully",convertToDTO(schedule));
        } catch (Exception e) {
            logger.error("Error fetching schedule [scheduleId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedule: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAllSchedules(Integer pageNo, Integer pageSize, Long branchId) {
        try {
            logger.info("Fetching all schedules [pageNo={}, pageSize={}, branchId={}]", pageNo, pageSize, branchId);

            User currentUser = DbUtill.getCurrentUser();
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            
            Page<Schedule> schedules;
            
            if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                // Super admins can see all schedules
                schedules = scheduleRepository.findAll(pageable);
            }
            else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER){
                // For SAAS admins, if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's schedules");
                    }
                    schedules = scheduleRepository.findByBranchId(branchId, pageable);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    schedules = scheduleRepository.findByCompanyId(companyId, pageable);
                }
            }
            else{
                // For other roles (BRANCH_MANAGER, DOCTOR, etc.)
                if (currentUser.getBranch() == null) {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
                }
                schedules = scheduleRepository.findByBranchId(currentUser.getBranch().getId(), pageable);
            }

            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);

            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching all schedules", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSchedulesByDoctorId(Long doctorId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching schedules for doctorId: {} [pageNo={}, pageSize={}]", 
                       doctorId, pageNo, pageSize);
            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
            
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access schedules for this doctor");
            }

            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules = scheduleRepository.findByDoctorId(doctorId, pageable);

            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);

            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching schedules for doctorId: {}", doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSchedulesByStatus(ScheduleStatus status, Integer pageNo, Integer pageSize, Long branchId) {
        try {
            logger.info("Fetching schedules with status: {} [pageNo={}, pageSize={}, branchId={}]", 
                       status, pageNo, pageSize, branchId);

            User currentUser = DbUtill.getCurrentUser();
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules;
            
            if (currentUser.getRole() == UserRole.SUPER_ADMIN || currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                // Super admins can see all schedules
                schedules = scheduleRepository.findByStatus(status, pageable);
            } else if (currentUser.getRole() == UserRole.SAAS_ADMIN || currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER) {
                // For SAAS admins, if branchId is provided, fetch branch-wise; otherwise company-wise
                if (branchId != null) {
                    // Validate branch access
                    Branch branch = branchRepository.findById(branchId)
                            .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                    if (!isBranchAccessibleToUser(branch, currentUser)) {
                        return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                "You don't have permission to access this branch's schedules");
                    }
                    schedules = scheduleRepository.findByBranchIdAndStatus(branchId, status, pageable);
                } else {
                    // Fetch company-wise
                    Long companyId = DbUtill.getLoggedInCompanyId();
                    schedules = scheduleRepository.findByCompanyIdAndStatus(companyId, status, pageable);
                }
            } else {
                // For other roles (BRANCH_MANAGER, DOCTOR, etc.)
                if (currentUser.getBranch() == null) {
                    return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "User not assigned to any branch");
                }
                schedules = scheduleRepository.findByBranchIdAndStatus(currentUser.getBranch().getId(), status, pageable);
            }

            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);

            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching schedules with status: {}", status, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> deleteSchedule(Long id) {
        try {
            logger.info("Deleting schedule with id: {}", id);

            if (!scheduleRepository.existsById(id)) {
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Schedule not found with id: " + id);
            }

            scheduleRepository.deleteById(id);

            logger.info("Schedule deleted successfully [scheduleId={}]", id);
            return baseResponse.successResponse("Schedule deleted successfully", null);
        } catch (Exception e) {
            logger.error("Error deleting schedule [scheduleId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete schedule: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> updateScheduleStatus(Long id, ScheduleStatus status) {
        try {
            logger.info("Updating schedule status for id: {} to: {}", id, status);

            Schedule schedule = scheduleRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Schedule not found with id: " + id));

            schedule.setStatus(status);
            Schedule updatedSchedule = scheduleRepository.save(schedule);

            logger.info("Schedule status updated successfully [scheduleId={}]", updatedSchedule.getId());
            return baseResponse.successResponse("Schedule status updated successfully", convertToDTO(updatedSchedule));
        } catch (Exception e) {
            logger.error("Error updating schedule status [scheduleId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to update schedule status: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getWeeklySchedule(Long doctorId, DayOfWeek dayOfWeek) {
        try {
            logger.info("Getting weekly schedule for doctorId: {} on day: {}", doctorId, dayOfWeek);

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
            
            // Validate that user can access this doctor
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access schedules for this doctor");
            }

            // Only get schedules that are accessible to the current user
            List<Schedule> schedules = scheduleRepository.findByDoctorIdAndDayOfWeek(doctorId, dayOfWeek);
            
            List<ScheduleDTO> scheduleDTOs = schedules.stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(scheduleDTOs);
        } catch (Exception e) {
            logger.error("Error getting weekly schedule for doctorId: {} on day: {}", doctorId, dayOfWeek, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get weekly schedule: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getActiveSchedulesForDate(Long doctorId, LocalDate date) {
        try {
            logger.info("Getting active schedules for doctorId: {} on date: {}", doctorId, date);

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
            
            // Validate that user can access this doctor
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access schedules for this doctor");
            }

            List<Schedule> schedules = scheduleRepository.findActiveSchedulesByDoctor(doctorId, date, ScheduleStatus.ACTIVE);
            
            List<ScheduleDTO> scheduleDTOs = schedules.stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());

            return ResponseEntity.ok(scheduleDTOs);
        } catch (Exception e) {
            logger.error("Error getting active schedules for doctorId: {} on date: {}", doctorId, date, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get active schedules: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getAvailableSlots(Long doctorId, LocalDate date) {
        try {
            logger.info("Getting available slots for doctorId: {} on date: {}", doctorId, date);

            if (doctorId == null || date == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Doctor ID and date are required");
            }

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
            
            // Validate that user can access this doctor
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access schedules for this doctor");
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<Schedule> schedules = scheduleRepository.findActiveScheduleByDoctorAndDayOfWeek(doctorId, dayOfWeek, date);

            // Filter schedules to only those accessible to the current user
            List<Schedule> accessibleSchedules = schedules.stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .collect(Collectors.toList());

            if (accessibleSchedules.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }

            List<LocalTime> availableSlots = getTimes(accessibleSchedules);

            return ResponseEntity.ok(availableSlots);
        } catch (Exception e) {
            logger.error("Error getting available slots for doctorId: {} on date: {}", doctorId, date, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get available slots: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> checkAvailability(Long doctorId, LocalDate date, LocalTime time) {
        try {
            logger.info("Checking availability for doctorId: {}, date: {}, time: {}", doctorId, date, time);

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
            
            // Validate that user can access this doctor
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to check availability for this doctor");
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<Schedule> schedules = scheduleRepository.findActiveScheduleByDoctorAndDayOfWeek(doctorId, dayOfWeek, date);

            // Filter schedules to only those accessible to the current user
            List<Schedule> accessibleSchedules = schedules.stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .collect(Collectors.toList());

            if (accessibleSchedules.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("available", false);
                response.put("message", "No schedule found for this doctor on the selected date");
                response.put("reason", "NO_SCHEDULE");
                return ResponseEntity.ok(response);
            }

            Schedule schedule = accessibleSchedules.get(0);

            boolean scheduleAvailable = schedule.isAvailable() && 
                                      schedule.getStatus() == ScheduleStatus.ACTIVE &&
                                      schedule.getScheduleType() != ScheduleType.ON_LEAVE &&
                                      schedule.isWithinWorkingHours(time) &&
                                      !schedule.hasBreakAtTime(time) &&
                                      !schedule.isOnLunchBreak(time);

            if (!scheduleAvailable) {
                Map<String, Object> response = new HashMap<>();
                response.put("available", false);
                response.put("message", "This time is outside the doctor's working hours or during break time");
                response.put("reason", "OUT_OF_SCHEDULE");
                return ResponseEntity.ok(response);
            }

            // Check if there's already an appointment at this time
            LocalDateTime slotDateTime = LocalDateTime.of(date, time);
            List<Appointment> existingAppointments = appointmentRepository
                .findByDoctorIdAndScheduledTime(doctorId, slotDateTime);
            
            if (!existingAppointments.isEmpty()) {
                Map<String, Object> response = new HashMap<>();
                response.put("available", false);
                response.put("message", "This time slot is already booked");
                response.put("reason", "ALREADY_BOOKED");
                response.put("existingAppointments", existingAppointments.size());
                return ResponseEntity.ok(response);
            }

            Map<String, Object> response = new HashMap<>();
            response.put("available", true);
            response.put("message", "Time slot is available");
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            logger.error("Error checking availability for doctorId: {}, date: {}, time: {}", doctorId, date, time, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to check availability: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> bulkCreateSchedules(List<ScheduleDTO> scheduleDTOs) {
        try {
            logger.info("Bulk creating {} schedules", scheduleDTOs.size());

            List<Schedule> schedules = new ArrayList<>();
            for (ScheduleDTO dto : scheduleDTOs) {
                User currentUser = DbUtill.getCurrentUser();
                
                if (currentUser.getRole() == UserRole.SAAS_ADMIN || 
                    currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER || 
                    currentUser.getRole() == UserRole.SUPER_ADMIN || 
                    currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                    if (dto.getBranchId() != null) {
                        Branch branch = branchRepository.findById(dto.getBranchId())
                                .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + dto.getBranchId()));
                        if (!isBranchAccessibleToUser(branch, currentUser)) {
                            return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                    "You don't have permission to assign schedule to this branch");
                        }
                    }
                } else {
                    if (currentUser.getBranch() != null) {
                        dto.setBranchId(currentUser.getBranch().getId());
                    }
                }
                
                ResponseEntity<?> result = createSchedule(dto);
                if (result.getStatusCode().is2xxSuccessful()) {
                    schedules.add(scheduleRepository.findById(
                            ((ScheduleDTO)result.getBody()).getId()).orElse(null));
                } else {
                    logger.warn("Failed to create schedule for doctorId: {}, dayOfWeek: {}",
                            dto.getDoctorId(), dto.getDayOfWeek());
                }
            }

            return baseResponse.successResponse("Bulk schedule creation completed",
                    schedules.stream().map(this::convertToDTO).collect(Collectors.toList()));
        } catch (Exception e) {
            logger.error("Error in bulk schedule creation", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to bulk create schedules: " + e.getMessage());
        }
    }
    
    private static List<LocalTime> getTimes(List<Schedule> schedules) {
        List<LocalTime> availableSlots = new ArrayList<>();
        Schedule schedule = schedules.get(0);

        if (schedule != null && schedule.isAvailable() && schedule.getStatus() == ScheduleStatus.ACTIVE &&
            schedule.getScheduleType() != ScheduleType.ON_LEAVE) {

            LocalTime startTime = schedule.getStartTime();
            LocalTime endTime = schedule.getEndTime();
            int duration = schedule.getAppointmentDurationMinutes() != null ? schedule.getAppointmentDurationMinutes() : 30;

            if (startTime != null && endTime != null) {
                LocalTime currentTime = startTime;

                while (!currentTime.isAfter(endTime.minusMinutes(duration))) {
                    if (schedule.isWithinWorkingHours(currentTime) &&
                        !schedule.hasBreakAtTime(currentTime) &&
                        !schedule.isOnLunchBreak(currentTime)) {

                        availableSlots.add(currentTime);
                    }
                    currentTime = currentTime.plusMinutes(duration);
                }
            }
        }
        return availableSlots;
    }

    @Override
    public ResponseEntity<?> isTimeSlotAvailable(Long doctorId, LocalDate date, LocalTime time) {
        try {
            logger.info("Checking time slot availability for doctorId: {}, date: {}, time: {}", 
                       doctorId, date, time);

            return checkAvailability(doctorId, date, time);
        } catch (Exception e) {
            logger.error("Error checking time slot availability for doctorId: {}, date: {}, time: {}", 
                        doctorId, date, time, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to check time slot availability: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<?> getAllSlotsWithAvailability(Long doctorId, LocalDate date) {
        try {
            logger.info("Getting all slots with availability for doctorId: {} on date: {}", doctorId, date);

            if (doctorId == null || date == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Doctor ID and date are required");
            }

            User currentUser = DbUtill.getCurrentUser();
            Doctor doctor = doctorRepository.findById(doctorId)
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
            
            // Validate that user can access this doctor
            if (!isDoctorAccessibleToUser(doctor, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access schedules for this doctor");
            }

            DayOfWeek dayOfWeek = date.getDayOfWeek();
            List<Schedule> schedules = scheduleRepository.findActiveScheduleByDoctorAndDayOfWeek(doctorId, dayOfWeek, date);

            // Filter schedules to only those accessible to the current user
            List<Schedule> accessibleSchedules = schedules.stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .collect(Collectors.toList());

            if (accessibleSchedules.isEmpty()) {
                return ResponseEntity.ok(Collections.emptyList());
            }
            List<Map<String, Object>> allSlotsWithStatus = new ArrayList<>();
            
            for (Schedule schedule : accessibleSchedules) {
                if (schedule.isAvailable() && schedule.getStatus() == ScheduleStatus.ACTIVE &&
                    schedule.getScheduleType() != ScheduleType.ON_LEAVE) {
                    
                    LocalTime startTime = schedule.getStartTime();
                    LocalTime endTime = schedule.getEndTime();
                    int duration = schedule.getAppointmentDurationMinutes() != null ? 
                                  schedule.getAppointmentDurationMinutes() : 30;

                    if (startTime != null && endTime != null) {
                        LocalTime currentTime = startTime;

                        while (!currentTime.isAfter(endTime.minusMinutes(duration))) {
                            Map<String, Object> slotInfo = new HashMap<>();
                            boolean scheduleAvailable = schedule.isWithinWorkingHours(currentTime) &&
                                                      !schedule.hasBreakAtTime(currentTime) &&
                                                      !schedule.isOnLunchBreak(currentTime);
                            LocalDateTime slotDateTime = LocalDateTime.of(date, currentTime);
                            List<Appointment> existingAppointments = appointmentRepository
                                .findByDoctorIdAndScheduledTime(doctorId, slotDateTime);
                            
                            boolean isAvailable = scheduleAvailable && existingAppointments.isEmpty();
                            
                            slotInfo.put("time", currentTime.toString());
                            slotInfo.put("available", isAvailable);
                            if (!scheduleAvailable) {
                                slotInfo.put("reason", "OUT_OF_SCHEDULE");
                            } else if (!existingAppointments.isEmpty()) {
                                slotInfo.put("reason", "ALREADY_BOOKED");
                                slotInfo.put("existingAppointments", existingAppointments.size());
                            }
                            
                            allSlotsWithStatus.add(slotInfo);
                            
                            currentTime = currentTime.plusMinutes(duration);
                        }
                    }
                }
            }

            allSlotsWithStatus.sort(Comparator.comparing(slot -> LocalTime.parse((String) slot.get("time"))));

            return ResponseEntity.ok(allSlotsWithStatus);
        } catch (Exception e) {
            logger.error("Error getting all slots with availability for doctorId: {} on date: {}", doctorId, date, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to get all slots with availability: " + e.getMessage());
        }
    }

    private ScheduleDTO convertToDTO(Schedule schedule) {
        ScheduleDTO dto = new ScheduleDTO();
        BeanUtils.copyProperties(schedule, dto);
        
        if (schedule.getDoctor() != null) {
            dto.setDoctorId(schedule.getDoctor().getId());
            dto.setDoctorName(schedule.getDoctor().getDoctorName());
            
            // Add branch information
            if (schedule.getDoctor().getBranch() != null) {
                dto.setBranchId(schedule.getDoctor().getBranch().getId());
                dto.setBranchName(schedule.getDoctor().getBranch().getBranchName());
                
                // Add company information
                if (schedule.getDoctor().getBranch().getClinic() != null) {
                    dto.setCompanyId(schedule.getDoctor().getBranch().getClinic().getId());
                    dto.setCompanyName(schedule.getDoctor().getBranch().getClinic().getCompanyName());
                }
            }
        }
        
        return dto;
    }


    private boolean isOverlapping(Schedule existing, ScheduleDTO newScheduleDTO) {
        if (existing.getDayOfWeek() != newScheduleDTO.getDayOfWeek()) {
            return false;
        }
        if (existing.getStartDate() == null || existing.getEndDate() == null ||
            newScheduleDTO.getStartDate() == null || newScheduleDTO.getEndDate() == null) {
            return true;
        }

        boolean dateOverlap = !(existing.getEndDate().isBefore(newScheduleDTO.getStartDate()) || 
                               existing.getStartDate().isAfter(newScheduleDTO.getEndDate()));

        boolean timeOverlap = true;
        
        return dateOverlap && timeOverlap;
    }

    private List<ScheduleDTO> generateScheduleDTOsFromBulk(BulkScheduleDTO bulkScheduleDTO) {
        List<ScheduleDTO> scheduleDTOs = new ArrayList<>();
        
        LocalDate currentDate = bulkScheduleDTO.getStartDate();
        while (!currentDate.isAfter(bulkScheduleDTO.getEndDate())) {
            DayOfWeek dayOfWeek = currentDate.getDayOfWeek();
            
            // Check if there's a specific configuration for this day
            BulkScheduleDTO.DayScheduleConfig dayConfig = bulkScheduleDTO.getDayConfigs() != null ?
                bulkScheduleDTO.getDayConfigs().stream()
                    .filter(config -> config.getDayOfWeek() == dayOfWeek)
                    .findFirst()
                    .orElse(null) : null;
            
            // Create schedule DTO for this day
            ScheduleDTO scheduleDTO = new ScheduleDTO();
            scheduleDTO.setDoctorId(bulkScheduleDTO.getDoctorId());
            scheduleDTO.setDayOfWeek(dayOfWeek);
            scheduleDTO.setStartDate(currentDate);
            scheduleDTO.setEndDate(currentDate);
            
            // Apply common settings or day-specific overrides
            if (dayConfig != null) {
                // Use day-specific configuration
                scheduleDTO.setStartTime(dayConfig.getStartTime() != null ? 
                    dayConfig.getStartTime() : bulkScheduleDTO.getCommonStartTime());
                scheduleDTO.setEndTime(dayConfig.getEndTime() != null ? 
                    dayConfig.getEndTime() : bulkScheduleDTO.getCommonEndTime());
                scheduleDTO.setBreakTimes(dayConfig.getBreakTimes() != null ? 
                    dayConfig.getBreakTimes() : bulkScheduleDTO.getCommonBreakTimes());
                scheduleDTO.setLunchStartTime(dayConfig.getLunchStartTime() != null ? 
                    dayConfig.getLunchStartTime() : bulkScheduleDTO.getCommonLunchStartTime());
                scheduleDTO.setLunchEndTime(dayConfig.getLunchEndTime() != null ? 
                    dayConfig.getLunchEndTime() : bulkScheduleDTO.getCommonLunchEndTime());
                scheduleDTO.setMaxDailyAppointments(dayConfig.getMaxDailyAppointments() != null ? 
                    dayConfig.getMaxDailyAppointments() : bulkScheduleDTO.getCommonMaxDailyAppointments());
                scheduleDTO.setAppointmentDurationMinutes(dayConfig.getAppointmentDurationMinutes() != null ? 
                    dayConfig.getAppointmentDurationMinutes() : bulkScheduleDTO.getCommonAppointmentDurationMinutes());
                scheduleDTO.setIsAvailable(dayConfig.getIsAvailable() != null ? 
                    dayConfig.getIsAvailable() : bulkScheduleDTO.getCommonIsAvailable());
                scheduleDTO.setScheduleType(dayConfig.getScheduleType() != null ? 
                    dayConfig.getScheduleType() : bulkScheduleDTO.getCommonScheduleType());
                scheduleDTO.setSpecialNote(dayConfig.getSpecialNote() != null ? 
                    dayConfig.getSpecialNote() : bulkScheduleDTO.getCommonSpecialNote());
                scheduleDTO.setStatus(dayConfig.getStatus() != null ? 
                    dayConfig.getStatus() : bulkScheduleDTO.getCommonStatus());
            } else {
                scheduleDTO.setStartTime(bulkScheduleDTO.getCommonStartTime());
                scheduleDTO.setEndTime(bulkScheduleDTO.getCommonEndTime());
                scheduleDTO.setBreakTimes(bulkScheduleDTO.getCommonBreakTimes());
                scheduleDTO.setLunchStartTime(bulkScheduleDTO.getCommonLunchStartTime());
                scheduleDTO.setLunchEndTime(bulkScheduleDTO.getCommonLunchEndTime());
                scheduleDTO.setMaxDailyAppointments(bulkScheduleDTO.getCommonMaxDailyAppointments());
                scheduleDTO.setAppointmentDurationMinutes(bulkScheduleDTO.getCommonAppointmentDurationMinutes());
                scheduleDTO.setIsAvailable(bulkScheduleDTO.getCommonIsAvailable());
                scheduleDTO.setScheduleType(bulkScheduleDTO.getCommonScheduleType());
                scheduleDTO.setSpecialNote(bulkScheduleDTO.getCommonSpecialNote());
                scheduleDTO.setStatus(bulkScheduleDTO.getCommonStatus());
            }
            
            // Set branch ID from bulk schedule DTO
            scheduleDTO.setBranchId(bulkScheduleDTO.getBranchId());
            
            // Only add if we have valid time configuration
            if (scheduleDTO.getStartTime() != null && scheduleDTO.getEndTime() != null) {
                scheduleDTOs.add(scheduleDTO);
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return scheduleDTOs;
    }
    
    private String getErrorMessage(ResponseEntity<?> response) {
        Object body = response.getBody();
        if (body instanceof Map) {
            Map<String, Object> map = (Map<String, Object>) body;
            Object message = map.get("message");
            return message != null ? message.toString() : "Unknown error";
        }
        return "Unknown error";
    }

    private List<ScheduleDTO> generateScheduleDTOsFromWeekTemplate(WeekTemplateDTO weekTemplateDTO) {
        List<ScheduleDTO> scheduleDTOs = new ArrayList<>();
        
        LocalDate currentDate = weekTemplateDTO.getStartDate();
        while (!currentDate.isAfter(weekTemplateDTO.getEndDate())) {
            boolean isWorkingDay = isWorkingDay(currentDate, weekTemplateDTO);
            
            if (isWorkingDay) {
                ScheduleDTO scheduleDTO = new ScheduleDTO();
                scheduleDTO.setDoctorId(weekTemplateDTO.getDoctorId());
                scheduleDTO.setDayOfWeek(currentDate.getDayOfWeek());
                scheduleDTO.setStartDate(currentDate);
                scheduleDTO.setEndDate(currentDate);
                scheduleDTO.setStartTime(weekTemplateDTO.getStartTime());
                scheduleDTO.setEndTime(weekTemplateDTO.getEndTime());
                scheduleDTO.setBreakTimes(weekTemplateDTO.getBreakTimes());
                scheduleDTO.setLunchStartTime(weekTemplateDTO.getLunchStartTime());
                scheduleDTO.setLunchEndTime(weekTemplateDTO.getLunchEndTime());
                scheduleDTO.setMaxDailyAppointments(weekTemplateDTO.getMaxDailyAppointments());
                scheduleDTO.setAppointmentDurationMinutes(weekTemplateDTO.getAppointmentDurationMinutes());
                scheduleDTO.setIsAvailable(weekTemplateDTO.getIsAvailable());
                scheduleDTO.setScheduleType(weekTemplateDTO.getScheduleType());
                scheduleDTO.setSpecialNote(weekTemplateDTO.getSpecialNote());
                scheduleDTO.setStatus(weekTemplateDTO.getStatus());
                
                // Set branch ID from week template DTO
                scheduleDTO.setBranchId(weekTemplateDTO.getBranchId());
                
                scheduleDTOs.add(scheduleDTO);
            }
            
            currentDate = currentDate.plusDays(1);
        }
        
        return scheduleDTOs;
    }
    
    @Override
    public ResponseEntity<?> createBulkSchedules(BulkScheduleDTO bulkScheduleDTO) {
        try {
            logger.info("Creating bulk schedules for doctorId: {} [{} to {}]", 
                       bulkScheduleDTO.getDoctorId(), bulkScheduleDTO.getStartDate(), bulkScheduleDTO.getEndDate());

            if (bulkScheduleDTO.getStartDate() == null || bulkScheduleDTO.getEndDate() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Start date and end date are required");
            }
            
            if (bulkScheduleDTO.getStartDate().isAfter(bulkScheduleDTO.getEndDate())) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Start date must be before or equal to end date");
            }

            Doctor doctor = doctorRepository.findById(bulkScheduleDTO.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + bulkScheduleDTO.getDoctorId()));

            List<ScheduleDTO> scheduleDTOs = generateScheduleDTOsFromBulk(bulkScheduleDTO);
            
            if (scheduleDTOs.isEmpty()) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "No valid schedules could be generated from the bulk configuration");
            }

            User currentUser = DbUtill.getCurrentUser();
            for (ScheduleDTO scheduleDTO : scheduleDTOs) {
                if (currentUser.getRole() == UserRole.SAAS_ADMIN || 
                    currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER || 
                    currentUser.getRole() == UserRole.SUPER_ADMIN || 
                    currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                    if (bulkScheduleDTO.getBranchId() != null) {
                        Branch branch = branchRepository.findById(bulkScheduleDTO.getBranchId())
                                .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + bulkScheduleDTO.getBranchId()));
                        if (!isBranchAccessibleToUser(branch, currentUser)) {
                            return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                    "You don't have permission to assign schedule to this branch");
                        }
                        scheduleDTO.setBranchId(bulkScheduleDTO.getBranchId());
                    }
                } else {
                    if (currentUser.getBranch() != null) {
                        scheduleDTO.setBranchId(currentUser.getBranch().getId());
                    }
                }
            }

            List<Schedule> createdSchedules = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (ScheduleDTO scheduleDTO : scheduleDTOs) {
                try {
                    ResponseEntity<?> result = createSchedule(scheduleDTO);
                    if (result.getStatusCode().is2xxSuccessful()) {
                        Schedule createdSchedule = scheduleRepository.findById(scheduleDTO.getId()).orElse(null);
                        if (createdSchedule != null) {
                            createdSchedules.add(createdSchedule);
                        }
                    } else {
                        errors.add("Failed to create schedule for " + scheduleDTO.getDayOfWeek() + 
                                 " (" + scheduleDTO.getStartDate() + "): " + getErrorMessage(result));
                    }
                } catch (Exception e) {
                    logger.error("Error creating schedule for day: {} on date: {}", 
                               scheduleDTO.getDayOfWeek(), scheduleDTO.getStartDate(), e);
                    errors.add("Error creating schedule for " + scheduleDTO.getDayOfWeek() + 
                             " (" + scheduleDTO.getStartDate() + "): " + e.getMessage());
                }
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("createdCount", createdSchedules.size());
            responseData.put("totalCount", scheduleDTOs.size());
            responseData.put("failedCount", errors.size());
            responseData.put("createdSchedules", createdSchedules.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
            responseData.put("errors", errors);

            String message = String.format("Bulk schedule creation completed. %d created, %d failed.", 
                                          createdSchedules.size(), errors.size());
            
            if (errors.isEmpty()) {
                logger.info("Bulk schedules created successfully for doctorId: {} [{} schedules]", 
                           bulkScheduleDTO.getDoctorId(), createdSchedules.size());
                return baseResponse.successResponse(message, responseData);
            } else {
                logger.warn("Bulk schedule creation partially completed for doctorId: {} [{} created, {} failed]", 
                           bulkScheduleDTO.getDoctorId(), createdSchedules.size(), errors.size());
                return baseResponse.successResponse(message + " Check errors for details.", responseData);
            }

        } catch (Exception e) {
            logger.error("Error creating bulk schedules for doctorId: {}", bulkScheduleDTO.getDoctorId(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create bulk schedules: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> createWeekTemplate(WeekTemplateDTO weekTemplateDTO) {
        try {
            logger.info("Creating week template for doctorId: {} [{} to {}]", 
                       weekTemplateDTO.getDoctorId(), weekTemplateDTO.getStartDate(), weekTemplateDTO.getEndDate());

            if (weekTemplateDTO.getStartDate() == null || weekTemplateDTO.getEndDate() == null) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Start date and end date are required");
            }
            
            if (weekTemplateDTO.getStartDate().isAfter(weekTemplateDTO.getEndDate())) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "Start date must be before or equal to end date");
            }

            boolean hasWorkingDays = (weekTemplateDTO.getMonday() != null && weekTemplateDTO.getMonday()) ||
                                   (weekTemplateDTO.getTuesday() != null && weekTemplateDTO.getTuesday()) ||
                                   (weekTemplateDTO.getWednesday() != null && weekTemplateDTO.getWednesday()) ||
                                   (weekTemplateDTO.getThursday() != null && weekTemplateDTO.getThursday()) ||
                                   (weekTemplateDTO.getFriday() != null && weekTemplateDTO.getFriday()) ||
                                   (weekTemplateDTO.getSaturday() != null && weekTemplateDTO.getSaturday()) ||
                                   (weekTemplateDTO.getSunday() != null && weekTemplateDTO.getSunday());
                                    
            if (!hasWorkingDays) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "At least one working day must be selected");
            }

            Doctor doctor = doctorRepository.findById(weekTemplateDTO.getDoctorId())
                    .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + weekTemplateDTO.getDoctorId()));

            List<ScheduleDTO> scheduleDTOs = generateScheduleDTOsFromWeekTemplate(weekTemplateDTO);
            
            if (scheduleDTOs.isEmpty()) {
                return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, "No valid schedules could be generated from the template");
            }

            User currentUser = DbUtill.getCurrentUser();
            
            for (ScheduleDTO scheduleDTO : scheduleDTOs) {
                if (currentUser.getRole() == UserRole.SAAS_ADMIN || 
                    currentUser.getRole() == UserRole.SAAS_ADMIN_MANAGER || 
                    currentUser.getRole() == UserRole.SUPER_ADMIN || 
                    currentUser.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
                    if (weekTemplateDTO.getBranchId() != null) {
                        Branch branch = branchRepository.findById(weekTemplateDTO.getBranchId())
                                .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + weekTemplateDTO.getBranchId()));
                        if (!isBranchAccessibleToUser(branch, currentUser)) {
                            return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                                    "You don't have permission to assign schedule to this branch");
                        }
                        scheduleDTO.setBranchId(weekTemplateDTO.getBranchId());
                    }
                } else {
                    if (currentUser.getBranch() != null) {
                        scheduleDTO.setBranchId(currentUser.getBranch().getId());
                    }
                }
            }

            List<Schedule> createdSchedules = new ArrayList<>();
            List<String> errors = new ArrayList<>();
            
            for (ScheduleDTO scheduleDTO : scheduleDTOs) {
                try {
                    ResponseEntity<?> result = createSchedule(scheduleDTO);
                    if (result.getStatusCode().is2xxSuccessful()) {
                        Schedule createdSchedule = scheduleRepository.findById(scheduleDTO.getId()).orElse(null);
                        if (createdSchedule != null) {
                            createdSchedules.add(createdSchedule);
                        }
                    } else {
                        errors.add("Failed to create schedule for " + scheduleDTO.getDayOfWeek() + 
                                 " (" + scheduleDTO.getStartDate() + "): " + getErrorMessage(result));
                    }
                } catch (Exception e) {
                    logger.error("Error creating schedule for day: {} on date: {}", 
                               scheduleDTO.getDayOfWeek(), scheduleDTO.getStartDate(), e);
                    errors.add("Error creating schedule for " + scheduleDTO.getDayOfWeek() + 
                             " (" + scheduleDTO.getStartDate() + "): " + e.getMessage());
                }
            }

            Map<String, Object> responseData = new HashMap<>();
            responseData.put("createdCount", createdSchedules.size());
            responseData.put("totalCount", scheduleDTOs.size());
            responseData.put("failedCount", errors.size());
            responseData.put("createdSchedules", createdSchedules.stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList()));
            responseData.put("errors", errors);

            String message = String.format("Week template creation completed. %d created, %d failed.", 
                                          createdSchedules.size(), errors.size());
            
            if (errors.isEmpty()) {
                logger.info("Week template created successfully for doctorId: {} [{} schedules]", 
                           weekTemplateDTO.getDoctorId(), createdSchedules.size());
                return baseResponse.successResponse(message, responseData);
            } else {
                logger.warn("Week template creation partially completed for doctorId: {} [{} created, {} failed]", 
                           weekTemplateDTO.getDoctorId(), createdSchedules.size(), errors.size());
                return baseResponse.successResponse(message + " Check errors for details.", responseData);
            }

        } catch (Exception e) {
            logger.error("Error creating week template for doctorId: {}", weekTemplateDTO.getDoctorId(), e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to create week template: " + e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getSchedulesByCompanyId(Long companyId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching schedules for company: {} [pageNo={}, pageSize={}],", companyId, pageNo, pageSize);
            
            User currentUser = DbUtill.getCurrentUser();
            
            // Validate that the user has access to this company
            if (currentUser.getRole() != UserRole.SUPER_ADMIN && 
                currentUser.getRole() != UserRole.SUPER_ADMIN_MANAGER &&
                currentUser.getRole() != UserRole.SAAS_ADMIN && 
                currentUser.getRole() != UserRole.SAAS_ADMIN_MANAGER) {
                Long userCompanyId = DbUtill.getLoggedInCompanyId();
                if (!userCompanyId.equals(companyId)) {
                    return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                            "You don't have permission to access this company's schedules");
                }
            }
            
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules = scheduleRepository.findByCompanyId(companyId, pageable);
            
            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);
            
            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching schedules for company: {}", companyId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<?> getSchedulesByCompanyWithOptionalBranch(Long companyId, Long branchId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching schedules for company: {} with optional branch: {} [pageNo={}, pageSize={}]", 
                       companyId, branchId, pageNo, pageSize);
            
            User currentUser = DbUtill.getCurrentUser();
            
            // Validate that the user has access to this company
            if (currentUser.getRole() != UserRole.SUPER_ADMIN && 
                currentUser.getRole() != UserRole.SUPER_ADMIN_MANAGER &&
                currentUser.getRole() != UserRole.SAAS_ADMIN && 
                currentUser.getRole() != UserRole.SAAS_ADMIN_MANAGER) {
                Long userCompanyId = DbUtill.getLoggedInCompanyId();
                if (!userCompanyId.equals(companyId)) {
                    return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                            "You don't have permission to access this company's schedules");
                }
            }
            
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules;
            
            // If branchId is provided, fetch branch-wise; otherwise fetch company-wise
            if (branchId != null) {
                logger.info("Fetching schedules branch-wise for branchId: {}", branchId);
                Branch branch = branchRepository.findById(branchId)
                        .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
                
                // Validate branch access
                if (!isBranchAccessibleToUser(branch, currentUser)) {
                    return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                            "You don't have permission to access this branch's schedules");
                }
                
                schedules = scheduleRepository.findByBranchId(branchId, pageable);
            } else {
                logger.info("Fetching schedules company-wise for companyId: {}", companyId);
                schedules = scheduleRepository.findByCompanyId(companyId, pageable);
            }
            
            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);
            
            String message = branchId != null 
                ? "Schedules fetched successfully for branch" 
                : "Schedules fetched successfully for company";
            return baseResponse.successResponse(message, response);
        } catch (Exception e) {
            logger.error("Error fetching schedules for company: {} with branch: {}", companyId, branchId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<?> getSchedulesByBranchId(Long branchId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching schedules for branch: {} [pageNo={}, pageSize={}],", branchId, pageNo, pageSize);
            
            User currentUser = DbUtill.getCurrentUser();
            Branch branch = branchRepository.findById(branchId)
                    .orElseThrow(() -> new IllegalArgumentException("Branch not found with id: " + branchId));
            
            if (!isBranchAccessibleToUser(branch, currentUser)) {
                return baseResponse.errorResponse(HttpStatus.FORBIDDEN,
                        "You don't have permission to access schedules for this branch");
            }
            
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules = scheduleRepository.findByBranchId(branchId, pageable);
            
            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .filter(schedule -> isScheduleAccessibleToUser(schedule, currentUser))
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);
            
            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching schedules for branch: {}", branchId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<?> getSchedulesByCompanyIdAndDoctorId(Long companyId, Long doctorId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching schedules for company: {} and doctor: {} [pageNo={}, pageSize={}],", companyId, doctorId, pageNo, pageSize);
            
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules = scheduleRepository.findByCompanyIdAndDoctorId(companyId, doctorId, pageable);
            
            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);
            
            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching schedules for company: {} and doctor: {}", companyId, doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }
    
    @Override
    public ResponseEntity<?> getSchedulesByBranchIdAndDoctorId(Long branchId, Long doctorId, Integer pageNo, Integer pageSize) {
        try {
            logger.info("Fetching schedules for branch: {} and doctor: {} [pageNo={}, pageSize={}],", branchId, doctorId, pageNo, pageSize);
            
            Pageable pageable = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);
            Page<Schedule> schedules = scheduleRepository.findByBranchIdAndDoctorId(branchId, doctorId, pageable);
            
            List<ScheduleDTO> scheduleDTOs = schedules.getContent().stream()
                    .map(this::convertToDTO)
                    .collect(Collectors.toList());
            
            Map<String, Object> response = DbUtill.buildPaginatedResponse(schedules, scheduleDTOs);
            
            return baseResponse.successResponse("Schedules fetched successfully", response);
        } catch (Exception e) {
            logger.error("Error fetching schedules for branch: {} and doctor: {}", branchId, doctorId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to fetch schedules: " + e.getMessage());
        }
    }
    
    private boolean isWorkingDay(LocalDate date, WeekTemplateDTO weekTemplateDTO) {
        switch (date.getDayOfWeek()) {
            case MONDAY: return weekTemplateDTO.getMonday() != null && weekTemplateDTO.getMonday();
            case TUESDAY: return weekTemplateDTO.getTuesday() != null && weekTemplateDTO.getTuesday();
            case WEDNESDAY: return weekTemplateDTO.getWednesday() != null && weekTemplateDTO.getWednesday();
            case THURSDAY: return weekTemplateDTO.getThursday() != null && weekTemplateDTO.getThursday();
            case FRIDAY: return weekTemplateDTO.getFriday() != null && weekTemplateDTO.getFriday();
            case SATURDAY: return weekTemplateDTO.getSaturday() != null && weekTemplateDTO.getSaturday();
            case SUNDAY: return weekTemplateDTO.getSunday() != null && weekTemplateDTO.getSunday();
            default: return false;
        }
    }

    /**
     * Validates doctor existence and branch access permissions
     * @param doctorId The doctor ID to validate
     * @param currentUser The current authenticated user
     * @return The validated Doctor entity
     * @throws IllegalArgumentException if doctor not found or access denied
     */
    private Doctor validateDoctorAndBranchAccess(Long doctorId, User currentUser) {
        Doctor doctor = doctorRepository.findById(doctorId)
                .orElseThrow(() -> new IllegalArgumentException("Doctor not found with id: " + doctorId));
        
        // Validate that user can access this doctor based on their role
        if (!isDoctorAccessibleToUser(doctor, currentUser)) {
            throw new IllegalArgumentException("You don't have permission to access this doctor");
        }
        
        return doctor;
    }
    
    /**
     * Checks if a doctor is accessible to the current user based on role and branch/company
     * @param doctor The doctor to check
     * @param user The current user
     * @return true if accessible, false otherwise
     */
    private boolean isDoctorAccessibleToUser(Doctor doctor, User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
            // Super admins can access all doctors
            return true;
        }
        
        if (user.getRole() == UserRole.BRANCH_MANAGER) {
            // Branch managers can only access doctors in their branch
            return user.getBranch() != null && 
                   doctor.getBranch() != null && 
                   user.getBranch().getId().equals(doctor.getBranch().getId());
        }
        
        if (user.getRole() == UserRole.DOCTOR) {
            // Doctors can only access their own schedule
            return doctor.getUser() != null && 
                   doctor.getUser().getId().equals(user.getId());
        }
        
        // For other roles (company-based), check company access
        Long userCompanyId = DbUtill.getLoggedInCompanyId();
        return doctor.getCompany() != null && 
               doctor.getCompany().getId().equals(userCompanyId);
    }
    
    /**
     * Checks if a schedule is accessible to the current user
     * @param schedule The schedule to check
     * @param user The current user
     * @return true if accessible, false otherwise
     */
    private boolean isScheduleAccessibleToUser(Schedule schedule, User user) {
        if (user.getRole() == UserRole.SUPER_ADMIN || user.getRole() == UserRole.SUPER_ADMIN_MANAGER) {
            // Super admins can access all schedules
            return true;
        }
        
        if (schedule.getDoctor() == null) {
            return false;
        }
        
        if (user.getRole() == UserRole.BRANCH_MANAGER) {
            // Branch managers can only access schedules of doctors in their branch
            return user.getBranch() != null && 
                   schedule.getDoctor().getBranch() != null && 
                   user.getBranch().getId().equals(schedule.getDoctor().getBranch().getId());
        }
        
        if (user.getRole() == UserRole.DOCTOR) {
            // Doctors can only access their own schedules
            return schedule.getDoctor().getUser() != null && 
                   schedule.getDoctor().getUser().getId().equals(user.getId());
        }
        
        // For other roles, check company access
        Long userCompanyId = DbUtill.getLoggedInCompanyId();
        return schedule.getDoctor().getCompany() != null && 
               schedule.getDoctor().getCompany().getId().equals(userCompanyId);
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

}