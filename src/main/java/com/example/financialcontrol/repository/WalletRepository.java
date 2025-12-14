package com.example.financialcontrol.repository;
import com.example.financialcontrol.entity.Wallet;
import com.example.financialcontrol.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
@Repository
public interface WalletRepository extends JpaRepository<Wallet, Long> {
    List<Wallet> findByUser(User user);
    List<Wallet> findByUserId(Long userId);
    Optional<Wallet> findByIdAndUser(Long id, User user);
    Optional<Wallet> findByIdAndUserId(Long id, Long userId);
}
