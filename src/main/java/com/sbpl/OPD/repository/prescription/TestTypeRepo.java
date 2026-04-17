package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.TestType;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This iS a Test Type Repository Class.
 *
 * @author Kousik Manik
 */
@Repository
public interface TestTypeRepo extends JpaRepository<TestType, Long> {

  boolean existsByNameIgnoreCaseAndIsActiveTrue(String name);

  List<TestType> findByIsActiveTrue();

  Page<TestType> findByIsActiveTrue(Pageable pageable);

}
