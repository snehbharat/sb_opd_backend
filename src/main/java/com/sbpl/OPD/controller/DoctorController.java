package com.sbpl.OPD.controller;

import com.sbpl.OPD.dto.Doctor.request.DoctorDTO;
import com.sbpl.OPD.enums.DoctorSearchType;
import com.sbpl.OPD.exception.AccessDeniedException;
import com.sbpl.OPD.service.DoctorService;
import com.sbpl.OPD.utils.RbacUtil;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for doctor-related operations.
 *
 * Provides APIs for managing doctor profiles,
 * availability, activation status,
 * and consultation details.
 *
 * @author Rahul Kumar
 */

@RestController
@RequestMapping("/api/v1/doctors")
public class DoctorController {

    @Autowired
    private DoctorService doctorService;

    @Autowired
    private RbacUtil rbacUtil;

    @GetMapping
    public ResponseEntity<?> getAllDoctors(
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize,
            @RequestParam(required = false) Long branchId) {
        return doctorService.getAllDoctors(pageNo, pageSize, branchId);
    }

//    @GetMapping("/minimal")
//    public ResponseEntity<?> getAllDoctorsMinimal() {
//        return doctorService.getAllDoctorsMinimal();
//    }

    @PostMapping
    public ResponseEntity<?> createDoctor(
            @Valid @RequestBody DoctorDTO doctorDTO) {
        return doctorService.createDoctor(doctorDTO);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> getDoctorById(
            @PathVariable @NotNull Long id) {
        return doctorService.getDoctorById(id);
    }

    @GetMapping("/by-department")
    public ResponseEntity<?> getDoctorsByDepartment(
            @RequestParam @NotNull String department,
            @RequestParam(required = false) Integer pageNo,
            @RequestParam(required = false) Integer pageSize) {
        return doctorService.getDoctorsByDepartment(department, pageNo, pageSize);
    }

//    @GetMapping("/by-specialization")
//    public ResponseEntity<?> getDoctorsBySpecialization(
//            @RequestParam @NotNull String specialization,
//            @RequestParam(required = false) Integer pageNo,
//            @RequestParam(required = false) Integer pageSize) {
//        // Only users with DOCTOR_VIEW permission can get doctors by specialization
//        if (!rbacUtil.hasPermission("DOCTOR_VIEW")) {
//            throw new AccessDeniedException("Insufficient permissions to view doctors by specialization");
//        }
//        return doctorService.getDoctorsBySpecialization(specialization, pageNo, pageSize);
//    }

    @PutMapping("/update/{id}")
    public ResponseEntity<?> updateDoctor(
            @PathVariable Long id,
            @Valid @RequestBody DoctorDTO doctorDTO) {
        return doctorService.updateDoctor(id, doctorDTO);
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteDoctor(
            @PathVariable Long id) {
        if (!rbacUtil.isAdmin()) {
            throw new AccessDeniedException("Insufficient role to delete doctor");
        }
        return doctorService.deleteDoctor(id);
    }

    @PutMapping("/activate-Or-deactivate/{doctorId}")
    public ResponseEntity<?> activateOrDeactivateDoctor(
            @PathVariable Long doctorId,
            @RequestParam Boolean active
    ) {
        return doctorService.activateOrDeactivateDoctor(doctorId, active);
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchDoctor(
            @RequestParam DoctorSearchType type,
            @RequestParam String keyword) {

        return doctorService.searchDoctor(type, keyword);
    }
}