package com.sbpl.OPD.dto.prescription;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Data Transfer Object representing a single medicine within a prescription.
 * <p>
 * Encapsulates the medicine details, dosage instructions, duration,
 * and specific timing for administration.
 * </p>
 *
 * @author Kousik Manik
 */
@Getter
@Setter
public class MedicineDto {

  private Long medicineId;

  @NotBlank(message = "medicine name is required")
  private String name;

  /**
   * The specific dosage amount (e.g., "500mg", "10ml").
   */
  @NotBlank(message = "Dosage is required")
  @Size(max = 100, message = "Dosage description cannot exceed 100 characters")
  private String dosage;

  /**
   * The duration for which the medicine should be taken (in days).
   */
  @NotNull(message = "Duration is required")
  @Positive(message = "Duration must be at least 1 day")
  @Max(value = 365, message = "Duration cannot exceed 365 days")
  private Integer durationDays;

  /**
   * The timing schedule for the medicine (e.g., "Morning, Night", "1-0-1", "After Food").
   */
  @NotBlank(message = "Timing information is required (e.g., 'Before Food', '1-0-1')")
  @Size(max = 100, message = "Timing description cannot exceed 100 characters")
  private String timing;

  /**
   * Specific instructions for administration (e.g., "Shake well before use").
   * This field is optional.
   */
  @Size(max = 500, message = "Instructions cannot exceed 500 characters")
  private String instructions;
}
