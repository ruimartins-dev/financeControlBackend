package com.example.financialcontrol.dto;

import com.example.financialcontrol.entity.TransactionType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * DTO representing a draft transaction before confirmation.
 * This is returned by the classification endpoint and contains
 * all the parsed/detected fields that the user can review and modify
 * before confirming the transaction.
 *
 * IMPORTANT: This is NOT persisted to the database.
 * It's only used to show the user what was detected from their input.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class TransactionDraftDto {

    // The wallet ID where the transaction will be created
    private Long walletId;

    // Transaction type: DEBIT (expense) or CREDIT (income)
    private TransactionType type;

    // The monetary amount detected from the text
    private BigDecimal amount;

    // The category detected or matched from the database
    private String category;

    // The subcategory detected or matched from the database
    private String subcategory;

    // The date detected from the text (or today if not specified)
    private LocalDate date;

    // The original text input (used as description)
    private String description;

    // Confidence indicators (optional, to help the user understand the detection)
    private boolean amountDetected;
    private boolean categoryMatched;  // true if category was found in DB, false if defaulted to "Other"
    private boolean dateDetected;     // true if date was explicitly mentioned in text
}

