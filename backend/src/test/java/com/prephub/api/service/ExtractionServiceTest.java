package com.prephub.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prephub.api.dto.CreateExtractionRequest;
import com.prephub.api.dto.ExtractionJobDto;
import com.prephub.api.entity.Company;
import com.prephub.api.entity.ExtractionJob;
import com.prephub.api.entity.JobStatus;
import com.prephub.api.entity.Profile;
import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.ExtractionJobRepository;
import com.prephub.api.repository.TopicRepository;
import com.prephub.api.service.gemini.ExtractionPromptBuilder;
import com.prephub.api.service.gemini.GeminiClient;
import com.prephub.api.service.gemini.GeminiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.oauth2.jwt.Jwt;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionServiceTest {

    @Mock
    private ExtractionJobRepository extractionJobRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private CompanyService companyService;

    @Mock
    private TopicRepository topicRepository;

    @Mock
    private ExtractionPromptBuilder promptBuilder;

    @Mock
    private GeminiClient geminiClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private ExtractionService extractionService;

    private Jwt dummyJwt;
    private Profile dummyProfile;

    @BeforeEach
    void setUp() {
        UUID userId = UUID.randomUUID();
        dummyJwt = Jwt.withTokenValue("mock-token")
            .header("alg", "none")
            .claim("sub", userId.toString())
            .build();

        dummyProfile = new Profile(userId, "Test User");
    }

    @Test
    @DisplayName("createExtraction runs synchronously, handles valid extraction with telemetry and topic filtering")
    void testCreateExtractionSuccess() {
        when(profileService.getOrCreateProfile(dummyJwt)).thenReturn(dummyProfile);
        when(extractionJobRepository.save(any(ExtractionJob.class))).thenAnswer(invocation -> {
            ExtractionJob j = invocation.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt with topics");

        String geminiJson = """
            {
              "isInterviewContent": true,
              "confidence": 0.98,
              "experience": {
                "companyName": "Google LLC",
                "roleTitle": "Software Engineer",
                "level": "MID",
                "yearsOfExperience": 3.5,
                "location": "Bangalore",
                "interviewYear": 2024,
                "interviewMonth": 5,
                "interviewMode": "HYBRID",
                "outcome": "SELECTED",
                "summary": "Overall positive interview experience.",
                "rounds": [
                  {
                    "roundNumber": 1,
                    "roundType": "TECHNICAL",
                    "durationMinutes": 45,
                    "notes": "DSA and Problem Solving",
                    "questions": [
                      {
                        "text": "Implement LRU Cache using linked hash map",
                        "questionType": "CODING",
                        "difficulty": "MEDIUM",
                        "topicSlugs": ["caching", "invalid-hallucinated-slug"]
                      }
                    ]
                  }
                ]
              },
              "suggestedNewTopics": ["WebAssembly"],
              "warnings": []
            }
            """;

        when(geminiClient.generateExtraction(any(), any()))
            .thenReturn(new GeminiResponse(geminiJson, 400, 150));

        Company canonicalCompany = new Company("Google", "google", new ArrayList<>());
        when(companyService.matchOrCreateCompany("Google LLC")).thenReturn(canonicalCompany);

        Topic validTopic = new Topic("Caching", "caching", TopicKind.SYSTEM_DESIGN);
        when(topicRepository.findAll()).thenReturn(List.of(validTopic));
        when(topicRepository.findBySlug("webassembly")).thenReturn(Optional.empty());

        String validRaw = "I gave an interview at Google for SDE2. Round 1 was coding on trees and caching, 45 minutes duration. Round 2 was system design.";
        CreateExtractionRequest request = new CreateExtractionRequest(validRaw);
        ExtractionJobDto result = extractionService.createExtraction(request, dummyJwt);

        assertNotNull(result);
        assertEquals(JobStatus.SUCCEEDED, result.status());
        assertNotNull(result.result());
        assertTrue(result.result().isInterviewContent());
        assertEquals("Google", result.result().experience().companyName());

        // Validate that invalid topic slug was dropped and caching remained
        List<String> topicSlugs = result.result().experience().rounds().get(0).questions().get(0).topicSlugs();
        assertEquals(List.of("caching"), topicSlugs);

        // Verify suggested new topic was auto-created as OTHER
        ArgumentCaptor<Topic> topicCaptor = ArgumentCaptor.forClass(Topic.class);
        verify(topicRepository).save(topicCaptor.capture());
        assertEquals("WebAssembly", topicCaptor.getValue().getName());
        assertEquals("webassembly", topicCaptor.getValue().getSlug());
        assertEquals(TopicKind.OTHER, topicCaptor.getValue().getKind());

        // Verify token telemetry was saved on job
        ArgumentCaptor<ExtractionJob> jobCaptor = ArgumentCaptor.forClass(ExtractionJob.class);
        verify(extractionJobRepository, org.mockito.Mockito.atLeastOnce()).save(jobCaptor.capture());
        ExtractionJob finalJob = jobCaptor.getValue();
        assertEquals(400, finalJob.getInputTokens());
        assertEquals(150, finalJob.getOutputTokens());
        assertNotNull(finalJob.getCompletedAt());
    }

    @Test
    @DisplayName("createExtraction fails when isInterviewContent is false (e.g. spam, recipe)")
    void testCreateExtractionNotInterviewContent() {
        when(profileService.getOrCreateProfile(dummyJwt)).thenReturn(dummyProfile);
        when(extractionJobRepository.save(any(ExtractionJob.class))).thenAnswer(invocation -> {
            ExtractionJob j = invocation.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt with topics");

        String nonInterviewJson = """
            {
              "isInterviewContent": false,
              "confidence": 0.1,
              "experience": null,
              "suggestedNewTopics": [],
              "warnings": ["Text appears to be a chocolate cake recipe."]
            }
            """;

        when(geminiClient.generateExtraction(any(), any()))
            .thenReturn(new GeminiResponse(nonInterviewJson, 100, 30));

        String cakeRecipe = "How to bake a delicious chocolate cake with eggs, flour, cocoa powder, sugar, butter, and baking soda. Mix well and bake at 350F for 30 minutes.";
        CreateExtractionRequest request = new CreateExtractionRequest(cakeRecipe);
        ExtractionJobDto result = extractionService.createExtraction(request, dummyJwt);

        assertNotNull(result);
        assertEquals(JobStatus.FAILED, result.status());
        assertEquals("This doesn't look like an interview experience", result.errorCode());
        assertNotNull(result.completedAt());
    }

    @Test
    @DisplayName("createExtraction marks job FAILED on Gemini API exception")
    void testCreateExtractionGeminiError() {
        when(profileService.getOrCreateProfile(dummyJwt)).thenReturn(dummyProfile);
        when(extractionJobRepository.save(any(ExtractionJob.class))).thenAnswer(invocation -> {
            ExtractionJob j = invocation.getArgument(0);
            if (j.getId() == null) {
                j.setId(UUID.randomUUID());
            }
            return j;
        });

        when(promptBuilder.buildSystemPrompt()).thenReturn("System prompt");
        when(geminiClient.generateExtraction(any(), any())).thenThrow(new RuntimeException("API connection timeout"));

        String validRaw = "I gave an interview at Google for SDE2. Round 1 was coding on trees and caching, 45 minutes duration. Round 2 was system design.";
        CreateExtractionRequest request = new CreateExtractionRequest(validRaw);
        ExtractionJobDto result = extractionService.createExtraction(request, dummyJwt);

        assertNotNull(result);
        assertEquals(JobStatus.FAILED, result.status());
        assertEquals("API connection timeout", result.errorCode());
        assertNotNull(result.completedAt());
    }
}
