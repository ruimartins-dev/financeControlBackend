package com.example.financialcontrol.service;

import com.example.financialcontrol.dto.TransactionRequest;
import com.example.financialcontrol.dto.TransactionResponse;
import com.example.financialcontrol.dto.VoiceRequestDto;
import com.example.financialcontrol.entity.TransactionType;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.entity.Wallet;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Service for parsing natural language text into transactions.
 * Supports Portuguese and English keywords for transaction type detection,
 * amount extraction, date parsing, and category detection.
 */
@Service
public class VoiceService {

    private final TransactionService transactionService;
    private final WalletService walletService;

    // Keywords that indicate a DEBIT transaction (spending money)
    private static final String[] DEBIT_KEYWORDS = {
        "gastei", "paguei", "comprei", "pago", "gasto", "compra",
        "spent", "paid", "bought", "purchase", "expense"
    };

    // Keywords that indicate a CREDIT transaction (receiving money)
    private static final String[] CREDIT_KEYWORDS = {
        "recebi", "salário", "entrada", "ganhei", "recebido",
        "received", "salary", "income", "earned", "deposit"
    };

    // Category keywords mapping - maps keywords to category names
    private static final Map<String, String> CATEGORY_KEYWORDS = new HashMap<>();

    static {
        // Food related
        CATEGORY_KEYWORDS.put("supermercado", "Food");
        CATEGORY_KEYWORDS.put("supermarket", "Food");
        CATEGORY_KEYWORDS.put("mercado", "Food");
        CATEGORY_KEYWORDS.put("grocery", "Food");
        CATEGORY_KEYWORDS.put("groceries", "Food");
        CATEGORY_KEYWORDS.put("restaurante", "Food");
        CATEGORY_KEYWORDS.put("restaurant", "Food");
        CATEGORY_KEYWORDS.put("café", "Food");
        CATEGORY_KEYWORDS.put("cafe", "Food");
        CATEGORY_KEYWORDS.put("coffee", "Food");
        CATEGORY_KEYWORDS.put("almoço", "Food");
        CATEGORY_KEYWORDS.put("lunch", "Food");
        CATEGORY_KEYWORDS.put("jantar", "Food");
        CATEGORY_KEYWORDS.put("dinner", "Food");
        CATEGORY_KEYWORDS.put("comida", "Food");
        CATEGORY_KEYWORDS.put("food", "Food");

        // Transport related
        CATEGORY_KEYWORDS.put("uber", "Transport");
        CATEGORY_KEYWORDS.put("taxi", "Transport");
        CATEGORY_KEYWORDS.put("gasolina", "Transport");
        CATEGORY_KEYWORDS.put("gas", "Transport");
        CATEGORY_KEYWORDS.put("fuel", "Transport");
        CATEGORY_KEYWORDS.put("combustível", "Transport");
        CATEGORY_KEYWORDS.put("transporte", "Transport");
        CATEGORY_KEYWORDS.put("transport", "Transport");
        CATEGORY_KEYWORDS.put("metro", "Transport");
        CATEGORY_KEYWORDS.put("bus", "Transport");
        CATEGORY_KEYWORDS.put("ônibus", "Transport");

        // Income related
        CATEGORY_KEYWORDS.put("salário", "Income");
        CATEGORY_KEYWORDS.put("salary", "Income");
        CATEGORY_KEYWORDS.put("renda", "Income");
        CATEGORY_KEYWORDS.put("income", "Income");
        CATEGORY_KEYWORDS.put("pagamento", "Income");
        CATEGORY_KEYWORDS.put("payment", "Income");

        // Entertainment
        CATEGORY_KEYWORDS.put("cinema", "Entertainment");
        CATEGORY_KEYWORDS.put("movie", "Entertainment");
        CATEGORY_KEYWORDS.put("filme", "Entertainment");
        CATEGORY_KEYWORDS.put("netflix", "Entertainment");
        CATEGORY_KEYWORDS.put("spotify", "Entertainment");

        // Shopping
        CATEGORY_KEYWORDS.put("roupa", "Shopping");
        CATEGORY_KEYWORDS.put("clothes", "Shopping");
        CATEGORY_KEYWORDS.put("loja", "Shopping");
        CATEGORY_KEYWORDS.put("store", "Shopping");
        CATEGORY_KEYWORDS.put("shopping", "Shopping");

        // Bills/Utilities
        CATEGORY_KEYWORDS.put("conta", "Bills");
        CATEGORY_KEYWORDS.put("bill", "Bills");
        CATEGORY_KEYWORDS.put("luz", "Bills");
        CATEGORY_KEYWORDS.put("electricity", "Bills");
        CATEGORY_KEYWORDS.put("água", "Bills");
        CATEGORY_KEYWORDS.put("water", "Bills");
        CATEGORY_KEYWORDS.put("internet", "Bills");
        CATEGORY_KEYWORDS.put("telefone", "Bills");
        CATEGORY_KEYWORDS.put("phone", "Bills");

        // Health
        CATEGORY_KEYWORDS.put("farmácia", "Health");
        CATEGORY_KEYWORDS.put("pharmacy", "Health");
        CATEGORY_KEYWORDS.put("médico", "Health");
        CATEGORY_KEYWORDS.put("doctor", "Health");
        CATEGORY_KEYWORDS.put("hospital", "Health");
        CATEGORY_KEYWORDS.put("saúde", "Health");
        CATEGORY_KEYWORDS.put("health", "Health");
    }

