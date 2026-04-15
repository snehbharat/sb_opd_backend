package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.Doctor.request.DoctorDTO;
import com.sbpl.OPD.enums.DoctorSearchType;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

/**
 * This is a doctor creation service class .
 *
 * @author Rahul Kumar
 */

public interface DoctorService {

//    ResponseEntity<?> createDoctor(DoctorDTO doctorDTO);

    ResponseEntity<?> createDoctor(DoctorDTO dto, MultipartFile doctorSign);

    ResponseEntity<?> updateDoctor(Long doctorId, DoctorDTO doctorDTO,MultipartFile doctorSign);

    ResponseEntity<?> getDoctorById(Long doctorId);

    ResponseEntity<?> getAllDoctors(Integer pageNo, Integer pageSize, Long branchId);

    ResponseEntity<?> getDoctorsByDepartment(
            String department,
            Integer pageNo,
            Integer pageSize
    );

    ResponseEntity<?> activateOrDeactivateDoctor(
            Long doctorId,
            Boolean active
    );

    ResponseEntity<?> deleteDoctor(Long doctorId);

    ResponseEntity<?> getDoctorSignAsBase64(Long doctorId);

    ResponseEntity<?> searchDoctor(DoctorSearchType type, String value);

//    ResponseEntity<?> getAllDoctorsMinimal();
}
