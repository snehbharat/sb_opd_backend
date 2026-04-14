package com.sbpl.OPD.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * Payment Dto.
 *
 * @author Kousik Manik
 */
@Data
@AllArgsConstructor
public class PaymentTypeDTO {
  private String value;
  private String label;
}
