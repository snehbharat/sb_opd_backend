package com.sbpl.OPD.dto.Doctor.response;

import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Setter
public class DoctorResponseDTO {

    private Long doctorId;
    private Long userId;

    private Long createdBy;

    // Basic Info
    private String prefix;
    private String doctorName;
    private String doctorEmail;
    private String phoneNumber;
    private String gender;
    private String dateOfBirth;

    // Professional Info
    private String specialization;
    private String subSpecialization;
    private String registrationNumber;
    private Integer experienceYears;
    private String qualification;
    private String university;

    // Organization Info
    private String department;
    private String consultationRoom;
    private String shiftTiming;

    private Double consultationFee;
    private Boolean onlineConsultationAvailable;

    private Boolean active;
    private LocalDate joiningDate;

    // Company Info
    private Long companyId;
    private String companyName;

    // Branch Info
    private Long branchId;
    private String branchName;

    private List<String> coreExpertiseNames;

    private DoctorScheduleDaysDTO scheduleDays;

}
