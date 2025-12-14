package com.example.financialcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryRequest {

    @NotBlank(message = "Subcategory name is required")
    @Size(max = 50, message = "Subcategory name must be at most 50 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Size(max = 7, message = "Color must be at most 7 characters (e.g., #FFFFFF)")
    private String color;

    @Size(max = 50, message = "Icon must be at most 50 characters")
    private String icon;

    @NotNull(message = "Category ID is required")
    private Long categoryId;
}

