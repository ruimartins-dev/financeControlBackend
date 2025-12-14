package com.example.financialcontrol.service;

import com.example.financialcontrol.dto.ImportSummaryResponse;
import com.example.financialcontrol.dto.TransactionRequest;
import com.example.financialcontrol.entity.TransactionType;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.entity.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

/**
 * Service for importing transactions from CSV bank statements.
 * Supports both comma and semicolon separated formats.
 *
 * Expected CSV format:
 * date,description,amount,type,category,subcategory
 *
 * Example row:
 * 2024-01-15,Supermarket purchase,45.90,DEBIT,Food,Groceries
 */
@Service
public class ImportService {

    private static final Logger logger = LoggerFactory.getLogger(ImportService.class);

    private final TransactionService transactionService;
    private final WalletService walletService;

    // Date formatter for parsing CSV dates (ISO format)
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE;

    public ImportService(TransactionService transactionService, WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    /**
     * Imports transactions from a CSV file into the specified wallet.
     *
     * @param walletId The ID of the wallet to import transactions into
     * @param file The CSV file containing transactions
     * @param user The authenticated user
     * @return Summary of the import (created and skipped counts)
     */
    @Transactional
    public ImportSummaryResponse importFromCsv(Long walletId, MultipartFile file, User user) {
        // Validate wallet ownership first
        Wallet wallet = walletService.getWalletEntityByIdAndUser(walletId, user);

        int created = 0;
        int skipped = 0;
        int lineNumber = 0;

        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {

            String line;
            boolean isFirstLine = true;

            while ((line = reader.readLine()) != null) {
                lineNumber++;

                // Skip empty lines
                if (line.trim().isEmpty()) {
                    continue;
                }

                // Skip header row if it looks like a header
                if (isFirstLine && isHeaderRow(line)) {
                    isFirstLine = false;
                    logger.debug("Skipping header row: {}", line);
                    continue;
                }
                isFirstLine = false;

                try {
                    // Parse the CSV row and create a transaction
                    TransactionRequest request = parseCsvRow(line);
                    transactionService.createTransaction(wallet.getId(), request, user);
                    created++;
                } catch (Exception e) {
                    // Log the error and skip the invalid row
                    logger.warn("Skipping invalid row {} : {} - Error: {}", lineNumber, line, e.getMessage());
                    skipped++;
                }
            }

        } catch (IOException e) {
            throw new RuntimeException("Error reading CSV file: " + e.getMessage());
        }

        logger.info("CSV import completed. Created: {}, Skipped: {}", created, skipped);
        return new ImportSummaryResponse(created, skipped);
    }

    /**
     * Checks if a row looks like a header row.
     * Returns true if the row contains common header keywords.
     */
    private boolean isHeaderRow(String line) {
        String lowerLine = line.toLowerCase();
        return lowerLine.contains("date") ||
               lowerLine.contains("description") ||
               lowerLine.contains("amount") ||
               lowerLine.contains("type") ||
               lowerLine.contains("category");
    }

    /**
     * Parses a single CSV row into a TransactionRequest.
     * Supports both comma (,) and semicolon (;) as delimiters.
     *
     * Expected format: date,description,amount,type,category,subcategory
     *
     * @param line The CSV row to parse
     * @return A TransactionRequest populated with the parsed data
     */
    private TransactionRequest parseCsvRow(String line) {
        // Detect delimiter (semicolon or comma)
        String delimiter = line.contains(";") ? ";" : ",";

        // Split the line by delimiter
        String[] parts = line.split(delimiter, -1); // -1 to keep empty trailing fields

        // Validate minimum required fields (6 fields expected)
        if (parts.length < 6) {
            throw new IllegalArgumentException("Row has insufficient columns. Expected 6, got " + parts.length);
        }

        // Parse each field
        LocalDate date = parseDate(parts[0].trim());
        String description = parts[1].trim();
        BigDecimal amount = parseAmount(parts[2].trim());
        TransactionType type = parseTransactionType(parts[3].trim());
        String category = parts[4].trim();
        String subcategory = parts[5].trim();

        // Validate required fields
        if (category.isEmpty()) {
            throw new IllegalArgumentException("Category cannot be empty");
        }
        if (subcategory.isEmpty()) {
            throw new IllegalArgumentException("Subcategory cannot be empty");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Amount must be a positive number");
        }

        // Create and return the transaction request
        TransactionRequest request = new TransactionRequest();
        request.setDate(date);
        request.setDescription(description.isEmpty() ? null : description);
        request.setAmount(amount);
        request.setType(type);
        request.setCategory(category);
        request.setSubcategory(subcategory);

        return request;
    }

    /**
     * Parses a date string in ISO format (YYYY-MM-DD).
     * Also supports DD/MM/YYYY format.
     */
    private LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) {
            throw new IllegalArgumentException("Date is required");
        }

        try {
            // Try ISO format first (YYYY-MM-DD)
            return LocalDate.parse(dateStr, DATE_FORMATTER);
        } catch (DateTimeParseException e) {
            // Try DD/MM/YYYY format
            try {
                DateTimeFormatter altFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return LocalDate.parse(dateStr, altFormatter);
            } catch (DateTimeParseException e2) {
                throw new IllegalArgumentException("Invalid date format. Expected YYYY-MM-DD or DD/MM/YYYY, got: " + dateStr);
            }
        }
    }

    /**
     * Parses an amount string to BigDecimal.
     * Handles both comma and dot as decimal separators.
     */
    private BigDecimal parseAmount(String amountStr) {
        if (amountStr == null || amountStr.isEmpty()) {
            throw new IllegalArgumentException("Amount is required");
        }

        try {
            // Remove currency symbols and whitespace
            String cleanAmount = amountStr.replaceAll("[€$R\\s]", "");
            // Replace comma with dot for decimal parsing
            cleanAmount = cleanAmount.replace(",", ".");
            return new BigDecimal(cleanAmount);
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Invalid amount format: " + amountStr);
        }
    }

    /**
     * Parses the transaction type string.
     * Accepts: DEBIT, CREDIT (case-insensitive), D, C
     */
    private TransactionType parseTransactionType(String typeStr) {
        if (typeStr == null || typeStr.isEmpty()) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        String upperType = typeStr.toUpperCase().trim();

        switch (upperType) {
            case "DEBIT":
            case "D":
                return TransactionType.DEBIT;
            case "CREDIT":
            case "C":
                return TransactionType.CREDIT;
            default:
                throw new IllegalArgumentException("Invalid transaction type: " + typeStr + ". Expected DEBIT or CREDIT");
        }
    }
}

