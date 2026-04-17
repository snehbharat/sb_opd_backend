package com.sbpl.OPD.model.catelog;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

@Entity
@Getter
@Setter
@Table(
        name = "symptom",
        schema = "sb_opd",
        indexes = {
                @Index(name = "idx_symptom_name", columnList = "name"),
                @Index(name = "idx_symptom_name_unique", columnList = "name", unique = true)
        }
)
public class Symptom {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}