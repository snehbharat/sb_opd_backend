package com.sbpl.OPD.repository.catelog;

import com.sbpl.OPD.model.catelog.Symptom;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository interface for Symptom entity operations.
 * Provides CRUD operations and custom query methods for symptom management.
 *
 * @author Rahul Kumar
 */
@Repository
public interface SymptomRepo extends JpaRepository<Symptom, Long> {

    Optional<Symptom> findByName(String name);

    boolean existsByName(String name);

    Page<Symptom> findAll(Pageable pageable);

    Page<Symptom> findByNameContainingIgnoreCase(String search, Pageable pageable);
}
