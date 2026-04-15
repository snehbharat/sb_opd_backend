package com.sbpl.OPD.dto.customer.response;

import com.sbpl.OPD.dto.treatment.pkg.PackageUsageInfoDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public interface CustomerListView {

    Long getId();

    String getPrefix();

    String getFirstName();

    String getLastName();

    String getPhoneNumber();

    String getEmail();

    LocalDate getDateOfBirth();

    Long getAge();

    String getAddress();

    String getGender();
    
    String getUhid();

    Long getCompanyId();

    Long getBranchId();

    Long getDepartmentId();

    BigDecimal getTotalPaidAmount();
    BigDecimal getTotalDueAmount();
    BigDecimal getTotalBillAmount();

    String getLastAppointmentNumber();

    LocalDateTime getLastAppointmentDate();

    String getLastDoctorName();

    Long getLastDoctorId();

    PackageUsageInfoDTO packageUsageInfo = null;
}
