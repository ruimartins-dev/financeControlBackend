package com.example.financialcontrol.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for CSV import summary response.
 * Contains the count of successfully created and skipped transactions.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportSummaryResponse {

    // Number of transactions successfully created
    private int created;

    // Number of rows skipped due to parsing errors
    private int skipped;
}

