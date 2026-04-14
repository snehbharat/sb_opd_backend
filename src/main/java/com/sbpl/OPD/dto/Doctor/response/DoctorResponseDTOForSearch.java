package com.sbpl.OPD.dto.Doctor.response;

/**
 * Minimal projection of Doctor for appointment or search operations.
 * Interface-based projection works directly with Spring Data JPA queries.
 *
 * @author Rahul Kumar
 */
public interface DoctorResponseDTOForSearch {

    Long getId();

    String getPrefix();

    String getDoctorName();

    String getDoctorEmail();

    String getPhoneNumber();

    String getSpecialization();

    String getDepartment();
    Boolean getOnlineConsultationAvailable();
    Boolean getIsActive();

    String getConsultationRoom();

    String getRegistrationNumber();

    Long getCompanyId();

    Long getBranchId();
    
    Long getCoreExpertiseId();
    
    String getCoreExpertiseName();
    
    String getCoreExpertiseCategory();
    
    Integer getExperienceYears();
}