package com.sbpl.OPD.dto.prescription;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is An Advice Dto.
 *
 * @author Kousik Manik
 */
@Getter
@Setter
public class AdviceDto {
  @NotBlank(message = "advice cannot be blank")
  private String advice;
}


