package com.sbpl.OPD.dto.catelog.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

/**
 * Request DTO for updating a test catalog entry.
 *
 * @author Rahul Kumar
 */
@Getter
@Setter
public class TestCatalogUpdateDTO {

    @NotBlank(message = "Test name is required")
    @Size(max = 150, message = "Test name must not exceed 150 characters")
    private String name;

    @NotBlank(message = "Category is required")
    @Size(max = 100, message = "Category must not exceed 100 characters")
    private String category;
}
