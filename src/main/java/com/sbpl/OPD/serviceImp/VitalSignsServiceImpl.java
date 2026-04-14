package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.VitalSignsDTO;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.VitalSigns;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.VitalSignsRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.VitalSignsService;
import com.sbpl.OPD.utils.DbUtill;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.Map;

/**
 * Implementation of vital signs service operations.
 * Handles vital sign recording, retrieval, and management.
 *
 * @author Rahul Kumar
 */
@Service
public class VitalSignsServiceImpl implements VitalSignsService {

    @Autowired
    private VitalSignsRepository vitalSignsRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private BaseResponse baseResponse;

    private static final Logger logger = LoggerFactory.getLogger(VitalSignsServiceImpl.class);

    @Override
    public ResponseEntity<?> createVitalSigns(VitalSignsDTO vitalSignsDTO) {
        logger.info("Creating vital signs for appointment [appointmentId={}]", vitalSignsDTO.getAppointmentId());

        try {
            Appointment appointment = appointmentRepository.findById(vitalSignsDTO.getAppointmentId())
                    .orElseThrow(() -> new IllegalArgumentException("Appointment not found"));

            User patient = userRepository.findById(vitalSignsDTO.getPatientId())
                    .orElseThrow(() -> new IllegalArgumentException("Patient not found"));

            User recorder = userRepository.findById(vitalSignsDTO.getRecordedById())
                    .orElseThrow(() -> new IllegalArgumentException("Recorder not found"));

            VitalSigns vitalSigns = new VitalSigns();
            vitalSigns.setAppointment(appointment);
            vitalSigns.setPatient(patient);
            vitalSigns.setRecordedBy(recorder);
            vitalSigns.setTemperature(vitalSignsDTO.getTemperature());
            vitalSigns.setBloodPressureSystolic(vitalSignsDTO.getBloodPressureSystolic());
            vitalSigns.setBloodPressureDiastolic(vitalSignsDTO.getBloodPressureDiastolic());
            vitalSigns.setHeartRate(vitalSignsDTO.getHeartRate());
            vitalSigns.setRespiratoryRate(vitalSignsDTO.getRespiratoryRate());
            vitalSigns.setOxygenSaturation(vitalSignsDTO.getOxygenSaturation());
            vitalSigns.setHeight(vitalSignsDTO.getHeight());
            vitalSigns.setWeight(vitalSignsDTO.getWeight());
            vitalSigns.setNotes(vitalSignsDTO.getNotes());

            if (vitalSignsDTO.getHeight() != null && vitalSignsDTO.getWeight() != null && vitalSignsDTO.getHeight() > 0) {
                double heightInMeters = vitalSignsDTO.getHeight() / 100.0; // Convert cm to meters
                double bmi = vitalSignsDTO.getWeight() / (heightInMeters * heightInMeters);
                vitalSigns.setBmi(Math.round(bmi * 100.0) / 100.0); // Round to 2 decimal places
            }

            VitalSigns savedVitalSigns = vitalSignsRepository.save(vitalSigns);

            logger.info("Vital signs created successfully [vitalSignsId={}]", savedVitalSigns.getId());
            return baseResponse.successResponse("Vital signs recorded successfully");

        } catch (IllegalArgumentException e) {
            logger.warn("Validation failed: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error creating vital signs", e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Something went wrong while recording vital signs. Please try again later.");
        }
    }

    @Override
    public ResponseEntity<?> getVitalSignsById(Long id) {
        logger.info("Request received to fetch vital signs [vitalSignsId={}]", id);

        try {
            VitalSigns vitalSigns = vitalSignsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Vital signs not found"));

            logger.info("Vital signs fetched successfully [vitalSignsId={}]", id);
            return baseResponse.successResponse("Vital signs fetched successfully", convertToDTO(vitalSigns));

        } catch (IllegalArgumentException e) {
            logger.warn("Vital signs not found [vitalSignsId={}]", id);
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error while fetching vital signs [vitalSignsId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch vital signs at the moment");
        }
    }

    @Override
    public ResponseEntity<?> getVitalSignsByAppointment(Long appointmentId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching vital signs for appointment [appointmentId={}, pageNo={}, pageSize={}]", 
                   appointmentId, pageNo, pageSize);

        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<VitalSigns> page = vitalSignsRepository.findByAppointmentId(appointmentId, pageRequest);

            var dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse("Vital signs fetched successfully", response);

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching vital signs for appointmentId={}", appointmentId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch vital signs");
        }
    }

    @Override
    public ResponseEntity<?> getVitalSignsByPatient(Long patientId, Integer pageNo, Integer pageSize) {
        logger.info("Fetching vital signs for patient [patientId={}, pageNo={}, pageSize={}]", 
                   patientId, pageNo, pageSize);

        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSortForJPQL(pageNo, pageSize);
            Page<VitalSigns> page = vitalSignsRepository.findByPatientId(patientId, pageRequest);

            var dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse("Patient vital signs fetched successfully", response);

        } catch (IllegalArgumentException e) {
            logger.warn("Pagination validation failed | {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.BAD_REQUEST, e.getMessage());
        } catch (Exception e) {
            logger.error("Error fetching vital signs for patientId={}", patientId, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch vital signs");
        }
    }

    @Override
    public ResponseEntity<?> updateVitalSigns(Long id, VitalSignsDTO vitalSignsDTO) {
        logger.info("Updating vital signs [vitalSignsId={}]", id);

        try {
            VitalSigns vitalSigns = vitalSignsRepository.findById(id)
                    .orElseThrow(() -> new IllegalArgumentException("Vital signs not found"));

            if (vitalSignsDTO.getTemperature() != null) vitalSigns.setTemperature(vitalSignsDTO.getTemperature());
            if (vitalSignsDTO.getBloodPressureSystolic() != null) vitalSigns.setBloodPressureSystolic(vitalSignsDTO.getBloodPressureSystolic());
            if (vitalSignsDTO.getBloodPressureDiastolic() != null) vitalSigns.setBloodPressureDiastolic(vitalSignsDTO.getBloodPressureDiastolic());
            if (vitalSignsDTO.getHeartRate() != null) vitalSigns.setHeartRate(vitalSignsDTO.getHeartRate());
            if (vitalSignsDTO.getRespiratoryRate() != null) vitalSigns.setRespiratoryRate(vitalSignsDTO.getRespiratoryRate());
            if (vitalSignsDTO.getOxygenSaturation() != null) vitalSigns.setOxygenSaturation(vitalSignsDTO.getOxygenSaturation());
            if (vitalSignsDTO.getHeight() != null) vitalSigns.setHeight(vitalSignsDTO.getHeight());
            if (vitalSignsDTO.getWeight() != null) vitalSigns.setWeight(vitalSignsDTO.getWeight());
            if (vitalSignsDTO.getNotes() != null) vitalSigns.setNotes(vitalSignsDTO.getNotes());

            if ((vitalSignsDTO.getHeight() != null || vitalSignsDTO.getWeight() != null) &&
                vitalSigns.getHeight() != null && vitalSigns.getWeight() != null && vitalSigns.getHeight() > 0) {
                double heightInMeters = vitalSigns.getHeight() / 100.0;
                double bmi = vitalSigns.getWeight() / (heightInMeters * heightInMeters);
                vitalSigns.setBmi(Math.round(bmi * 100.0) / 100.0);
            }

            vitalSigns.setUpdatedAt(new Date());
            vitalSignsRepository.save(vitalSigns);

            logger.info("Vital signs updated successfully [vitalSignsId={}]", id);
            return baseResponse.successResponse("Vital signs updated successfully");

        } catch (IllegalArgumentException e) {
            logger.warn("Update failed: {}", e.getMessage());
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        } catch (Exception e) {
            logger.error("Error updating vital signs [vitalSignsId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to update vital signs");
        }
    }

    @Override
    public ResponseEntity<?> deleteVitalSigns(Long id) {
        logger.info("Request received to delete vital signs [vitalSignsId={}]", id);

        try {
            if (!vitalSignsRepository.existsById(id)) {
                logger.warn("Vital signs not found for deletion [vitalSignsId={}]", id);
                return baseResponse.errorResponse(HttpStatus.NOT_FOUND, "Vital signs not found");
            }

            vitalSignsRepository.deleteById(id);

            logger.info("Vital signs deleted successfully [vitalSignsId={}]", id);
            return baseResponse.successResponse("Vital signs deleted successfully");

        } catch (Exception e) {
            logger.error("Error deleting vital signs [vitalSignsId={}]", id, e);
            return baseResponse.errorResponse(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Failed to delete vital signs");
        }
    }

    @Override
    public ResponseEntity<?> createMyVitalSigns(VitalSignsDTO vitalSignsDTO) {
        User currentUser = DbUtill.getCurrentUser();
        vitalSignsDTO.setRecordedById(currentUser.getId());
        return createVitalSigns(vitalSignsDTO);
    }

    @Override
    public ResponseEntity<?> getMyVitalSignsByAppointment(Long appointmentId) {
        User currentUser = DbUtill.getCurrentUser();
        return getVitalSignsByAppointment(appointmentId, 0, 10);
    }

    @Override
    public ResponseEntity<?> getMyVitalSigns(Integer pageNo, Integer pageSize) {
        User currentUser = DbUtill.getCurrentUser();
        PageRequest pageRequest = DbUtill.buildPageRequest(pageNo, pageSize);
        Page<VitalSigns> page = vitalSignsRepository.findByPatientId(currentUser.getId(), pageRequest);
        
        var dtoList = page.getContent().stream()
                .map(this::convertToDTO)
                .toList();

        Map<String, Object> response = DbUtill.buildPaginatedResponse(page, dtoList);
        return baseResponse.successResponse("My vital signs fetched successfully", response);
    }

    private VitalSignsDTO convertToDTO(VitalSigns vitalSigns) {
        VitalSignsDTO dto = new VitalSignsDTO();
        dto.setId(vitalSigns.getId());
        dto.setAppointmentId(vitalSigns.getAppointment().getId());
        dto.setPatientId(vitalSigns.getPatient().getId());
        dto.setRecordedById(vitalSigns.getRecordedBy().getId());
        dto.setTemperature(vitalSigns.getTemperature());
        dto.setBloodPressureSystolic(vitalSigns.getBloodPressureSystolic());
        dto.setBloodPressureDiastolic(vitalSigns.getBloodPressureDiastolic());
        dto.setHeartRate(vitalSigns.getHeartRate());
        dto.setRespiratoryRate(vitalSigns.getRespiratoryRate());
        dto.setOxygenSaturation(vitalSigns.getOxygenSaturation());
        dto.setHeight(vitalSigns.getHeight());
        dto.setWeight(vitalSigns.getWeight());
        dto.setBmi(vitalSigns.getBmi());
        dto.setNotes(vitalSigns.getNotes());
        dto.setCreatedAt(vitalSigns.getCreatedAt());
        dto.setUpdatedAt(vitalSigns.getUpdatedAt());
        return dto;
    }
}