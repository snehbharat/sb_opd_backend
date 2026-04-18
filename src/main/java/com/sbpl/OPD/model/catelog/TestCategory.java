package com.sbpl.OPD.model.catelog;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "test_category",
        schema = "sb_opd",
        indexes = {
                @Index(name = "uk_test_category_name", columnList = "category_name", unique = true)
        }
)
public class TestCategory extends BaseEntity {

    @Column(name = "category_name",nullable = false)
    private String categoryName;

    @Column(name = "category_description")
    private String categoryDescription;

    @Column(name = "is_active")
    private Boolean isActive;
}
