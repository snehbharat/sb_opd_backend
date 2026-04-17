package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.TestCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Test Catalog Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface TestCatalogRepo extends JpaRepository<TestCatalog, Long> {

  Page<TestCatalog> findByIsActiveTrue(Pageable pageable);

  Page<TestCatalog> findByIsActiveTrueAndNameContainingIgnoreCase(
      String name,
      Pageable pageable
  );

  boolean existsByNameIgnoreCaseAndCategoryIgnoreCaseAndTypeId(
      String name,
      String category,
      Long typeId
  );

  Page<TestCatalog> findByIsActiveTrueAndTypeId(
      Long typeId,
      Pageable pageable
  );

  Page<TestCatalog> findByIsActiveTrueAndTypeIdAndNameContainingIgnoreCase(
      Long typeId,
      String name,
      Pageable pageable
  );

}
