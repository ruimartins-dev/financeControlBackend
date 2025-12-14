package com.example.financialcontrol.repository;

import com.example.financialcontrol.entity.HiddenSubcategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HiddenSubcategoryRepository extends JpaRepository<HiddenSubcategory, Long> {

    // Find all hidden subcategories for a user
    List<HiddenSubcategory> findByUserId(Long userId);

    // Find hidden subcategory by user and subcategory
    Optional<HiddenSubcategory> findByUserIdAndSubcategoryId(Long userId, Long subcategoryId);

    // Check if a subcategory is hidden for a user
    boolean existsByUserIdAndSubcategoryId(Long userId, Long subcategoryId);

    // Get list of hidden subcategory IDs for a user
    @Query("SELECT hs.subcategory.id FROM HiddenSubcategory hs WHERE hs.user.id = :userId")
    List<Long> findHiddenSubcategoryIdsByUserId(@Param("userId") Long userId);

    // Delete hidden subcategory by user and subcategory
    void deleteByUserIdAndSubcategoryId(Long userId, Long subcategoryId);
}

