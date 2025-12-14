package com.example.financialcontrol.dto;
import com.example.financialcontrol.entity.TransactionType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDate;
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionRequest {
    @NotNull(message = "Transaction type is required")
    private TransactionType type;
    @NotBlank(message = "Category is required")
    @Size(max = 50)
    private String category;
    @NotBlank(message = "Subcategory is required")
    @Size(max = 50)
    private String subcategory;
    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;
    @Size(max = 255)
    private String description;
    @NotNull(message = "Date is required")
    private LocalDate date;
}