package com.example.financialcontrol.repository;

import com.example.financialcontrol.entity.Subcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubcategoryRepository extends JpaRepository<Subcategory, Long> {

    // Find all subcategories for a specific category
    List<Subcategory> findByCategoryId(Long categoryId);

    // Find all default subcategories for a category
    List<Subcategory> findByCategoryIdAndIsDefaultTrue(Long categoryId);

    // Find all user subcategories for a category
    List<Subcategory> findByCategoryIdAndUserId(Long categoryId, Long userId);

    // Find all subcategories available to a user for a specific category (default + user's own)
    @Query("SELECT s FROM Subcategory s WHERE s.category.id = :categoryId AND (s.isDefault = true OR s.user.id = :userId)")
    List<Subcategory> findAllAvailableForUserByCategory(@Param("categoryId") Long categoryId, @Param("userId") Long userId);

    // Check if a subcategory name already exists for a category
    @Query("SELECT COUNT(s) > 0 FROM Subcategory s WHERE s.name = :name AND s.category.id = :categoryId AND (s.isDefault = true OR s.user.id = :userId)")
    boolean existsByNameForCategoryAndUser(@Param("name") String name, @Param("categoryId") Long categoryId, @Param("userId") Long userId);

    // Find subcategory by name for a specific category and user
    @Query("SELECT s FROM Subcategory s WHERE s.name = :name AND s.category.id = :categoryId AND (s.isDefault = true OR s.user.id = :userId)")
    Optional<Subcategory> findByNameForCategoryAndUser(@Param("name") String name, @Param("categoryId") Long categoryId, @Param("userId") Long userId);

    // Find all subcategories available to a user
    @Query("SELECT s FROM Subcategory s WHERE s.isDefault = true OR s.user.id = :userId")
    List<Subcategory> findAllAvailableForUser(@Param("userId") Long userId);
}

