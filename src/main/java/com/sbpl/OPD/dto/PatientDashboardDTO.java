package com.sbpl.OPD.dto;

import lombok.Data;

import java.util.List;

@Data
public class PatientDashboardDTO {
    private String patientName;
    private String patientEmail;
    private Long totalAppointments;
    private Long upcomingAppointments;
    private Long completedAppointments;
    private Long medicalRecords;
    private Long totalBills;
    private Long paidBills;
    private List<AppointmentDTO> upcomingAppointmentsList;
    private List<MedicalRecordDTO> recentMedicalRecords;
    private List<BillDTO> recentBills;
}