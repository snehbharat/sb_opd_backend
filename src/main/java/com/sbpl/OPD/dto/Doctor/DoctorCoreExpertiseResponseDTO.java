package com.sbpl.OPD.dto.Doctor;

import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO for doctor's core expertise.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class DoctorCoreExpertiseResponseDTO {

    private Long id;
    private String expertiseName;
    private String description;
    private String category;
    private Boolean isActive;
    private Long doctorCount; // Number of doctors with this expertise
}
