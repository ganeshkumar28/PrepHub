package com.prephub.api.service.gemini;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.prephub.api.dto.ExtractionResultDto;
import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.TopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.mockito.Mockito;
import org.springframework.web.client.RestClient;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * Live sanity test against the real Gemini API with messy WhatsApp-style text.
 * Runs automatically if GEMINI_API_KEY is present in the environment:
 *   mvn test -Dtest=ExtractionLiveSanityTest
 */
@EnabledIfEnvironmentVariable(named = "GEMINI_API_KEY", matches = ".+")
class ExtractionLiveSanityTest {

    private final ObjectMapper objectMapper = new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);

    @Test
    @DisplayName("Live Sanity: Messy WhatsApp interview text produces clean, well-formed extraction JSON")
    void testLiveMessyWhatsAppExtraction() throws Exception {
        String apiKey = System.getenv("GEMINI_API_KEY");
        String model = System.getenv("GEMINI_MODEL") != null ? System.getenv("GEMINI_MODEL") : "gemini-3.5-flash-lite";

        TopicRepository topicRepo = Mockito.mock(TopicRepository.class);
        when(topicRepo.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of(
            new Topic("Java", "java", TopicKind.LANGUAGE),
            new Topic("Arrays & Strings", "arrays-strings", TopicKind.DSA),
            new Topic("Caching", "caching", TopicKind.SYSTEM_DESIGN),
            new Topic("Redis", "redis", TopicKind.DATABASE),
            new Topic("Microservices", "microservices", TopicKind.SYSTEM_DESIGN),
            new Topic("System Design Basics", "system-design-basics", TopicKind.SYSTEM_DESIGN),
            new Topic("Conflict Resolution", "conflict-resolution", TopicKind.BEHAVIORAL),
            new Topic("Leadership", "leadership", TopicKind.BEHAVIORAL)
        ));

        ExtractionPromptBuilder promptBuilder = new ExtractionPromptBuilder(topicRepo);
        GeminiClient geminiClient = new GeminiClient(
            apiKey,
            model,
            "https://generativelanguage.googleapis.com",
            objectMapper,
            RestClient.builder()
        );

        String messyWhatsAppText = """
            Hey guys!! Finally sharing my exp after getting the offer yesterday 🎉🎉
            Profile: 3.5 yrs exp backend dev, applied for L4 SSE at Google Bangalore.
            
            R1 (DSA): 45 mins. Started 5 mins late coz interviewer was having audio issues lol.
            Asked 1 question - variation of trapping rain water / 2 pointer array problem.
            Follow-up: what if array is streaming or huge? Talked about space complexity O(1). Solved & dry run done.
            
            R2 (System Design): 1 hr. Design whatsapp status / instagram stories feature.
            Discussed push vs pull model for fanout, redis caching for active status, s3/cdn for media storage.
            Interviewer was very deep into network partitions and consistency vs availability trade-offs (CAP theorem).
            
            R3 (Googlyness / HM): 45 mins with Director.
            Standard behavioral: Tell me about a time you had disagreement with tech lead on microservices vs monolith,
            how do you handle tight project deadlines.
            
            HR called after 1 week: Selected!! CTC offered was pretty good.
            All the best to everyone prepping!!
            """;

        String systemPrompt = promptBuilder.buildSystemPrompt();
        GeminiResponse response = geminiClient.generateExtraction(systemPrompt, messyWhatsAppText);

        System.out.println("==================================================");
        System.out.println("GEMINI LIVE RESPONSE TELEMETRY:");
        System.out.println("Input Tokens:  " + response.inputTokens());
        System.out.println("Output Tokens: " + response.outputTokens());
        System.out.println("==================================================");
        System.out.println("EXTRACTED JSON:");
        System.out.println(response.content());
        System.out.println("==================================================");

        ExtractionResultDto result = objectMapper.readValue(response.content(), ExtractionResultDto.class);

        assertTrue(result.isInterviewContent(), "Expected isInterviewContent to be true");
        assertNotNull(result.experience(), "Expected experience to be populated");
        assertNotNull(result.experience().companyName());
        assertTrue(result.experience().rounds().size() >= 2, "Expected at least 2 rounds extracted");

        System.out.println("Parsed Successfully! Company: " + result.experience().companyName());
        System.out.println("Role: " + result.experience().roleTitle());
        System.out.println("Rounds Extracted: " + result.experience().rounds().size());
    }

    @Test
    @DisplayName("Live Sanity: Non-interview text (recipe/spam) sets isInterviewContent to false")
    void testLiveNonInterviewText() throws Exception {
        Thread.sleep(1000); // polite pause between live API calls
        String apiKey = System.getenv("GEMINI_API_KEY");
        String model = System.getenv("GEMINI_MODEL") != null ? System.getenv("GEMINI_MODEL") : "gemini-3.5-flash-lite";

        TopicRepository topicRepo = Mockito.mock(TopicRepository.class);
        when(topicRepo.findAll(any(org.springframework.data.domain.Sort.class))).thenReturn(List.of());

        ExtractionPromptBuilder promptBuilder = new ExtractionPromptBuilder(topicRepo);
        GeminiClient geminiClient = new GeminiClient(
            apiKey,
            model,
            "https://generativelanguage.googleapis.com",
            objectMapper,
            RestClient.builder()
        );

        String spamText = """
            Hey friends! Here is the best chocolate cake recipe:
            Take 2 cups of flour, 1 cup sugar, 2 eggs, and 1 cup milk.
            Preheat oven to 350 degrees F and bake for 30 minutes!
            """;

        String systemPrompt = promptBuilder.buildSystemPrompt();
        GeminiResponse response = geminiClient.generateExtraction(systemPrompt, spamText);

        System.out.println("==================================================");
        System.out.println("NON-INTERVIEW TEXT TEST RESPONSE:");
        System.out.println(response.content());
        System.out.println("==================================================");

        ExtractionResultDto result = objectMapper.readValue(response.content(), ExtractionResultDto.class);
        assertFalse(result.isInterviewContent(), "Expected isInterviewContent to be false for recipe");
    }
}

