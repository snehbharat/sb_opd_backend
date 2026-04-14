package com.sbpl.OPD.dto.treatment.pkg;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class PatientBillWithPackageDTO {

    private Long billId;

    private String billNumber;

    private Date createdAt;

    private BigDecimal totalAmount;

    private BigDecimal paidAmount;

    private BigDecimal balanceAmount;

    private String status;

    private String paymentType;

    private List<PatientBillItemWithPackageDTO> billItems;

    private PackageUsageInfoDTO packageUsage;

    @Getter
    @Setter
    public static class PatientBillItemWithPackageDTO {
        private Long itemId;
        private String itemName;
        private String itemDescription;
        private Integer quantity;
        private BigDecimal unitPrice;
        private BigDecimal totalPrice;
        private Long treatmentPackageId;
        private String packageName;
        private Integer packageSessions;
    }

}