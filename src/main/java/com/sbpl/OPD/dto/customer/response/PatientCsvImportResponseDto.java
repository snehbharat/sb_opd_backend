package com.sbpl.OPD.dto.customer.response;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class PatientCsvImportResponseDto {

    private int totalProcessed;
    private int successfulImports;
    private int failedImports;
    private List<String> errors;
    private List<Long> importedPatientIds;
}