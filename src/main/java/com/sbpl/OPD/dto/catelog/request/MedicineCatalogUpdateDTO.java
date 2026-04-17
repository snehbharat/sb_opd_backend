package com.sbpl.OPD.dto.catelog.request;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MedicineCatalogUpdateDTO {

    private String name;
    private String form;
    private String brandName;
    private String strength;
}