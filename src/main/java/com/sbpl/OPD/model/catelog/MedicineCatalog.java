package com.sbpl.OPD.model.catelog;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.Setter;

/**
 * This Is a medicine catalog Table .
 *
 * @author Kousik Manik
 */
@Entity
@Table(
        name = "medicine_catalog",
        schema = "sb_opd",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_medicine_name_brand_strength_form",
                        columnNames = {"name", "brand_name", "strength", "form"}
                )
        },
        indexes = {
                @Index(name = "idx_medicine_active", columnList = "is_active"),
                @Index(name = "idx_medicine_name_active", columnList = "name, is_active"),
                @Index(name = "idx_medicine_name", columnList = "name")
        }
)
@Getter
@Setter
public class MedicineCatalog extends BaseEntity {

    private String name;

    private String form;

    @Column(name = "brand_name")
    private String brandName;

    private String strength;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;
}

