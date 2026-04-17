package com.sbpl.OPD.dto.catelog.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating a symptom.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class SymptomUpdateDTO {

    @NotBlank(message = "Symptom name is required")
    @Size(max = 150, message = "Symptom name must not exceed 150 characters")
    private String name;
}
