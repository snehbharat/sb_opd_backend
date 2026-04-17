package com.sbpl.OPD.dto.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * This Is An Create Test Type Request Dto.
 *
 * @author Kousik Manik
 */
@Data
public class CreateTestTypeRequestDto {

  @NotBlank(message = "Test name is required")
  @Size(max = 150, message = "Test name must not exceed 150 characters")
  private String name;

  @NotBlank(message = "description is required")
  @Size(max = 100, message = "description must not exceed 100 characters")
  private String description;

}
