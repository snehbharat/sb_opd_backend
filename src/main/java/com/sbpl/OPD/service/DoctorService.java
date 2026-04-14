package com.sbpl.OPD.service;

import com.sbpl.OPD.dto.Doctor.request.DoctorDTO;
import com.sbpl.OPD.enums.DoctorSearchType;
import org.springframework.http.ResponseEntity;

/**
 * This is a doctor creation service class .
 *
 * @author Rahul Kumar
 */

public interface DoctorService {

    ResponseEntity<?> createDoctor(DoctorDTO doctorDTO);

    ResponseEntity<?> updateDoctor(Long doctorId, DoctorDTO doctorDTO);

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

    ResponseEntity<?> searchDoctor(DoctorSearchType type, String value);

//    ResponseEntity<?> getAllDoctorsMinimal();
}
