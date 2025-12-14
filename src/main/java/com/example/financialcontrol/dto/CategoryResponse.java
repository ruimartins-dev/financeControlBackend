package com.example.financialcontrol.dto;

import com.example.financialcontrol.entity.Category;
import com.example.financialcontrol.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryResponse {

    private Long id;
    private String name;
    private String description;
    private String color;
    private String icon;
    private TransactionType type;
    private boolean isDefault;
    private LocalDateTime createdAt;
    private List<SubcategoryResponse> subcategories;

    public static CategoryResponse fromEntity(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        response.setDescription(category.getDescription());
        response.setColor(category.getColor());
        response.setIcon(category.getIcon());
        response.setType(category.getType());
        response.setDefault(category.isDefault());
        response.setCreatedAt(category.getCreatedAt());
        return response;
    }

    public static CategoryResponse fromEntityWithSubcategories(Category category) {
        CategoryResponse response = fromEntity(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            response.setSubcategories(
                category.getSubcategories().stream()
                    .map(SubcategoryResponse::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        return response;
    }

    public static CategoryResponse fromEntityWithFilteredSubcategories(Category category, Long userId) {
        CategoryResponse response = fromEntity(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            response.setSubcategories(
                category.getSubcategories().stream()
                    .filter(s -> s.isDefault() || (s.getUser() != null && s.getUser().getId().equals(userId)))
                    .map(SubcategoryResponse::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        return response;
    }

    public static CategoryResponse fromEntityWithFilteredSubcategories(Category category, Long userId, Set<Long> hiddenSubcategoryIds) {
        CategoryResponse response = fromEntity(category);
        if (category.getSubcategories() != null && !category.getSubcategories().isEmpty()) {
            response.setSubcategories(
                category.getSubcategories().stream()
                    .filter(s -> s.isDefault() || (s.getUser() != null && s.getUser().getId().equals(userId)))
                    .filter(s -> !hiddenSubcategoryIds.contains(s.getId()))
                    .map(SubcategoryResponse::fromEntity)
                    .collect(Collectors.toList())
            );
        }
        return response;
    }
}

