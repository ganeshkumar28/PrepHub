package com.prephub.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateExtractionRequest(
    @NotBlank
    @Size(min = 100, max = 10000)
    String rawText
) {}

