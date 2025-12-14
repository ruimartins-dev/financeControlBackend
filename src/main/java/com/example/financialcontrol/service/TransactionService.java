package com.example.financialcontrol.service;
import com.example.financialcontrol.dto.TransactionRequest;
import com.example.financialcontrol.dto.TransactionResponse;
import com.example.financialcontrol.entity.Transaction;
import com.example.financialcontrol.entity.TransactionType;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.entity.Wallet;
import com.example.financialcontrol.repository.TransactionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;
@Service
public class TransactionService {
    private final TransactionRepository transactionRepository;
    private final WalletService walletService;
    public TransactionService(TransactionRepository transactionRepository, WalletService walletService) {
        this.transactionRepository = transactionRepository;
        this.walletService = walletService;
    }
    @Transactional(readOnly = true)
    public List<TransactionResponse> getTransactions(Long walletId, User user, TransactionType type, LocalDate fromDate, LocalDate toDate) {
        walletService.getWalletEntityByIdAndUser(walletId, user);
        return transactionRepository.findByWalletIdWithFilters(walletId, type, fromDate, toDate).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }
    @Transactional
    public TransactionResponse createTransaction(Long walletId, TransactionRequest request, User user) {
        Wallet wallet = walletService.getWalletEntityByIdAndUser(walletId, user);
        Transaction t = new Transaction();
        t.setWallet(wallet);
        t.setType(request.getType());
        t.setCategory(request.getCategory());
        t.setSubcategory(request.getSubcategory());
        t.setAmount(request.getAmount());
        t.setDescription(request.getDescription());
        t.setDate(request.getDate());
        return mapToResponse(transactionRepository.save(t));
    }
    @Transactional
    public void deleteTransaction(Long transactionId, User user) {
        Transaction t = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new RuntimeException("Transaction not found"));
        if (!walletService.isWalletOwnedByUser(t.getWallet().getId(), user)) {
            throw new RuntimeException("Access denied");
        }
        transactionRepository.delete(t);
    }
    private TransactionResponse mapToResponse(Transaction t) {
        return new TransactionResponse(t.getId(), t.getWallet().getId(), t.getType(), t.getCategory(),
                t.getSubcategory(), t.getAmount(), t.getDescription(), t.getDate(), t.getCreatedAt());
    }
}