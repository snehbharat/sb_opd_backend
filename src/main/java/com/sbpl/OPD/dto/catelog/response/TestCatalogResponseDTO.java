package com.sbpl.OPD.dto.catelog.response;

import lombok.Getter;
import lombok.Setter;

/**
 * Response DTO for test catalog data.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class TestCatalogResponseDTO {

    private Long id;
    private String name;
    private String category;
    private boolean isActive;
}
