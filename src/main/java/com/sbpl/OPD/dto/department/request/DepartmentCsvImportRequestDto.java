package com.sbpl.OPD.dto.department.request;

import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
public class DepartmentCsvImportRequestDto {

    @NotNull(message = "Company ID is required")
    private Long companyId;

    @NotNull(message = "Branch ID is required")
    private Long branchId;

    private MultipartFile csvFile;
}