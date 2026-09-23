package com.prephub.api.service.gemini;

public record GeminiResponse(
    String content,
    Integer inputTokens,
    Integer outputTokens
) {}

