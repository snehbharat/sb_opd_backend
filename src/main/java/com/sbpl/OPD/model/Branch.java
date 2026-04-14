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

@Getter
@Setter
@Entity
@Table(
        name = "branches",
        schema = "sb_opd",
        indexes = {
                @Index(name = "idx_branch_clinic", columnList = "clinic_id"),
                @Index(name = "idx_branch_name", columnList = "branch_name"),
                @Index(name = "idx_branch_phone", columnList = "phone_number"),
                @Index(name = "idx_branch_email", columnList = "email")
        }
)
public class Branch extends BaseEntity {

    @Column(name = "branch_name", nullable = false)
    private String branchName;

    @Column(name = "address", columnDefinition = "TEXT")
    private String address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Column(name = "email")
    private String email;

    @Column(name = "established_date")
    private String establishedDate;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinic_id", nullable = false)
    private CompanyProfile clinic;

}