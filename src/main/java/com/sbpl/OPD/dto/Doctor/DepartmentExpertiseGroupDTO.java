package com.sbpl.OPD.dto.Doctor;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * DTO for grouping core expertise by department.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class DepartmentExpertiseGroupDTO {

    private String departmentName;
    private Long expertiseCount;
    private Long totalDoctorCount;
    private List<DoctorCoreExpertiseResponseDTO> expertiseList;
}
