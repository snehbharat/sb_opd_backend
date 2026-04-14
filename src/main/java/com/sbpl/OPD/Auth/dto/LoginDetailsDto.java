package com.sbpl.OPD.Auth.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class LoginDetailsDto {

  private String userId;
  private String username;
  private String fullName;
  private String email;
  private String role;
  private Boolean active;

}
