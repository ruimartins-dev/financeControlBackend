package com.example.financialcontrol.service;
import com.example.financialcontrol.dto.WalletRequest;
import com.example.financialcontrol.dto.WalletResponse;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.entity.Wallet;
import com.example.financialcontrol.repository.WalletRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class WalletService {
    private final WalletRepository walletRepository;
    public WalletService(WalletRepository walletRepository) {
        this.walletRepository = walletRepository;
    }
    @Transactional(readOnly = true)
    public List<WalletResponse> getWalletsByUser(User user) {
        return walletRepository.findByUser(user).stream()
                .map(w -> new WalletResponse(w.getId(), w.getName(), w.getCurrency(), w.getCreatedAt()))
                .collect(Collectors.toList());
    }
    @Transactional(readOnly = true)
    public WalletResponse getWalletById(Long walletId, User user) {
        Wallet wallet = walletRepository.findByIdAndUser(walletId, user)
                .orElseThrow(() -> new RuntimeException("Wallet not found or access denied"));
        return new WalletResponse(wallet.getId(), wallet.getName(), wallet.getCurrency(), wallet.getCreatedAt());
    }
    @Transactional(readOnly = true)
    public Wallet getWalletEntityByIdAndUser(Long walletId, User user) {
        return walletRepository.findByIdAndUser(walletId, user)
                .orElseThrow(() -> new RuntimeException("Wallet not found or access denied"));
    }
    @Transactional
    public WalletResponse createWallet(WalletRequest request, User user) {
        Wallet wallet = new Wallet();
        wallet.setUser(user);
        wallet.setName(request.getName());
        wallet.setCurrency(request.getCurrency());
        Wallet saved = walletRepository.save(wallet);
        return new WalletResponse(saved.getId(), saved.getName(), saved.getCurrency(), saved.getCreatedAt());
    }
    @Transactional(readOnly = true)
    public boolean isWalletOwnedByUser(Long walletId, User user) {
        return walletRepository.findByIdAndUser(walletId, user).isPresent();
    }
}
