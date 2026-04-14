package com.sbpl.OPD.serviceImp;

import com.sbpl.OPD.Auth.repository.UserRepository;
import com.sbpl.OPD.dto.MedicalRecordDTO;
import com.sbpl.OPD.dto.medicalrecord.MedicalRecordRequestDto;
import com.sbpl.OPD.model.Appointment;
import com.sbpl.OPD.model.Customer;
import com.sbpl.OPD.model.Doctor;
import com.sbpl.OPD.model.MedicalRecord;
import com.sbpl.OPD.repository.AppointmentRepository;
import com.sbpl.OPD.repository.CustomerRepository;
import com.sbpl.OPD.repository.DoctorRepository;
import com.sbpl.OPD.repository.MedicalRecordRepository;
import com.sbpl.OPD.response.BaseResponse;
import com.sbpl.OPD.service.MedicalRecordService;
import com.sbpl.OPD.utils.DbUtill;
import com.sbpl.OPD.utils.S3BucketStorageUtility;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.Date;
import java.util.List;
import java.util.Map;

@Service
public class MedicalRecordServiceImpl implements MedicalRecordService {

    private static final Logger logger = LoggerFactory.getLogger(MedicalRecordServiceImpl.class);

    @Autowired
    private MedicalRecordRepository medicalRecordRepository;

    @Autowired
    private UserRepository userRepository;
    @Autowired
    private CustomerRepository customerRepository;
    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private BaseResponse baseResponse;

    @Autowired
    private S3BucketStorageUtility s3BucketStorageUtility;

    @Override
    public ResponseEntity<?> uploadMedicalRecord(MedicalRecordRequestDto requestDto) {

        logger.info("Uploading medical record [patientId={}, doctorId={}, appointmentId={}]",
                requestDto.getPatientId(),
                requestDto.getDoctorId(),
                requestDto.getAppointmentId());

        try {

            MultipartFile file = requestDto.getRecord();

            if (file == null || file.isEmpty()) {
                throw new RuntimeException("File is empty");
            }

            String originalFileName = file.getOriginalFilename();
            String contentType = file.getContentType();
            byte[] fileBytes = file.getBytes();

            // Upload to S3

            String s3Url;
            if ("other".equalsIgnoreCase(requestDto.getRecordType())) {
                s3Url = s3BucketStorageUtility.uploadFileWithNonMedRec(
                        originalFileName, fileBytes, contentType, requestDto.getUhid(), requestDto.getAppointmentId());
            } else {
                s3Url = s3BucketStorageUtility.uploadFileWithUniqueName(
                        originalFileName, fileBytes, contentType, requestDto.getUhid(), requestDto.getAppointmentId());
            }
            MedicalRecord medicalRecord = new MedicalRecord();
            medicalRecord.setRecordType(requestDto.getRecordType());
            medicalRecord.setFileName(originalFileName);
            medicalRecord.setFilePath(s3Url);
            medicalRecord.setMimeType(contentType);
            medicalRecord.setFileSize(file.getSize());
            medicalRecord.setDescription(requestDto.getDescription());
            medicalRecord.setAccessLevel(requestDto.getAccessLevel());
            medicalRecord.setTags(requestDto.getTags());
            medicalRecord.setIsConfidential(requestDto.getIsConfidential());
            medicalRecord.setCreatedAt(new Date());

            // Patient
            Customer patient = customerRepository.findById(requestDto.getPatientId())
                    .orElseThrow(() -> new RuntimeException("Patient not found"));
            medicalRecord.setPatient(patient);

            // Doctor
            Doctor doctor = doctorRepository.findById(requestDto.getDoctorId())
                    .orElseThrow(() -> new RuntimeException("Doctor not found"));
            medicalRecord.setDoctor(doctor);

            // Appointment
            if (requestDto.getAppointmentId() != null) {
                Appointment appointment = appointmentRepository.findById(requestDto.getAppointmentId())
                        .orElseThrow(() -> new RuntimeException("Appointment not found"));
                medicalRecord.setAppointment(appointment);
            }

            medicalRecordRepository.save(medicalRecord);

            return baseResponse.successResponse("Medical record uploaded successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to process file: " + e.getMessage());
        }
    }


