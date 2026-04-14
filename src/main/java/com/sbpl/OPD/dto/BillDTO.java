package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.BillStatus;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

@Getter
@Setter
public class BillDTO {
    private Long id;
    private Long patientId;
    private BigDecimal patientPaidAmount;
    private BigDecimal patientDueAmount;
    private BigDecimal patientBillAmount;
    private Long billingStaffId;
    private Long appointmentId;
    private BillStatus status;
    private String paymentType;
    private String couponCode;
    private BigDecimal couponAmount;
    private List<BillItemDTO> billItems;
    private BigDecimal totalAmount;
    private BigDecimal paidAmount;
    private BigDecimal balanceAmount;
    private Date createdAt;
    private Date updatedAt;
    private LocalDateTime paymentDate;
    private String notes;
    private String patientName;
    private String billingStaffName;
    private String billNumber;
    private BigDecimal previousDueAmount;
    
    // Company and Branch information
    private Long companyId;
    private String companyName;
    private Long branchId;
    private String branchName;
}