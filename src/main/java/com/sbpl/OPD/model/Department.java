package com.sbpl.OPD.model;

import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Entity representing a Department in a Branch.
 * Each department belongs to one Branch and is used to
 * organize services or units within the organization.
 * Table: aestheticq.departments
 * Indexed on branch_id and department_name.
 *
 * @author Rahul Kumar
 */

@Getter
@Setter
@Entity
@Table(
        name = "departments",
        schema = "sb_opd",
        indexes = {
                @Index(name = "idx_department_branch", columnList = "branch_id"),
                @Index(name = "idx_department_name", columnList = "department_name")
        }
)
public class Department extends BaseEntity {

    @Column(name = "department_name", nullable = false)
    private String departmentName;

    @Column(name = "description")
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id", nullable = false)
    private Branch branch;

}