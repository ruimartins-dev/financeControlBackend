package com.example.financialcontrol.repository;

import com.example.financialcontrol.entity.Category;
import com.example.financialcontrol.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, Long> {

    // Find all default categories
    List<Category> findByIsDefaultTrue();

    // Find all categories for a specific user
    List<Category> findByUserId(Long userId);

    // Find all default categories by type
    List<Category> findByIsDefaultTrueAndType(TransactionType type);

    // Find all user categories by type
    List<Category> findByUserIdAndType(Long userId, TransactionType type);

    // Find all categories available to a user (default + user's own)
    @Query("SELECT c FROM Category c WHERE c.isDefault = true OR c.user.id = :userId")
    List<Category> findAllAvailableForUser(@Param("userId") Long userId);

    // Find all categories available to a user by type (default + user's own)
    @Query("SELECT c FROM Category c WHERE (c.isDefault = true OR c.user.id = :userId) AND c.type = :type")
    List<Category> findAllAvailableForUserByType(@Param("userId") Long userId, @Param("type") TransactionType type);

    // Check if a category name already exists for a user or as default
    @Query("SELECT COUNT(c) > 0 FROM Category c WHERE c.name = :name AND (c.isDefault = true OR c.user.id = :userId)")
    boolean existsByNameForUser(@Param("name") String name, @Param("userId") Long userId);

    // Find category by name for user
    @Query("SELECT c FROM Category c WHERE c.name = :name AND (c.isDefault = true OR c.user.id = :userId)")
    Optional<Category> findByNameForUser(@Param("name") String name, @Param("userId") Long userId);
}
