package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.TestCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is A Test Catalog Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface TestCatalogRepo extends JpaRepository<TestCatalog, Long> {
}
