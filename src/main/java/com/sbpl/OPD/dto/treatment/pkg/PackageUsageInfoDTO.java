package com.sbpl.OPD.dto.treatment.pkg;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

@Getter
@Setter
public class PackageUsageInfoDTO {
    private Long packageUsageId;
    private Long treatmentPackageId;
    private String packageName;
    private String treatmentName;
    private Integer totalSessions;
    private Integer sessionsUsed;
    private Boolean followUp;
    private String followUpDate;
    private Integer sessionsRemaining;
    private Boolean completed;
    private Date purchaseDate;
    private Date lastSessionDate;
}