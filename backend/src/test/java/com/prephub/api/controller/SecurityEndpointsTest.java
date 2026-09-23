package com.prephub.api.controller;

import com.prephub.api.config.CorsConfig;
import com.prephub.api.config.SecurityConfig;
import com.prephub.api.dto.CompanyDto;
import com.prephub.api.dto.TopicDto;
import com.prephub.api.dto.UserDto;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.service.CompanyService;
import com.prephub.api.service.ExperienceService;
import com.prephub.api.service.ExtractionService;
import com.prephub.api.service.ProfileService;
import com.prephub.api.service.QuestionService;
import com.prephub.api.service.TopicService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, CorsConfig.class})
class SecurityEndpointsTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private TopicService topicService;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private ExperienceService experienceService;

    @MockBean
    private QuestionService questionService;

    @Test
    @DisplayName("GET /api/v1/companies?q=test returns [] with no Authorization header (public endpoint)")
    void getCompanies_withoutAuth_returnsOk() throws Exception {
        when(companyService.searchCompanies(eq("test"))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/companies")
                .param("q", "test")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$.length()").value(0));
    }

    @Test
    @DisplayName("GET /api/v1/topics returns 200 with no Authorization header (public endpoint)")
    void getTopics_withoutAuth_returnsOk() throws Exception {
        when(topicService.listTopics(null)).thenReturn(List.of(
            new TopicDto("java", "Java", TopicKind.LANGUAGE)
        ));

        mockMvc.perform(get("/api/v1/topics")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].slug").value("java"))
            .andExpect(jsonPath("$[0].name").value("Java"));
    }

    @Test
    @DisplayName("GET /api/v1/me returns 401 with no Authorization header")
    void getMe_withoutAuth_returnsUnauthorized() throws Exception {
        mockMvc.perform(get("/api/v1/me")
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /api/v1/me returns valid User object when called with a valid Supabase JWT")
    void getMe_withValidJwt_returnsUser() throws Exception {
        UUID userId = UUID.randomUUID();
        when(profileService.getCurrentUser(any())).thenReturn(
            new UserDto(userId, "Test Engineer")
        );

        mockMvc.perform(get("/api/v1/me")
                .with(jwt().jwt(builder -> builder.subject(userId.toString()).claim("email", "test@example.com")))
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(userId.toString()))
            .andExpect(jsonPath("$.displayName").value("Test Engineer"));
    }

    @Test
    @DisplayName("POST /api/v1/extractions returns 401 without Authorization header")
    void postExtraction_withoutAuth_returnsUnauthorized() throws Exception {
        String validRawText = "This is a detailed interview experience containing more than one hundred characters of text describing rounds, questions, and other relevant information.";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/extractions")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rawText\": \"" + validRawText + "\"}"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /api/v1/extractions returns 202 with valid JWT and job status")
    void postExtraction_withValidJwt_returnsAccepted() throws Exception {
        UUID userId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        when(extractionService.createExtraction(any(), any())).thenReturn(
            new com.prephub.api.dto.ExtractionJobDto(
                jobId,
                com.prephub.api.entity.JobStatus.SUCCEEDED,
                null,
                null,
                java.time.Instant.now(),
                java.time.Instant.now()
            )
        );

        String validRawText = "This is a detailed interview experience containing more than one hundred characters of text describing rounds, questions, and other relevant information.";
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/extractions")
                .with(jwt().jwt(builder -> builder.subject(userId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rawText\": \"" + validRawText + "\"}"))
            .andExpect(status().isAccepted())
            .andExpect(jsonPath("$.id").value(jobId.toString()))
            .andExpect(jsonPath("$.status").value("SUCCEEDED"));
    }
}

