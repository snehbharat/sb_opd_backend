package com.sbpl.OPD.dto.prescription;


import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is a Test Dto.
 *
 * @author Kousik Manik
 */
@Getter
@Setter
public class TestDto {

  private Long testId;

  @NotNull(message = "name cannot be Null")
  @NotBlank(message = "name cannot be Blank")
  private String name;

  private String instructions;
}


