package com.sbpl.OPD.repository;

import com.sbpl.OPD.model.TreatmentCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;


@Repository
public interface TreatmentCategoryRepository
    extends JpaRepository<TreatmentCategory, Long> {

  Optional<TreatmentCategory> findByNameIgnoreCase(String name);

  List<TreatmentCategory> findByActiveTrue();
}
