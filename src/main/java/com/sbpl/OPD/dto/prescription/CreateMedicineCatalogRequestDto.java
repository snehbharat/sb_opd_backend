package com.sbpl.OPD.dto.prescription;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * This Is An Create Medicine Catalog Request Dto.
 *
 * @author Kousik Manik
 */
@Data
public class CreateMedicineCatalogRequestDto {

  @NotBlank(message = "Medicine name is required")
  @Size(max = 255, message = "Medicine name must not exceed 255 characters")
  private String name;

  @NotBlank(message = "Medicine form is required")
  @Size(max = 100, message = "Medicine form must not exceed 100 characters")
  private String form;

  @NotBlank(message = "Medicine strength is required")
  @Size(max = 100, message = "Medicine strength must not exceed 100 characters")
  @Pattern(
      regexp = "^[0-9]+(\\.[0-9]+)?\\s?(mg|g|ml|mcg|IU)$",
      message = "Strength must be in valid format (e.g., 500 mg, 5 ml)"
  )
  private String strength;

}
