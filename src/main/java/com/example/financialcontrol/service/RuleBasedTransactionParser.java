package com.example.financialcontrol.service;

import com.example.financialcontrol.entity.TransactionType;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Rule-based parser for extracting transaction details from natural language text.
 *
 * This class uses simple regex patterns and keyword matching to detect:
 * - Transaction type (DEBIT/CREDIT)
 * - Amount (various formats)
 * - Date (today, yesterday, specific dates)
 *
 * IMPORTANT: No AI or external APIs are used. All logic is rule-based.
 */
@Component
public class RuleBasedTransactionParser {

    // =====================================================
    // KEYWORDS FOR TRANSACTION TYPE DETECTION
    // =====================================================

    // Keywords that indicate a DEBIT transaction (spending money)
    // Portuguese: gastei, paguei, comprei, pago, gasto, compra
    // English: spent, paid, bought, purchase, expense
    private static final String[] DEBIT_KEYWORDS = {
        "gastei", "paguei", "comprei", "pago", "gasto", "compra",
        "spent", "paid", "bought", "purchase", "expense"
    };

    // Keywords that indicate a CREDIT transaction (receiving money)
    // Portuguese: recebi, salário, entrada, ganhei, recebido
    // English: received, salary, income, earned, deposit
    private static final String[] CREDIT_KEYWORDS = {
        "recebi", "salário", "salario", "entrada", "ganhei", "recebido",
        "received", "salary", "income", "earned", "deposit"
    };

    // =====================================================
    // TRANSACTION TYPE DETECTION
    // =====================================================

    /**
     * Detects the transaction type (DEBIT or CREDIT) based on keywords in the text.
     *
     * Logic:
     * 1. First check for CREDIT keywords (receiving money is less common, so prioritize detection)
     * 2. Then check for DEBIT keywords (spending money)
     * 3. Default to DEBIT if no keywords found (most transactions are expenses)
     *
     * @param text The input text (should be lowercase)
     * @return CREDIT if income keywords found, DEBIT otherwise
     */
    public TransactionType detectTransactionType(String text) {
        String lowerText = text.toLowerCase();

        // Check for CREDIT keywords first (receiving money)
        for (String keyword : CREDIT_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return TransactionType.CREDIT;
            }
        }

        // Check for DEBIT keywords (spending money)
        for (String keyword : DEBIT_KEYWORDS) {
            if (lowerText.contains(keyword)) {
                return TransactionType.DEBIT;
            }
        }

