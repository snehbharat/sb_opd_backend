package com.sbpl.OPD.dto.Doctor;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * DTO for creating or updating a doctor's core expertise.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class DoctorCoreExpertiseDTO {

    @NotBlank(message = "Expertise name is required")
    @Size(max = 100, message = "Expertise name must not exceed 100 characters")
    private String expertiseName;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;

    private Boolean isActive;
}
