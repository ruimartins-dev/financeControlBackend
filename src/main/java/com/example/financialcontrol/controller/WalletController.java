package com.example.financialcontrol.controller;
import com.example.financialcontrol.dto.ApiResponse;
import com.example.financialcontrol.dto.WalletRequest;
import com.example.financialcontrol.dto.WalletResponse;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.service.UserService;
import com.example.financialcontrol.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/wallets")
public class WalletController {
    private final WalletService walletService;
    private final UserService userService;
    public WalletController(WalletService walletService, UserService userService) {
        this.walletService = walletService;
        this.userService = userService;
    }
    @GetMapping
    public ResponseEntity<List<WalletResponse>> getMyWallets(Authentication auth) {
        User user = userService.findByUsername(auth.getName());
        return ResponseEntity.ok(walletService.getWalletsByUser(user));
    }
    @GetMapping("/{id}")
    public ResponseEntity<?> getWallet(@PathVariable Long id, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            return ResponseEntity.ok(walletService.getWalletById(id, user));
        } catch (RuntimeException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(e.getMessage()));
        }
    }
    @PostMapping
    public ResponseEntity<?> createWallet(@Valid @RequestBody WalletRequest request, Authentication auth) {
        try {
            User user = userService.findByUsername(auth.getName());
            return ResponseEntity.status(HttpStatus.CREATED).body(walletService.createWallet(request, user));
        } catch (RuntimeException e) {
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}
