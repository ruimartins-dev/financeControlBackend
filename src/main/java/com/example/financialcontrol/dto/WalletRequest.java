package com.example.financialcontrol.dto;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletRequest {
    @NotBlank(message = "Wallet name is required")
    @Size(max = 100, message = "Wallet name must be at most 100 characters")
    private String name;
    @NotBlank(message = "Currency is required")
    @Size(max = 10, message = "Currency code must be at most 10 characters")
    private String currency;
}