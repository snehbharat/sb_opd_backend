package com.sbpl.OPD.Auth.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SimpleBranchDto {
    private Long id;
    private String branchName;
    private Long clinicId;
}