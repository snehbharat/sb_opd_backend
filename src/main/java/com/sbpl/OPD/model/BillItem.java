package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Represents an individual bill item in the Hospital Management System.
 *
 * Each bill item corresponds to a specific charge such as a medical service,
 * consultation fee, laboratory test, or medication.
 *
 * Bill items are linked to a parent bill and contribute to the total
 * amount of the bill.
 *
 * Database indexes are used to improve performance for bill-wise
 * and item name–based queries.
 *
 * @author Rahul Kumar
 */

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor

@Table(name = "bill_items", schema = "sb_opd", indexes = {
        @Index(name = "idx_bill_item_bill_id", columnList = "bill_id"),
        @Index(name = "idx_bill_item_bill_name", columnList = "bill_id, item_name"),
        @Index(name = "idx_bill_item_name", columnList = "item_name")})
public class BillItem extends BaseEntity {


    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "bill_id", nullable = false)
    private Bill bill;

    private String itemName;

    private String itemDescription;

    private Long treatmentPackageId;

    private Integer quantity;

    private BigDecimal unitPrice;

    private BigDecimal totalPrice;
}