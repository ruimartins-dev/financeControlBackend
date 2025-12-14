package com.example.financialcontrol.dto;

import com.example.financialcontrol.entity.Subcategory;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubcategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private Long categoryId;
    private String categoryName;
    private boolean isDefault;
    private LocalDateTime createdAt;

    public static SubcategoryResponse fromEntity(Subcategory subcategory) {
        SubcategoryResponse response = new SubcategoryResponse();
        response.setId(subcategory.getId());
        response.setName(subcategory.getName());
        response.setDescription(subcategory.getDescription());
        response.setColor(subcategory.getColor());
        response.setIcon(subcategory.getIcon());
        response.setCategoryId(subcategory.getCategory().getId());
        response.setCategoryName(subcategory.getCategory().getName());
        response.setDefault(subcategory.isDefault());
        response.setCreatedAt(subcategory.getCreatedAt());
        return response;
    }
}

