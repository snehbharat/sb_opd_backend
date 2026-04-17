package com.sbpl.OPD.dto.catelog.response;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicineCatalogResponseDTO {

    private Long id;
    private String name;
    private String form;
    private String brandName;
    private String strength;
    private boolean isActive;
}