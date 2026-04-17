package com.sbpl.OPD.dto.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * This Is A Create Test Catalog Request DTO.
 *
 * @author Kousik Manik
 */
@Data
public class CreateTestCatalogRequestDto {

  @NotBlank(message = "Test name is required")
  @Size(max = 150, message = "Test name must not exceed 150 characters")
  private String name;

  @NotBlank(message = "Category is required")
  @Size(max = 100, message = "Category must not exceed 100 characters")
  private String category;

  @NotNull(message = "Type ID is required")
  @Positive(message = "Type ID must be a positive number")
  private Long typeId;

}
