package com.sbpl.OPD.model;


import com.sbpl.OPD.Entity.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

/**
 * Represents a core expertise area for doctors in the Hospital Management System.
 *
 * This entity stores the main areas of medical expertise that doctors can specialize in.
 * Each doctor is associated with one primary core expertise.
 *
 * @author Rahul Kumar
 */

@Entity
@Getter
@Setter
@Table(name = "doctor_core_expertise", schema = "sb_opd")
public class DoctorCoreExpertise extends BaseEntity {

    @Column(name = "expertise_name", nullable = false, unique = true, length = 100)
    private String expertiseName;

    @Column(name = "department_name", length = 100)
    private String departmentName;

}
