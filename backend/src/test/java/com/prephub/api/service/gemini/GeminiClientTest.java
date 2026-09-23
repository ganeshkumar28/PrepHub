package com.prephub.api.service.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestClient;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class GeminiClientTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final GeminiClient geminiClient = new GeminiClient(
        "dummy-key",
        "gemini-2.5-flash",
        "https://example.com",
        objectMapper,
        RestClient.builder()
    );

    @Test
    @DisplayName("Parse Gemini response extracts content and token telemetry")
    void testParseGeminiResponse() {
        String json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "{\\"isInterviewContent\\": true, \\"confidence\\": 0.95}"
                      }
                    ],
                    "role": "model"
                  },
                  "finishReason": "STOP"
                }
              ],
              "usageMetadata": {
                "promptTokenCount": 350,
                "candidatesTokenCount": 120,
                "totalTokenCount": 470
              }
            }
            """;

        GeminiResponse response = geminiClient.parseGeminiResponse(json);

        assertNotNull(response);
        assertEquals("{\"isInterviewContent\": true, \"confidence\": 0.95}", response.content());
        assertEquals(350, response.inputTokens());
        assertEquals(120, response.outputTokens());
    }

    @Test
    @DisplayName("Parse Gemini response strips markdown fences if present")
    void testParseGeminiResponseWithFences() {
        String json = """
            {
              "candidates": [
                {
                  "content": {
                    "parts": [
                      {
                        "text": "```json\\n{\\"isInterviewContent\\": false}\\n```"
                      }
                    ]
                  }
                }
              ]
            }
            """;

        GeminiResponse response = geminiClient.parseGeminiResponse(json);

        assertEquals("{\"isInterviewContent\": false}", response.content());
    }

    @Test
    @DisplayName("Throws exception if candidates array is missing")
    void testParseGeminiResponseNoCandidates() {
        String json = "{\"error\": \"quota exceeded\"}";
        assertThrows(RuntimeException.class, () -> geminiClient.parseGeminiResponse(json));
    }
}