    @Override
    public ResponseEntity<?> getMedicalRecordById(Long id) {
        logger.info("fetching medical record through medicalId = {}",id);
        try {
            MedicalRecord record = medicalRecordRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Medical record not found"));
            logger.info("medical record fetched successfully for medicalId ={}",id);
            return baseResponse.successResponse("Record fetched successfully",record);
        } catch (Exception ex) {
            logger.error(
                    "Error while fetching medical record [medicalId={}]",
                    id, ex
            );
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, ex.getMessage());
        }

    }

    @Override
    public MedicalRecord getMedicalRecordEntityById(Long id) {
        logger.info("fetching medical record through medicalId = {}",id);
        try {
            MedicalRecord record = medicalRecordRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("Medical record not found"));
            logger.info("medical record fetched successfully for medicalId ={}",id);
            return record;
        } catch (Exception ex) {
            logger.error(
                    "Error while fetching medical record [medicalId={}]",
                    id, ex
            );
            throw new RuntimeException(ex);
        }

    }

    @Override
    public ResponseEntity<?> getMedicalRecordsByPatientId(Long patientId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Fetching appointments for patient [patientId={}, pageNo={}, pageSize={}]", 
                patientId, pageNo, pageSize
        );

        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

            Page<MedicalRecord> page =
                    medicalRecordRepository.findByPatientId(patientId, pageRequest);

            List<MedicalRecordDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();


            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            return baseResponse.successResponse(
                    "Patient medical record fetched successfully",
                    response
            );
        } catch (Exception e) {
            logger.error(
                    "Error while fetching medical record [patientId={}]", 
                    patientId, e
            );
            return baseResponse.errorResponse(HttpStatus.NOT_FOUND, e.getMessage());
        }
    }

    @Override
    public ResponseEntity<?> getMedicalRecordsByDoctorId(Long doctorId, Integer pageNo, Integer pageSize) {
        logger.info(
                "Request received to fetch medical records by doctorId [doctorId={}, pageNo={}, pageSize={}]", 
                doctorId, pageNo, pageSize
        );
       try {

           PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

           Page<MedicalRecord> page =
                    medicalRecordRepository.findByDoctorId(doctorId, pageRequest);

           List<MedicalRecordDTO> dtoList = page.getContent().stream()
                   .map(this::convertToDTO)
                   .toList();
           Map<String,Object> response =
                   DbUtill.buildPaginatedResponse(page,dtoList);
           logger.info(
                   "Fetched {} medical records for doctorId={} on page {}",
                   page.getNumberOfElements(),
                   doctorId,
                   page.getNumber()
           );

           return baseResponse.successResponse(
                   "Medical records fetched successfully",
                   response
           );
       } catch (Exception e) {
           logger.info(
                   "Error while fetching medical record of patient by the doctorId ={}",doctorId);
           return baseResponse.errorResponse(HttpStatus.NOT_FOUND,e.getMessage());
       }
    }

    @Override
    public ResponseEntity<?> getMedicalRecordsByAppointmentId(Long appointmentId, Integer pageNo, Integer pageSize) {

        logger.info(
                "Request received to fetch medical records by appointmentId [appointmentId={}, pageNo={}, pageSize={}]", 
                appointmentId, pageNo, pageSize
        );

        try {
            PageRequest pageRequest = DbUtill.buildPageRequestWithDefaultSort(pageNo, pageSize);

            Page<MedicalRecord> page =
                    medicalRecordRepository.findByAppointmentId(appointmentId, pageRequest);

            List<MedicalRecordDTO> dtoList = page.getContent().stream()
                    .map(this::convertToDTO)
                    .toList();

            Map<String, Object> response =
                    DbUtill.buildPaginatedResponse(page, dtoList);

            logger.info(
                    "Fetched {} medical records for appointmentId={} on page {}",
                    page.getNumberOfElements(),
                    appointmentId,
                    page.getNumber()
            );

            return baseResponse.successResponse(
                    "Medical records fetched successfully",
                    response
            );

        } catch (Exception e) {

            logger.error(
                    "Error while fetching medical records for appointmentId={}",
                    appointmentId,
                    e
            );

            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to fetch medical records at the moment. Please try again later."
            );
        }
    }


    @Override
    public ResponseEntity<?> deleteMedicalRecord(Long medicalId) {

        logger.info(
                "Request received to delete medical record [medicalId={}]", 
                medicalId
        );

        try {

            MedicalRecord record = medicalRecordRepository.findById(medicalId)
                    .orElseThrow(() ->
                            new IllegalArgumentException("Medical record not found"));

            // If the record has a file path (S3 URL), extract the file key and delete from S3
            if (record.getFilePath() != null && !record.getFilePath().isBlank()) {
                String fileName = extractFileNameFromS3Url(record.getFilePath());
                if (fileName != null) {
                    s3BucketStorageUtility.deleteFile(fileName);
                    logger.info(
                            "Medical record file deleted from S3 [path={}]", 
                            record.getFilePath()
                    );
                }
            }

            medicalRecordRepository.delete(record);

            logger.info(
                    "Medical record deleted successfully [medicalId={}]", 
                    medicalId
            );

            return baseResponse.successResponse(
                    "Medical record deleted successfully"
            );

        } catch (IllegalArgumentException e) {

            logger.warn(
                    "Delete medical record failed | medicalId={} | reason={}",
                    medicalId, e.getMessage()
            );

            return baseResponse.errorResponse(
                    HttpStatus.NOT_FOUND,
                    e.getMessage()
            );

        } catch (Exception e) {

            logger.error(
                    "Unexpected error while deleting medical record [medicalId={}]", 
                    medicalId, e
            );

            return baseResponse.errorResponse(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "Unable to delete medical record at the moment"
            );
        }
    }


    @Override
    public byte[] downloadMedicalRecord(Long id) {
        MedicalRecord record = medicalRecordRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Medical record not found"));

        // Extract file name from S3 URL and download from S3
        String fileName = extractFileNameFromS3Url(record.getFilePath());
        if (fileName != null) {
            String base64Content = s3BucketStorageUtility.getFileAsBase64(fileName);
            if (base64Content != null) {
                return java.util.Base64.getDecoder().decode(base64Content);
            } else {
                throw new RuntimeException("Failed to download file from S3");
            }
        } else {
            throw new RuntimeException("Invalid S3 file path");
        }
    }

    private MedicalRecordDTO convertToDTO(MedicalRecord record) {
        MedicalRecordDTO dto = new MedicalRecordDTO();
        dto.setId(record.getId());
        dto.setPatientId(record.getPatient().getId());
        dto.setDoctorId(record.getDoctor().getId());
        if (record.getAppointment() != null) {
            dto.setAppointmentId(record.getAppointment().getId());
        }
        dto.setRecordType(record.getRecordType());
        dto.setFileName(record.getFileName());
        dto.setFilePath(record.getFilePath()); // This will now be the S3 URL
        dto.setMimeType(record.getMimeType());
        dto.setFileSize(record.getFileSize());
        dto.setDescription(record.getDescription());
        dto.setCreatedAt(record.getCreatedAt().toInstant().atZone(java.time.ZoneId.systemDefault()).toLocalDateTime());
        
        // Set names for display
        dto.setPatientName(record.getPatient().getFirstName() + " " + record.getPatient().getLastName());
        dto.setDoctorName(record.getDoctor().getDoctorName());

        // Set base64 content of the file
        if (record.getFilePath() != null && !record.getFilePath().isBlank()) {
            try {
                String fileName = extractFileNameFromS3Url(record.getFilePath());
                if (fileName != null) {
                    String base64Content = s3BucketStorageUtility.getFileAsBase64(fileName);
                    dto.setBase64(base64Content);
                }
            } catch (Exception e) {
                logger.warn("Failed to fetch base64 content for medical record id={}: {}", record.getId(), e.getMessage());
                // Continue without base64 - don't fail the entire operation
            }
        }

        return dto;
    }

    /**
     * Extracts the file name from an S3 URL
     */
    private String extractFileNameFromS3Url(String s3Url) {
        if (s3Url == null) return null;
        // Extract the file name from the S3 URL
        // Format: https://bucket-name.s3.region.amazonaws.com/filename
        int lastSlashIndex = s3Url.lastIndexOf('/');
        if (lastSlashIndex != -1 && lastSlashIndex < s3Url.length() - 1) {
            return s3Url.substring(lastSlashIndex + 1);
        }
        return s3Url;
    }
}