package com.sbpl.OPD.repository;

import com.sbpl.OPD.Auth.model.User;
import com.sbpl.OPD.model.MedicalRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicalRecordRepository extends JpaRepository<MedicalRecord, Long> {

    Page<MedicalRecord> findAll(Pageable pageable);

    Page<MedicalRecord> findByPatientId(Long patientId, Pageable pageable);

    Page<MedicalRecord> findByDoctorId(Long doctorId,Pageable pageable);
    
    List<MedicalRecord> findByPatient(User patient);
    List<MedicalRecord> findByDoctor(User doctor);
    List<MedicalRecord> findByAppointmentId(Long appointmentId);
    
    Page<MedicalRecord> findByAppointmentId(Long appointmentId, Pageable pageable);
}