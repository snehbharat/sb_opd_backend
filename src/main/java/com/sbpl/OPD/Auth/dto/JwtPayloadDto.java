package com.sbpl.OPD.Auth.dto;

import lombok.Data;

@Data
public class JwtPayloadDto {
    private String fullName;
    private String mobileNumber;
    private String userName;
    private String tokenType;
    private String role;
    private Boolean active;
    private Long userId;
}
