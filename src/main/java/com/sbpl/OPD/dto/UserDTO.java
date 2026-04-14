package com.sbpl.OPD.dto;

import com.sbpl.OPD.Auth.enums.UserRole;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserDTO {
    private Long id;
    
    @NotBlank(message = "Username is required")
    @Size(min = 3, max = 50, message = "Username must be between 3 and 50 characters")
    private String username;
    
    @NotBlank(message = "Email is required")
    @Email(message = "Email should be valid")
    private String email;
    
    private String firstName;
    
    private String lastName;
    
    private String phoneNumber;
    
    @NotNull(message = "Role is required")
    private UserRole role;
    
    private String dateOfBirth;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;
    
    private String gender;
    private String address;
    private String employeeId;
    private String department;
    private Boolean isActive;

    // Company and branch associations for role-based access
    private Long companyId;
    private Long branchId;

    private String companyName;
    private String branchName;
}