package com.sbpl.OPD.dto.appointment;

import com.sbpl.OPD.dto.appointment.PackageInputDTO;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TreatmentPackageBulkRequest {
    private Long treatmentId;
    private List<PackageInputDTO> packages;
}