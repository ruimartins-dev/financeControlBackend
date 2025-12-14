package com.example.financialcontrol.config;

import com.example.financialcontrol.service.CategoryService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class DataInitializer implements CommandLineRunner {

    private final CategoryService categoryService;

    public DataInitializer(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @Override
    public void run(String... args) {
        // Initialize default categories and subcategories
        categoryService.initializeDefaultCategories();
    }
}

