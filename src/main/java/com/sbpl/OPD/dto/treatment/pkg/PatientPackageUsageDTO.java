package com.sbpl.OPD.dto.treatment.pkg;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;

@Getter
@Setter
public class PatientPackageUsageDTO {

    private Long id;

    private Long patientId;

    private String patientName;

    private Long treatmentPackageId;

    private String packageName;

    private Long treatmentId;

    private String treatmentName;

    private Integer totalSessions;

    private Integer sessionsUsed;

    private Integer sessionsRemaining;

    private BigDecimal packagePricePaid;

    private Boolean followUp;

    private String followUpDate;

    private Date purchaseDate;

    private Date lastSessionDate;

    private Boolean completed;

    private Boolean active;
}