package com.sbpl.OPD.dto.customer.response;

import com.sbpl.OPD.dto.treatment.pkg.PackageUsageInfoDTO;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@Setter
public class CustomerWithPackageUsageDTO {
    private Long id;
    private String prefix;
    private String firstName;
    private String lastName;
    private String phoneNumber;
    private String email;
    private LocalDate dateOfBirth;
    private Long age;
    private String address;
    private String gender;
    private String uhid;
    private Long companyId;
    private Long branchId;
    private Long departmentId;
    private BigDecimal totalPaidAmount;
    private BigDecimal totalDueAmount;
    private BigDecimal totalBillAmount;
    private PackageUsageInfoDTO packageUsageInfo;
}