    public VoiceService(TransactionService transactionService, WalletService walletService) {
        this.transactionService = transactionService;
        this.walletService = walletService;
    }

    /**
     * Parses natural language text and creates a transaction.
     *
     * @param request The voice request containing wallet ID and text
     * @param user The authenticated user
     * @return The created transaction response
     */
    @Transactional
    public TransactionResponse parseAndCreateTransaction(VoiceRequestDto request, User user) {
        // Validate wallet ownership
        Wallet wallet = walletService.getWalletEntityByIdAndUser(request.getWalletId(), user);

        String text = request.getText().toLowerCase().trim();

        // Parse the text to extract transaction details
        TransactionType type = detectTransactionType(text);
        BigDecimal amount = extractAmount(text);
        LocalDate date = extractDate(text);
        String category = detectCategory(text);
        String subcategory = detectSubcategory(text, category);

        // Validate that we could extract an amount
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new RuntimeException("Could not extract a valid amount from the text");
        }

        // Create the transaction request
        TransactionRequest transactionRequest = new TransactionRequest();
        transactionRequest.setType(type);
        transactionRequest.setAmount(amount);
        transactionRequest.setDate(date);
        transactionRequest.setCategory(category);
        transactionRequest.setSubcategory(subcategory);
        transactionRequest.setDescription(request.getText()); // Use original text as description

