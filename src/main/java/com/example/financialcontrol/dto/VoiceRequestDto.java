package com.example.financialcontrol.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for voice/text transaction parsing requests.
 * Contains the wallet ID and the natural language text to parse.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRequestDto {

    @NotNull(message = "Wallet ID is required")
    private Long walletId;

    @NotBlank(message = "Text is required")
    private String text;
}

