package com.sbpl.OPD.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class SuperAdminDashboardDTO {
    private Long totalCompanies;
    private Long totalBranches;
    private Long totalDoctors;
    private Long totalPatients;
    private Long activeCompanies;
    private Long activeBranches;
    private Long activeDoctors;
    private Long activePatients;
    private LocalDateTime lastSystemUpdate;
    private String systemStatus;
}