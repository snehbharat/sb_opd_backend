package com.sbpl.OPD.dto.customer.response;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * Customer response DTO.
 *
 * Used for sending customer details in API responses.
 *
 * @author Rahul Kumar
 */
@Data
public class CustomerResponseDto {

    private Long id;

    private String prefix;

    private String firstName;

    private String lastName;

    private String phoneNumber;

    private String email;

    private Long age;

    private String dateOfBirth;

    private String address;

    private String gender;
    
    private String uhid;

    private Long companyId;
    private String companyName;

    private Long branchId;
    private String branchName;
    
    private Long departmentId;
    private String departmentName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
