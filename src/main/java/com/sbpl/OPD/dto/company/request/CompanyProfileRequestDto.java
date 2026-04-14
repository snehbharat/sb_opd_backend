package com.sbpl.OPD.dto.company.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

/**
 * Company Profile request DTO.
 *
 * Used for creating and updating healthcare
 * company (tenant / organization) details.
 *
 * @author rahul kumar
 */
@Getter
@Setter
public class CompanyProfileRequestDto {

    @NotBlank
    @Size(max = 150)
    private String companyName;

    private MultipartFile companyLogo;

//    @Pattern(
//        regexp = "^(https?://)?([\\w.-]+)+(:\\d+)?(/.*)?$",
//        message = "Invalid company URL"
//    )
//    private String companyUrl;

    @NotBlank
    private String address;

    @Pattern(
        regexp = "^[0-9]{2}[A-Z]{5}[0-9]{4}[A-Z]{1}[1-9A-Z]{1}Z[0-9A-Z]{1}$",
        message = "Invalid GSTIN"
    )
    private String gstinNumber;

    @Pattern(
        regexp = "^[A-Z]{1}[0-9]{5}[A-Z]{2}[0-9]{4}[A-Z]{3}[0-9]{6}$",
        message = "Invalid CIN number"
    )
    private String cinNumber;

    @NotBlank
    private String dlNo;

    private String registeredOffice;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid mobile number")
    private String companyPhone;

    @Email
    @NotBlank
    private String companyEmail;

    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Invalid alternate number")
    private String companyAlternateNo;



}