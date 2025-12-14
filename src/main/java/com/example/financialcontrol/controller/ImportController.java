package com.example.financialcontrol.controller;

import com.example.financialcontrol.dto.ApiResponse;
import com.example.financialcontrol.dto.ImportSummaryResponse;
import com.example.financialcontrol.entity.User;
import com.example.financialcontrol.service.ImportService;
import com.example.financialcontrol.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * Controller for importing transactions from CSV bank statements.
 * Allows users to upload a CSV file and bulk import transactions.
 */
@RestController
@RequestMapping("/api/wallets")
public class ImportController {

    private final ImportService importService;
    private final UserService userService;

    public ImportController(ImportService importService, UserService userService) {
        this.importService = importService;
        this.userService = userService;
    }

    /**
     * Imports transactions from a CSV file into the specified wallet.
     *
     * Expected CSV format (comma or semicolon separated):
     * date,description,amount,type,category,subcategory
     *
     * Example row:
     * 2024-01-15,Supermarket purchase,45.90,DEBIT,Food,Groceries
     *
     * The first row will be skipped if it appears to be a header row.
     * Invalid rows will be skipped and logged.
     *
     * @param walletId The ID of the wallet to import into
     * @param file The CSV file to import
     * @param auth The authentication object for the current user
     * @return A summary with the number of created and skipped transactions
     */
    @PostMapping(value = "/{walletId}/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> importCsv(
            @PathVariable Long walletId,
            @RequestParam("file") MultipartFile file,
            Authentication auth) {

        // Validate that a file was provided
        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("No file provided"));
        }

        // Validate file type (must be CSV)
        String filename = file.getOriginalFilename();
        if (filename == null || !filename.toLowerCase().endsWith(".csv")) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("File must be a CSV file"));
        }

        try {
            // Get the authenticated user
            User user = userService.findByUsername(auth.getName());

            // Import transactions from the CSV
            ImportSummaryResponse summary = importService.importFromCsv(walletId, file, user);

            return ResponseEntity.status(HttpStatus.OK).body(summary);
        } catch (RuntimeException e) {
            // Return error message if import fails
            return ResponseEntity.badRequest().body(ApiResponse.error(e.getMessage()));
        }
    }
}

