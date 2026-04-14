package com.sbpl.OPD.dto.branch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO for branch creation and update requests.
 *
 * @author Rahul Kumar
 */
@Data
public class BranchRequestDto {

    @NotBlank(message = "Branch name is required")
    private String branchName;

    @Size(max = 1000, message = "Address cannot exceed 1000 characters")
    private String address;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[0-9]{10,15}$",
            message = "Phone number must contain 10 to 15 digits"
    )
    private String phoneNumber;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    private String email;

    private String establishedDate;

    @NotNull(message = "Clinic ID is required")
    private Long clinicId;
}