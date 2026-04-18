package com.sbpl.OPD.dto.catelog.response;

import lombok.Data;

/**
 * Response DTO for Test Category.
 *
 * @author Rahul Kumar
 */
@Data
public class TestCategoryResponseDTO {

    private Long id;
    private String categoryName;
    private String categoryDescription;
    private Boolean isActive;
}
