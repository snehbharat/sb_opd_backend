package com.sbpl.OPD.dto.Doctor.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
public class DoctorDTO {

    @Size(max = 10, message = "Prefix must not exceed 10 characters")
    private String prefix;

    @NotBlank
    private String doctorName;

    @NotBlank
    @Email
    private String doctorEmail;

    @NotBlank
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone number must be 10 digits")
    private String phoneNumber;

    @NotBlank
    private String gender;

    private String dateOfBirth;

    @NotBlank
    private String specialization;

    private String subSpecialization;

    @NotBlank
    private String registrationNumber;

    @Min(0)
    private Integer experienceYears;

    private String qualification;
    private String university;

    private String department;
    private String consultationRoom;
    private String shiftTiming;

    @PositiveOrZero
    private Double consultationFee;

    private Boolean onlineConsultationAvailable;

    private Boolean isActive;

    private LocalDate joiningDate;

    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;

    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotNull
    private Long companyId;

    @NotNull
    private Long branchId;

    private Long coreExpertiseId;
}
