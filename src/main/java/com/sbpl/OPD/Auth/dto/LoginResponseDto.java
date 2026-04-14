package com.sbpl.OPD.Auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * This is a Login Response Dto.
 *
 * @author Kousik Manik
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginResponseDto {

  private String token;

  private String refreshToken;

  private Object loginDetails;

}