package com.example.financialcontrol.controller;

import com.example.financialcontrol.dto.ApiResponse;
import com.example.financialcontrol.dto.TransactionDraftDto;
import com.example.financialcontrol.dto.TransactionResponse;
import com.example.financialcontrol.dto.VoiceRequestDto;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.service.UserService;
import com.example.financialcontrol.service.VoiceClassificationService;
import com.example.financialcontrol.service.VoiceService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

/**
 * Controller for handling voice/text transaction processing.
 *
 * Supports TWO flows:
 * 1. TWO-STEP FLOW (Recommended):
 *    - POST /api/voice/classify - Classifies text and returns a draft (NO database insert)
 *    - POST /api/wallets/{walletId}/transactions - Confirms and saves the transaction
 *
 * 2. LEGACY ONE-STEP FLOW:
 *    - POST /api/voice/parse - Parses and immediately creates the transaction
 *
 * The two-step flow is recommended because it allows the user to review
 * and modify the detected values before confirming the transaction.
 */
@RestController
@RequestMapping("/api/voice")
public class VoiceController {

    private final VoiceService voiceService;
    private final VoiceClassificationService classificationService;
    private final UserService userService;

    public VoiceController(
            VoiceService voiceService,
            VoiceClassificationService classificationService,
            UserService userService) {
        this.voiceService = voiceService;
        this.classificationService = classificationService;
        this.userService = userService;
    }

    // =========================================================================
    // STEP 1: CLASSIFY TEXT (NO DATABASE INSERT)
    // =========================================================================

    /**
     * Classifies natural language text and returns a draft transaction.
     *
     * IMPORTANT: This endpoint NEVER saves anything to the database.
     * It only parses the text and returns what was detected for user review.
     *
     * Example request body:
     * {
     *   "walletId": 1,
     *   "text": "gastei 23.50 no supermercado ontem"
     * }
     *
     * Example response:
     * {
     *   "walletId": 1,
     *   "type": "DEBIT",
     *   "amount": 23.50,
     *   "category": "Food",
     *   "subcategory": "Groceries",
     *   "date": "2024-01-13",
     *   "description": "gastei 23.50 no supermercado ontem",
     *   "amountDetected": true,
     *   "categoryMatched": true,
     *   "dateDetected": true
     * }
     *
     * After receiving this response, the frontend should:
     * 1. Display the draft to the user for review
     * 2. Allow the user to modify any fields
     * 3. Call POST /api/wallets/{walletId}/transactions to confirm and save
     *
     * @param request The voice request containing wallet ID and text
     * @param auth The authentication object for the current user
     * @return TransactionDraftDto with all detected fields, or error
     */
    @PostMapping("/classify")
    public ResponseEntity<?> classifyVoiceInput(
            @Valid @RequestBody VoiceRequestDto request,
            Authentication auth) {
        try {
            // Get the authenticated user
            User user = userService.findByUsername(auth.getName());

            // Classify the text (NO database insert)
            TransactionDraftDto draft = classificationService.classifyText(request, user);

            // Return the draft for user review
            return ResponseEntity.ok(draft);
        } catch (RuntimeException e) {
            // Return error message if classification fails
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }

    // =========================================================================
    // LEGACY: ONE-STEP PARSE AND CREATE (DEPRECATED)
    // =========================================================================

    /**
     * [LEGACY] Parses natural language text and immediately creates a transaction.
     *
     * NOTE: This endpoint is kept for backward compatibility.
     * For new implementations, use the two-step flow:
     * 1. POST /api/voice/classify - Get draft
     * 2. POST /api/wallets/{walletId}/transactions - Confirm and save
     *
     * @param request The voice request containing wallet ID and text
     * @param auth The authentication object for the current user
     * @return The created transaction or an error response
     */
    @PostMapping("/parse")
    public ResponseEntity<?> parseVoiceInput(
            @Valid @RequestBody VoiceRequestDto request,
            Authentication auth) {
        try {
            // Get the authenticated user
            User user = userService.findByUsername(auth.getName());

            // Parse the text and create a transaction (legacy behavior)
            TransactionResponse response = voiceService.parseAndCreateTransaction(request, user);

            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (RuntimeException e) {
            // Return error message if parsing fails or wallet not found
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

