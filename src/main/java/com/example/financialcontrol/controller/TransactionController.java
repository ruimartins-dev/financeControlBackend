package com.example.financialcontrol.controller;
import com.example.financialcontrol.dto.ApiResponse;
import com.example.financialcontrol.dto.TransactionRequest;
import com.example.financialcontrol.dto.TransactionResponse;
import com.example.financialcontrol.entity.TransactionType;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.service.TransactionService;
import com.example.financialcontrol.service.UserService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
@RestController
@RequestMapping("/api")
public class TransactionController {
    private final TransactionService transactionService;
    private final UserService userService;
    public TransactionController(TransactionService transactionService, UserService userService) {
        this.transactionService = transactionService;
        this.userService = userService;
    }
    @GetMapping("/wallets/{walletId}/transactions")
    public ResponseEntity<?> getTransactions(
            @PathVariable Long walletId,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            List<TransactionResponse> transactions = transactionService.getTransactions(walletId, user, type, fromDate, toDate);
            return ResponseEntity.ok(transactions);
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
    @PostMapping("/wallets/{walletId}/transactions")
    public ResponseEntity<?> createTransaction(@PathVariable Long walletId, @Valid @RequestBody TransactionRequest request, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(transactionService.createTransaction(walletId, request, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
    @DeleteMapping("/transactions/{id}")
    public ResponseEntity<?> deleteTransaction(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            transactionService.deleteTransaction(id, user);
            return ResponseEntity.ok(ApiResponse.success("Transaction deleted successfully"));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
}
