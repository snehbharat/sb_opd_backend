package com.sbpl.OPD.dto.department;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

/**
 * DTO for department response data.
 *
 * @author Rahul Kumar
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentResponseDto {

    private Long id;
    private String departmentName;
    private Long branchId;
    private Long clinicId;
    private String branchName;
    private String description;
    private Date createdAt;
    private Date updatedAt;
}