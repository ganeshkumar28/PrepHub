package com.prephub.api.service.gemini;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Component
public class GeminiClient {

    private static final Logger log = LoggerFactory.getLogger(GeminiClient.class);

    private final RestClient restClient;
    private final ObjectMapper objectMapper;
    private final String apiKey;
    private final String model;
    private final String baseUrl;

    public GeminiClient(
            @Value("${gemini.api-key:}") String apiKey,
            @Value("${gemini.model:gemini-3.5-flash-lite}") String model,
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com}") String baseUrl,
            ObjectMapper objectMapper,
            RestClient.Builder restClientBuilder) {
        this.apiKey = apiKey != null ? apiKey.trim() : "";
        this.model = model != null && !model.isBlank() ? model.trim() : "gemini-3.5-flash-lite";
        this.baseUrl = baseUrl != null && !baseUrl.isBlank() ? baseUrl.trim() : "https://generativelanguage.googleapis.com";
        this.objectMapper = objectMapper;
        this.restClient = restClientBuilder.build();
    }

    public GeminiResponse generateExtraction(String systemPrompt, String rawText) {
        if (apiKey.isBlank()) {
            throw new IllegalStateException("Gemini API key is not configured. Please set GEMINI_API_KEY.");
        }

        Map<String, Object> requestBody = Map.of(
            "system_instruction", Map.of(
                "parts", List.of(Map.of("text", systemPrompt))
            ),
            "contents", List.of(
                Map.of(
                    "role", "user",
                    "parts", List.of(Map.of("text", rawText))
                )
            ),
            "generationConfig", Map.of(
                "response_mime_type", "application/json",
                "temperature", 0.1
            )
        );

        List<String> modelsToTry = new ArrayList<>();
        modelsToTry.add(this.model);
        if (!"gemini-3.5-flash-lite".equals(this.model)) {
            modelsToTry.add("gemini-3.5-flash-lite");
        }

        Exception lastException = null;
        for (String currentModel : modelsToTry) {
            String uri = String.format("%s/v1beta/models/%s:generateContent", baseUrl, currentModel);
            int maxAttempts = 3;
            long backoffMs = 1500;

            for (int attempt = 1; attempt <= maxAttempts; attempt++) {
                try {
                    String rawResponseBody = restClient.post()
                        .uri(uri)
                        .header("x-goog-api-key", apiKey)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(requestBody)
                        .retrieve()
                        .body(String.class);

                    return parseGeminiResponse(rawResponseBody);
                } catch (HttpStatusCodeException e) {
                    lastException = e;
                    int statusCode = e.getStatusCode().value();
                    if ((statusCode == 503 || statusCode == 429) && attempt < maxAttempts) {
                        log.warn("Gemini model {} returned HTTP {}. Retrying in {}ms (attempt {}/{})",
                            currentModel, statusCode, backoffMs, attempt, maxAttempts);
                        try {
                            Thread.sleep(backoffMs);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            throw new RuntimeException("Interrupted during retry backoff", ie);
                        }
                        backoffMs *= 2;
                        continue;
                    }
                    if (statusCode == 503 && modelsToTry.indexOf(currentModel) < modelsToTry.size() - 1) {
                        log.warn("Gemini model {} unavailable after retries with HTTP 503. Attempting fallback model...", currentModel);
                        break;
                    }
                    throw e;
                } catch (Exception e) {
                    lastException = e;
                    throw new RuntimeException("Failed to call Gemini API: " + e.getMessage(), e);
                }
            }
        }

        throw new RuntimeException("All attempts to call Gemini API failed: " +
            (lastException != null ? lastException.getMessage() : "Unknown error"), lastException);
    }

    public GeminiResponse parseGeminiResponse(String responseJson) {
        try {
            JsonNode root = objectMapper.readTree(responseJson);

            Integer inputTokens = null;
            Integer outputTokens = null;
            JsonNode usage = root.path("usageMetadata");
            if (!usage.isMissingNode()) {
                if (usage.has("promptTokenCount")) {
                    inputTokens = usage.get("promptTokenCount").asInt();
                }
                if (usage.has("candidatesTokenCount")) {
                    outputTokens = usage.get("candidatesTokenCount").asInt();
                }
            }

            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                throw new IllegalStateException("Gemini returned no candidates in response");
            }

            JsonNode firstCandidate = candidates.get(0);
            JsonNode parts = firstCandidate.path("content").path("parts");
            if (!parts.isArray() || parts.isEmpty()) {
                throw new IllegalStateException("Gemini returned no content parts in candidate");
            }

            String contentText = parts.get(0).path("text").asText("");
            contentText = sanitizeJson(contentText);

            return new GeminiResponse(contentText, inputTokens, outputTokens);
        } catch (Exception e) {
            log.error("Failed to parse Gemini response: {}", responseJson, e);
            throw new RuntimeException("Failed to parse Gemini response: " + e.getMessage(), e);
        }
    }

    private String sanitizeJson(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.trim();
        if (trimmed.startsWith("```json")) {
            trimmed = trimmed.substring(7);
        } else if (trimmed.startsWith("```")) {
            trimmed = trimmed.substring(3);
        }
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }
        return trimmed.trim();
    }
}
