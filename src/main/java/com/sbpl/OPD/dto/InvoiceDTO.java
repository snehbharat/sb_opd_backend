package com.sbpl.OPD.dto;

import com.sbpl.OPD.enums.InvoiceStatus;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Data Transfer Object for invoice data.
 * Used for API communication and data exchange.
 */
@Data
public class InvoiceDTO {
    private Long id;
    private String invoiceNumber;
    private Long appointmentId;
    private Long patientId;
    private Long generatedById;
    private Long companyId;
    private Long branchId;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal discountAmount;
    private BigDecimal netAmount;
    private InvoiceStatus status;
    private LocalDateTime dueDate;
    private LocalDateTime issueDate;
    private String notes;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}