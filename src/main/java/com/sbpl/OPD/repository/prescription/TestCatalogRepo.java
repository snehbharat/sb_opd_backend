package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.TestCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
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

  boolean existsByNameIgnoreCaseAndFormIgnoreCaseAndStrengthIgnoreCase(
      String name,
      String form,
      String strength
  );


  boolean existsByName(String name);

  java.util.Optional<TestCatalog> findByName(String name);

  Page<TestCatalog> findByNameContainingIgnoreCase(String search, Pageable pageable);

  Page<TestCatalog> findByCategoryIgnoreCase(String category, Pageable pageable);

  Page<TestCatalog> findByIsActiveTrueAndCategoryIgnoreCase(String category, Pageable pageable);

  Page<TestCatalog> findByIsActiveTrueAndCategoryContainingIgnoreCase(String radiology, PageRequest pageRequest);

  Page<TestCatalog> findByNameContainingIgnoreCaseAndCategoryContainingIgnoreCase(String search, String category, Pageable pageable);

}
