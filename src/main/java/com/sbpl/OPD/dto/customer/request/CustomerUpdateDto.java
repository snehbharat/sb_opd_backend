package com.sbpl.OPD.dto.customer.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Customer update request DTO.
 *
 * Used for updating existing customers.
 * Has relaxed validation compared to registration DTO.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class CustomerUpdateDto {

    @Size(max = 10, message = "Prefix must not exceed 10 characters")
    private String prefix;

    @Size(max = 100, message = "First name must not exceed 100 characters")
    private String firstName;

    @Size(max = 100, message = "Last name must not exceed 100 characters")
    private String lastName;

    @Pattern(
            regexp = "^[6-9][0-9]{9}$",
            message = "Phone number must be a valid 10-digit Indian mobile number"
    )
    private String phoneNumber;

    @Email(message = "Invalid email format")
    private String email;

    private String dateOfBirth;

    private Long age;

    private String gender;

    @Size(max = 1000, message = "Address must not exceed 1000 characters")
    private String address;

    // Nullable for updates - users shouldn't be able to change company
    private Long companyId;

    // Nullable for updates - users can change branch within their company
    private Long branchId;

    private Long departmentId;

}