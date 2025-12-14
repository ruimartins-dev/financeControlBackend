package com.example.financialcontrol.dto;

import com.example.financialcontrol.entity.TransactionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 50, message = "Category name must be at most 50 characters")
    private String name;

    @Size(max = 255, message = "Description must be at most 255 characters")
    private String description;

    @Size(max = 7, message = "Color must be at most 7 characters (e.g., #FFFFFF)")
    private String color;

    @Size(max = 50, message = "Icon must be at most 50 characters")
    private String icon;

    @NotNull(message = "Transaction type is required")
    private TransactionType type;
}

