package com.sbpl.OPD.dto.company.response;

import lombok.Getter;
import lombok.Setter;

import java.util.Date;

/**
 * Company Profile response DTO.
 *
 * Used for returning company details
 * in API responses.
 *
 * @author rahul kumar
 */
@Getter
@Setter
public class CompanyProfileResponseDto {

    private Long companyId;

    private String companyName;
    private String companyLogoUrl;
    private String logoBase64;
    private String companyUrl;
    private String address;

    private String gstinNumber;
    private String cinNumber;
    private String dlNo;
    private String registeredOffice;

    private String companyPhone;
    private String companyEmail;
    private String companyAlternateNo;

    private Long adminUserId;

    private Boolean active;

    private Date createdAt;
    private Date updatedAt;
}
