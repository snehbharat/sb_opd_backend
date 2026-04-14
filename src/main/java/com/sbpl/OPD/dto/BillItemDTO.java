package com.sbpl.OPD.dto;

import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class BillItemDTO {
    private Long id;
    private Long billId;
    private String itemName;
    private String itemDescription;
    private Long treatmentPackageId;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal totalPrice;
}