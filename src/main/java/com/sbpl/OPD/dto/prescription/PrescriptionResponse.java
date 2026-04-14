package com.sbpl.OPD.dto.prescription;

import lombok.Data;

import java.util.Date;

/**
 * This Is A Prescription Response Dto.
 *
 * @author Kousik Manik
 */
@Data
public class PrescriptionResponse {
  private String requestId;
  private Long prescriptionId;
  private Integer versionNo;
  private Date createdAt;
  private String status;
}
