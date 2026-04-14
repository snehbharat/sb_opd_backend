package com.sbpl.OPD.Auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleCompanyDto {
    private Long id;
    private String companyName;
    private Long adminUserId;
}