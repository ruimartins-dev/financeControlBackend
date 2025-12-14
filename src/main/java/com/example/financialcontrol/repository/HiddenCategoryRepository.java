package com.example.financialcontrol.repository;

import com.example.financialcontrol.entity.HiddenCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HiddenCategoryRepository extends JpaRepository<HiddenCategory, Long> {

    List<HiddenCategory> findByUserId(Long userId);

    Optional<HiddenCategory> findByUserIdAndCategoryId(Long userId, Long categoryId);

    boolean existsByUserIdAndCategoryId(Long userId, Long categoryId);

    @Query("SELECT hc.category.id FROM HiddenCategory hc WHERE hc.user.id = :userId")
    List<Long> findHiddenCategoryIdsByUserId(@Param("userId") Long userId);

    void deleteByUserIdAndCategoryId(Long userId, Long categoryId);
}

