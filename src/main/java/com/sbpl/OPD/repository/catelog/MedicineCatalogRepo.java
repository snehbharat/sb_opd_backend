package com.sbpl.OPD.repository.catelog;

import com.sbpl.OPD.model.catelog.MedicineCatalog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * This Is An Medicine Catalog Repo.
 *
 * @author Kousik Manik
 */
@Repository
public interface MedicineCatalogRepo extends JpaRepository<MedicineCatalog, Long> {

    Page<MedicineCatalog> findByIsActiveTrue(Pageable pageable);

    Page<MedicineCatalog> findByIsActiveTrueAndNameContainingIgnoreCase(String name, Pageable pageable);

}
