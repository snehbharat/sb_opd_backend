package com.sbpl.OPD.dto.branch;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for branch response data.
 *
 * @author HMS Team
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BranchResponseDto {

    private Long id;
    private String branchName;
    private String address;
    private String phoneNumber;
    private String email;
    private String establishedDate;
    private Long clinicId;
    private String clinicName;
    private Date createdAt;
    private Date updatedAt;
}