package com.sbpl.OPD.dto.prescription;


import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is A Note Dto.
 *
 * @author Kousik Manik
 */
@Getter
@Setter
public class NoteDto {
  @NotBlank(message = "noteType cannot be blank")
  private String noteType;
  @NotBlank(message = "content cannot be blank")
  private String content;
}


