package com.sbpl.OPD.dto.experties;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class UpdateExpertiseRequest {
        private String expertiseName;
        private String departmentName;
        private Boolean isActive;
    }