        // Default to DEBIT (most common transaction type)
        return TransactionType.DEBIT;
    }

    // =====================================================
    // AMOUNT EXTRACTION
    // =====================================================

    /**
     * Result class for amount extraction, includes whether amount was detected.
     */
    public static class AmountResult {
        public final BigDecimal amount;
        public final boolean detected;

        public AmountResult(BigDecimal amount, boolean detected) {
            this.amount = amount;
            this.detected = detected;
        }
    }

    /**
     * Extracts the monetary amount from the text using regex patterns.
     *
     * Supported formats:
     * - Simple numbers: 23, 100, 1000
     * - Decimal with dot: 23.5, 23.50
     * - Decimal with comma: 23,5, 23,50
     * - With currency symbols: €23, $23, R$23
     * - With currency words: 23 euros, 23 reais, 23 dollars
     *
     * @param text The input text
     * @return AmountResult with the extracted amount and detection flag
     */
    public AmountResult extractAmount(String text) {
        String lowerText = text.toLowerCase();

        // Regex pattern explanation:
        // (?:€|\$|R\$)? - Optional currency symbol at start (€, $, R$)
        // \s* - Optional whitespace
        // (\d+(?:[.,]\d{1,2})?) - Number with optional decimal part (group 1)
        // \s* - Optional whitespace
        // (?:euros?|reais|dollars?|€|\$)? - Optional currency word/symbol at end
        Pattern amountPattern = Pattern.compile(
            "(?:€|\\$|R\\$)?\\s*(\\d+(?:[.,]\\d{1,2})?)\\s*(?:euros?|reais|dollars?|€|\\$)?"
        );

        Matcher matcher = amountPattern.matcher(lowerText);

        if (matcher.find()) {
            String amountStr = matcher.group(1);
            // Replace comma with dot for decimal parsing (European format support)
            amountStr = amountStr.replace(",", ".");
            try {
                BigDecimal amount = new BigDecimal(amountStr);
                // Only return if amount is positive
                if (amount.compareTo(BigDecimal.ZERO) > 0) {
                    return new AmountResult(amount, true);
                }
            } catch (NumberFormatException e) {
                // Invalid number format, return not detected
            }
        }

        // No valid amount found
        return new AmountResult(null, false);
    }

    // =====================================================
    // DATE EXTRACTION
    // =====================================================

    /**
     * Result class for date extraction, includes whether date was explicitly detected.
     */
    public static class DateResult {
        public final LocalDate date;
        public final boolean explicitlyDetected;

        public DateResult(LocalDate date, boolean explicitlyDetected) {
            this.date = date;
            this.explicitlyDetected = explicitlyDetected;
        }
    }

    /**
     * Extracts the date from the text.
     *
     * Supported formats:
     * - "hoje" or "today" → today's date
     * - "ontem" or "yesterday" → yesterday's date
     * - "anteontem" or "day before yesterday" → 2 days ago
     * - "há X dias" or "X days ago" → X days before today
     * - "há X semanas" or "X weeks ago" → X weeks before today
     * - ISO format: YYYY-MM-DD (e.g., 2024-01-15)
     * - European format: DD/MM/YYYY (e.g., 15/01/2024)
     * - Default: today's date if no date mentioned
     *
     * @param text The input text
     * @return DateResult with the date and whether it was explicitly mentioned
     */
    public DateResult extractDate(String text) {
        String lowerText = text.toLowerCase();

        // Check for "hoje" (Portuguese) or "today" (English)
        if (lowerText.contains("hoje") || lowerText.contains("today")) {
            return new DateResult(LocalDate.now(), true);
        }

        // Check for "ontem" (Portuguese) or "yesterday" (English)
        if (lowerText.contains("ontem") || lowerText.contains("yesterday")) {
            return new DateResult(LocalDate.now().minusDays(1), true);
        }

        // Check for "anteontem" (Portuguese) or "day before yesterday" (English)
        if (lowerText.contains("anteontem") || lowerText.contains("day before yesterday")) {
            return new DateResult(LocalDate.now().minusDays(2), true);
        }

        // Check for "há X dias" / "ha X dias" (Portuguese) - e.g., "há 2 dias", "há 3 dias"
        // Pattern matches: "há 2 dias", "ha 3 dias", "á 5 dias", "a 1 dia"
        Pattern ptDaysAgoPattern = Pattern.compile("(?:há|ha|á|a)\\s*(\\d+)\\s*dias?");
        Matcher ptDaysMatcher = ptDaysAgoPattern.matcher(lowerText);
        if (ptDaysMatcher.find()) {
            try {
                int daysAgo = Integer.parseInt(ptDaysMatcher.group(1));
                return new DateResult(LocalDate.now().minusDays(daysAgo), true);
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }

        // Check for "X days ago" (English) - e.g., "2 days ago", "3 days ago"
        Pattern enDaysAgoPattern = Pattern.compile("(\\d+)\\s*days?\\s*ago");
        Matcher enDaysMatcher = enDaysAgoPattern.matcher(lowerText);
        if (enDaysMatcher.find()) {
            try {
                int daysAgo = Integer.parseInt(enDaysMatcher.group(1));
                return new DateResult(LocalDate.now().minusDays(daysAgo), true);
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }

        // Check for "há X semanas" / "ha X semanas" (Portuguese) - e.g., "há 1 semana", "há 2 semanas"
        Pattern ptWeeksAgoPattern = Pattern.compile("(?:há|ha|á|a)\\s*(\\d+)\\s*semanas?");
        Matcher ptWeeksMatcher = ptWeeksAgoPattern.matcher(lowerText);
        if (ptWeeksMatcher.find()) {
            try {
                int weeksAgo = Integer.parseInt(ptWeeksMatcher.group(1));
                return new DateResult(LocalDate.now().minusWeeks(weeksAgo), true);
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }

        // Check for "X weeks ago" (English) - e.g., "1 week ago", "2 weeks ago"
        Pattern enWeeksAgoPattern = Pattern.compile("(\\d+)\\s*weeks?\\s*ago");
        Matcher enWeeksMatcher = enWeeksAgoPattern.matcher(lowerText);
        if (enWeeksMatcher.find()) {
            try {
                int weeksAgo = Integer.parseInt(enWeeksMatcher.group(1));
                return new DateResult(LocalDate.now().minusWeeks(weeksAgo), true);
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }

        // Check for "há X meses" / "ha X meses" (Portuguese) - e.g., "há 1 mês", "há 2 meses"
        Pattern ptMonthsAgoPattern = Pattern.compile("(?:há|ha|á|a)\\s*(\\d+)\\s*(?:mês|mes|meses)");
        Matcher ptMonthsMatcher = ptMonthsAgoPattern.matcher(lowerText);
        if (ptMonthsMatcher.find()) {
            try {
                int monthsAgo = Integer.parseInt(ptMonthsMatcher.group(1));
                return new DateResult(LocalDate.now().minusMonths(monthsAgo), true);
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }

        // Check for "X months ago" (English) - e.g., "1 month ago", "2 months ago"
        Pattern enMonthsAgoPattern = Pattern.compile("(\\d+)\\s*months?\\s*ago");
        Matcher enMonthsMatcher = enMonthsAgoPattern.matcher(lowerText);
        if (enMonthsMatcher.find()) {
            try {
                int monthsAgo = Integer.parseInt(enMonthsMatcher.group(1));
                return new DateResult(LocalDate.now().minusMonths(monthsAgo), true);
            } catch (NumberFormatException e) {
                // Continue to next pattern
            }
        }

        // Check for "last week" / "semana passada" (Portuguese)
        if (lowerText.contains("semana passada") || lowerText.contains("last week")) {
            return new DateResult(LocalDate.now().minusWeeks(1), true);
        }

        // Check for "last month" / "mês passado" (Portuguese)
        if (lowerText.contains("mês passado") || lowerText.contains("mes passado") || lowerText.contains("last month")) {
            return new DateResult(LocalDate.now().minusMonths(1), true);
        }

        // Check for ISO date format: YYYY-MM-DD
        Pattern isoDatePattern = Pattern.compile("(\\d{4})-(\\d{2})-(\\d{2})");
        Matcher isoMatcher = isoDatePattern.matcher(text);

        if (isoMatcher.find()) {
            try {
                int year = Integer.parseInt(isoMatcher.group(1));
                int month = Integer.parseInt(isoMatcher.group(2));
                int day = Integer.parseInt(isoMatcher.group(3));
                return new DateResult(LocalDate.of(year, month, day), true);
            } catch (Exception e) {
                // Invalid date, continue to next pattern
            }
        }

        // Check for European format: DD/MM/YYYY (common in Portugal/Brazil)
        Pattern euroDatePattern = Pattern.compile("(\\d{1,2})/(\\d{1,2})/(\\d{4})");
        Matcher euroMatcher = euroDatePattern.matcher(text);

        if (euroMatcher.find()) {
            try {
                int day = Integer.parseInt(euroMatcher.group(1));
                int month = Integer.parseInt(euroMatcher.group(2));
                int year = Integer.parseInt(euroMatcher.group(3));
                return new DateResult(LocalDate.of(year, month, day), true);
            } catch (Exception e) {
                // Invalid date, fall through to default
            }
        }

        // Default to today (date was NOT explicitly mentioned)
        return new DateResult(LocalDate.now(), false);
    }
}

