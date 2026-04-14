package com.sbpl.OPD.dto.prescription;


import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

/**
 * Data Transfer Object for creating a new medical prescription.
 * <p>
 * This DTO captures all necessary details including the patient, doctor,
 * diagnosis, and lists of prescribed items (medicines, tests, etc.).
 * </p>
 *
 * @author Kousik Manik
 */
@Getter
@Setter
public class PrescriptionCreateRequestDto {

  @NotNull(message = "prescriptionName cannot be null")
  @NotBlank(message = "prescriptionName cannot be blank")
  @NotEmpty(message = "prescriptionName cannot be empty")
  private String prescriptionName;

  @NotNull(message = "prescriptionDescription cannot be null")
  @NotBlank(message = "prescriptionDescription cannot be blank")
  @NotEmpty(message = "prescriptionDescription cannot be empty")
  private String prescriptionDescription;

  @NotNull(message = "documentLinked cannot be null")
  private Boolean documentLinked;

  /**
   * The unique identifier of the clinic where the prescription is issued.
   */
  @NotNull(message = "Clinic ID is required")
  @Positive(message = "Clinic ID must be a positive number")
  private Long clinicId;

  /**
   * The unique identifier of the doctor issuing the prescription.
   */
  @NotNull(message = "Doctor ID is required")
  @Positive(message = "Doctor ID must be a positive number")
  private Long doctorId;

  /**
   * The unique identifier of the patient receiving the prescription.
   */
  @NotNull(message = "Patient ID is required")
  @Positive(message = "Patient ID must be a positive number")
  private Long patientId;

  /**
   * The doctor's diagnosis or observation.
   * Maximum length is restricted to prevent database overflow.
   */
  @NotBlank(message = "Diagnosis cannot be blank")
  @Size(max = 2000, message = "Diagnosis description cannot exceed 2000 characters")
  private String diagnosis;

  /**
   * List of medicines prescribed.
   * Can be empty, but must not be null.
   */
  @NotNull(message = "Medicines list cannot be null (send empty list if none)")
  @Valid
  private List<MedicineDto> medicines;

  /**
   * List of medical tests prescribed.
   * Can be empty, but must not be null.
   */
  @NotNull(message = "Tests list cannot be null (send empty list if none)")
  @Valid
  private List<TestDto> tests;

  /**
   * List of general advice or instructions for the patient.
   * Can be empty, but must not be null.
   */
  @NotNull(message = "Advice list cannot be null (send empty list if none)")
  @Valid
  private List<AdviceDto> advice;

  /**
   * List of internal or private notes regarding this prescription.
   * Can be empty, but must not be null.
   */
  @NotNull(message = "Notes list cannot be null (send empty list if none)")
  @Valid
  private List<NoteDto> notes;
}
