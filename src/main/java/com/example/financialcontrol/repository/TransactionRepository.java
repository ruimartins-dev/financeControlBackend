package com.example.financialcontrol.repository;
import com.example.financialcontrol.entity.Transaction;
import com.example.financialcontrol.entity.TransactionType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.time.LocalDate;
import java.util.List;
@Repository
public interface TransactionRepository extends JpaRepository<Transaction, Long> {
    List<Transaction> findByWalletId(Long walletId);
    List<Transaction> findByWalletIdOrderByDateDesc(Long walletId);
    @Query("SELECT t FROM Transaction t WHERE t.wallet.id = :walletId " +
           "AND (:type IS NULL OR t.type = :type) " +
           "AND (:fromDate IS NULL OR t.date >= :fromDate) " +
           "AND (:toDate IS NULL OR t.date <= :toDate) " +
           "ORDER BY t.date DESC")
    List<Transaction> findByWalletIdWithFilters(
            @Param("walletId") Long walletId,
            @Param("type") TransactionType type,
            @Param("fromDate") LocalDate fromDate,
            @Param("toDate") LocalDate toDate
    );

    /**
     * Check if any transaction exists with the given category name for a specific user
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t " +
           "WHERE t.category = :categoryName AND t.wallet.user.id = :userId")
    boolean existsByCategoryNameAndUserId(@Param("categoryName") String categoryName, @Param("userId") Long userId);

    /**
     * Check if any transaction exists with the given subcategory name for a specific user
     */
    @Query("SELECT CASE WHEN COUNT(t) > 0 THEN true ELSE false END FROM Transaction t " +
           "WHERE t.subcategory = :subcategoryName AND t.wallet.user.id = :userId")
    boolean existsBySubcategoryNameAndUserId(@Param("subcategoryName") String subcategoryName, @Param("userId") Long userId);
}
