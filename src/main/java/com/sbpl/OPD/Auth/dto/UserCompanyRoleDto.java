package com.sbpl.OPD.Auth.dto;

import com.sbpl.OPD.Auth.enums.UserRole;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserCompanyRoleDto {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private UserRole role;
    private Boolean isActive;
    private String dateOfBirth;
    private String gender;
    private String address;
    private String employeeId;
    private String department;
    private SimpleCompanyDto company;
    private SimpleBranchDto branch;
}