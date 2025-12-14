package com.example.financialcontrol.controller;

import com.example.financialcontrol.dto.*;
import com.example.financialcontrol.entity.TransactionType;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.service.CategoryService;
import com.example.financialcontrol.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/categories")
public class CategoryController {

    private final CategoryService categoryService;
    private final UserService userService;

    public CategoryController(CategoryService categoryService, UserService userService) {
        this.categoryService = categoryService;
        this.userService = userService;
    }

    // ==================== CATEGORY ENDPOINTS ====================

    /**
     * Get all categories available to the authenticated user
     */
    @GetMapping
    public ResponseEntity<List<CategoryResponse>> getAllCategories(
            @RequestParam(required = false) TransactionType type,
            Authentication auth) {
        User user = userService.findByUsername(auth.getName());

        List<CategoryResponse> categories;
        if (type != null) {
            categories = categoryService.getCategoriesForUserByType(user, type);
        } else {
            categories = categoryService.getAllCategoriesForUser(user);
        }

        return ResponseEntity.ok(categories);
    }

    /**
     * Get a single category by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<?> getCategoryById(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            return ResponseEntity.ok(categoryService.getCategoryById(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create a new category for the authenticated user
     */
    @PostMapping
    public ResponseEntity<?> createCategory(@Valid @RequestBody CategoryRequest request, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            CategoryResponse response = categoryService.createCategory(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update a category owned by the authenticated user
     */
    @PutMapping("/{id}")
    public ResponseEntity<?> updateCategory(
            @PathVariable Long id,
            @Valid @RequestBody CategoryRequest request,
            Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            CategoryResponse response = categoryService.updateCategory(id, request, user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a category owned by the authenticated user
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteCategory(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            categoryService.deleteCategory(id, user);
            return ResponseEntity.ok(ApiResponse.success("Category deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== SUBCATEGORY ENDPOINTS ====================

    /**
     * Get all subcategories for a specific category
     */
    @GetMapping("/{categoryId}/subcategories")
    public ResponseEntity<?> getSubcategoriesByCategory(@PathVariable Long categoryId, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            List<SubcategoryResponse> subcategories = categoryService.getSubcategoriesForCategory(categoryId, user);
            return ResponseEntity.ok(subcategories);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Get a single subcategory by ID
     */
    @GetMapping("/subcategories/{id}")
    public ResponseEntity<?> getSubcategoryById(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            return ResponseEntity.ok(categoryService.getSubcategoryById(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Create a new subcategory for a category
     */
    @PostMapping("/subcategories")
    public ResponseEntity<?> createSubcategory(@Valid @RequestBody SubcategoryRequest request, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            SubcategoryResponse response = categoryService.createSubcategory(request, user);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Update a subcategory owned by the authenticated user
     */
    @PutMapping("/subcategories/{id}")
    public ResponseEntity<?> updateSubcategory(
            @PathVariable Long id,
            @Valid @RequestBody SubcategoryRequest request,
            Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            SubcategoryResponse response = categoryService.updateSubcategory(id, request, user);
            return ResponseEntity.ok(response);
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Delete a subcategory owned by the authenticated user
     */
    @DeleteMapping("/subcategories/{id}")
    public ResponseEntity<?> deleteSubcategory(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            categoryService.deleteSubcategory(id, user);
            return ResponseEntity.ok(ApiResponse.success("Subcategory deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // ==================== HIDDEN/RESTORE ENDPOINTS ====================

    /**
     * Get all hidden categories for the authenticated user
     */
    @GetMapping("/hidden")
    public ResponseEntity<List<CategoryResponse>> getHiddenCategories(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(categoryService.getHiddenCategoriesForUser(user));
    }

    /**
     * Get all hidden subcategories for the authenticated user
     */
    @GetMapping("/subcategories/hidden")
    public ResponseEntity<List<SubcategoryResponse>> getHiddenSubcategories(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(categoryService.getHiddenSubcategoriesForUser(user));
    }

    /**
     * Restore a hidden category for the authenticated user
     */
    @PostMapping("/{id}/restore")
    public ResponseEntity<?> restoreCategory(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            categoryService.restoreCategory(id, user);
            return ResponseEntity.ok(ApiResponse.success("Category restored successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    /**
     * Restore a hidden subcategory for the authenticated user
     */
    @PostMapping("/subcategories/{id}/restore")
    public ResponseEntity<?> restoreSubcategory(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            categoryService.restoreSubcategory(id, user);
            return ResponseEntity.ok(ApiResponse.success("Subcategory restored successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

