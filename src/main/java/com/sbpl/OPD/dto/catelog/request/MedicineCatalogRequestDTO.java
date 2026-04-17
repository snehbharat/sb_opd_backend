package com.sbpl.OPD.dto.catelog.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicineCatalogRequestDTO {

    @NotBlank(message = "Medicine name is required")
    @Size(max = 150, message = "Medicine name must not exceed 150 characters")
    private String name;

    @Size(max = 50, message = "Form must not exceed 50 characters")
    private String form;  //Tablet,Syrup

    @Size(max = 100, message = "Brand name must not exceed 100 characters")
    private String brandName;

    @Size(max = 50, message = "Strength must not exceed 50 characters")
    private String strength; // 650mg
}