package com.sbpl.OPD.repository.prescription;

import com.sbpl.OPD.model.prescription.MedicineCatalog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is An Medicine Catalog Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface MedicineCatalogRepo extends JpaRepository<MedicineCatalog, Long> {
}
