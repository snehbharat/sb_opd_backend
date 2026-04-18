package com.sbpl.OPD.dto.catelog.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Request DTO for creating/updating a Test Category.
 *
 * @author Rahul Kumar
 */
@Data
public class TestCategoryRequestDTO {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name must be at most 100 characters")
    private String categoryName;

    @Size(max = 500, message = "Category description must be at most 500 characters")
    private String categoryDescription;
}