        // Use existing TransactionService to create the transaction
        return transactionService.createTransaction(wallet.getId(), transactionRequest, user);
    }

    /**
     * Detects the transaction type (DEBIT or CREDIT) based on keywords in the text.
     * Defaults to DEBIT if no keywords are found (most common use case).
     */
    private TransactionType detectTransactionType(String text) {
        // Check for CREDIT keywords first (receiving money)
        for (String keyword : CREDIT_KEYWORDS) {
            if (text.contains(keyword)) {
                return TransactionType.CREDIT;
            }
        }

        // Check for DEBIT keywords (spending money)
        for (String keyword : DEBIT_KEYWORDS) {
            if (text.contains(keyword)) {
                return TransactionType.DEBIT;
            }
        }

        // Default to DEBIT (most common transaction type)
        return TransactionType.DEBIT;
    }

    /**
     * Extracts the monetary amount from the text using regex patterns.
     * Supports formats: 23, 23.5, 23.50, 23,50, 23 euros, €23, $23, R$23
     */
    private BigDecimal extractAmount(String text) {
        // Pattern to match various amount formats
        // Matches: 23, 23.5, 23.50, 23,50, €23, $23, R$23, 23 euros, etc.
        Pattern amountPattern = Pattern.compile(
            "(?:€|\\$|R\\$)?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*(?:euros?|reais|dollars?|€|\\$)?"
        );

        Matcher matcher = amountPattern.matcher(text);

        if (matcher.find()) {
            String amountStr = matcher.group(1);
            // Replace comma with dot for decimal parsing
            amountStr = amountStr.replace(",", ".");
            try {
                return new BigDecimal(amountStr);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        return null;
    }

    /**
     * Extracts the date from the text.
     * Supports: "hoje" (today), "ontem" (yesterday), and ISO format (YYYY-MM-DD).
     * Defaults to today's date if no date is found.
     */
    private LocalDate extractDate(String text) {
        // Check for "hoje" or "today"
        if (text.contains("hoje") || text.contains("today")) {
            return LocalDate.now();
        }

        // Check for "ontem" or "yesterday"
        if (text.contains("ontem") || text.contains("yesterday")) {
            return LocalDate.now().minusDays(1);
        }

        // Check for ISO date format (YYYY-MM-DD)
        Pattern datePattern = Pattern.compile("(\\d{4}-\\d{2}-\\d{2})");
        Matcher matcher = datePattern.matcher(text);

        if (matcher.find()) {
            try {
                return LocalDate.parse(matcher.group(1));
            } catch (Exception e) {
                // Invalid date format, fall through to default
            }
        }

        // Check for DD/MM/YYYY format (common in Brazil/Europe)
        Pattern datePatternDMY = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
        Matcher matcherDMY = datePatternDMY.matcher(text);

        if (matcherDMY.find()) {
            try {
                int day = Integer.parseInt(matcherDMY.group(1));
                int month = Integer.parseInt(matcherDMY.group(2));
                int year = Integer.parseInt(matcherDMY.group(3));
                return LocalDate.of(year, month, day);
            } catch (Exception e) {
                // Invalid date, fall through to default
            }
        }

        // Default to today
        return LocalDate.now();
    }

    /**
     * Detects the category based on keywords in the text.
     * Defaults to "Other" if no keywords are found.
     */
    private String detectCategory(String text) {
        for (Map.Entry<String, String> entry : CATEGORY_KEYWORDS.entrySet()) {
            if (text.contains(entry.getKey())) {
                return entry.getValue();
            }
        }
        return "Other";
    }

    /**
     * Detects the subcategory based on keywords and the detected category.
     * Defaults to "General" if no specific subcategory is found.
     */
    private String detectSubcategory(String text, String category) {
        // Try to find a more specific subcategory based on keywords
        switch (category) {
            case "Food":
                if (text.contains("supermercado") || text.contains("supermarket") ||
                    text.contains("mercado") || text.contains("grocery") || text.contains("groceries")) {
                    return "Groceries";
                }
                if (text.contains("restaurante") || text.contains("restaurant")) {
                    return "Restaurant";
                }
                if (text.contains("café") || text.contains("cafe") || text.contains("coffee")) {
                    return "Coffee";
                }
                if (text.contains("almoço") || text.contains("lunch")) {
                    return "Lunch";
                }
                if (text.contains("jantar") || text.contains("dinner")) {
                    return "Dinner";
                }
                return "General";

            case "Transport":
                if (text.contains("uber") || text.contains("taxi")) {
                    return "Ride";
                }
                if (text.contains("gasolina") || text.contains("gas") ||
                    text.contains("fuel") || text.contains("combustível")) {
                    return "Fuel";
                }
                if (text.contains("metro") || text.contains("bus") || text.contains("ônibus")) {
                    return "Public Transport";
                }
                return "General";

            case "Income":
                if (text.contains("salário") || text.contains("salary")) {
                    return "Salary";
                }
                return "General";

            case "Entertainment":
                if (text.contains("cinema") || text.contains("movie") || text.contains("filme")) {
                    return "Movies";
                }
                if (text.contains("netflix") || text.contains("spotify")) {
                    return "Streaming";
                }
                return "General";

            case "Bills":
                if (text.contains("luz") || text.contains("electricity")) {
                    return "Electricity";
                }
                if (text.contains("água") || text.contains("water")) {
                    return "Water";
                }
                if (text.contains("internet")) {
                    return "Internet";
                }
                if (text.contains("telefone") || text.contains("phone")) {
                    return "Phone";
                }
                return "General";

            case "Health":
                if (text.contains("farmácia") || text.contains("pharmacy")) {
                    return "Pharmacy";
                }
                if (text.contains("médico") || text.contains("doctor")) {
                    return "Doctor";
                }
                if (text.contains("hospital")) {
                    return "Hospital";
                }
                return "General";

            default:
                return "General";
        }
    }
}

