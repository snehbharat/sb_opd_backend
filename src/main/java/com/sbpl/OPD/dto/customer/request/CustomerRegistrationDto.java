package com.sbpl.OPD.dto.customer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer registration request DTO.
 *
 * Used for creating new customers.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class CustomerRegistrationDto {

    @Size(max = 10, message = "Prefix must not exceed 10 characters")
    private String prefix;

    @NotBlank(message = "First name is required")
    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @NotBlank(message = "Last name is required")
    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @NotBlank(message = "Phone number is required")
    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Phone number must be a valid 10-digit Indian mobile number"
    )
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    @NotNull(message = "Date of birth is required")
    private String dateOfBirth;

    private Long age;

    private String gender;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private Long departmentId;

}

