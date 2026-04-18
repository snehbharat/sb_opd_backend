package com.sbpl.OPD.repository.catelog;

import com.sbpl.OPD.model.catelog.TestCategory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TestCategoryRepo extends JpaRepository<TestCategory, Long> {
    
    boolean existsByCategoryNameIgnoreCase(String categoryName);

    boolean existsByCategoryNameIgnoreCaseAndIdNot(String categoryName, Long id);

    Page<TestCategory> findByCategoryNameContainingIgnoreCase(String search, Pageable pageable);
}
