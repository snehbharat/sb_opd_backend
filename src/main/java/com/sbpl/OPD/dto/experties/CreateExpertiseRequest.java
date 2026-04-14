package com.sbpl.OPD.dto.experties;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateExpertiseRequest {
        @NotBlank(message = "Expertise name is required")
        private String expertiseName;
        private String description;
        private String category;
    }