package com.prephub.api.controller;

import com.prephub.api.config.CorsConfig;
import com.prephub.api.config.SecurityConfig;
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
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private TopicService topicService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private ExperienceService experienceService;

    @MockBean
    private QuestionService questionService;

    @Test
    @DisplayName("Validation error returns 400 with application/problem+json and field errors")
    void validationError_returns400ProblemJson() throws Exception {
        // Missing required field 'rawText' or too short
        mockMvc.perform(post("/api/v1/extractions")
                .with(jwt().jwt(b -> b.subject(UUID.randomUUID().toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"rawText\": \"too short\"}"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.errors").isArray())
            .andExpect(jsonPath("$.errors[0].field").value("rawText"));
    }

    @Test
    @DisplayName("Missing required query param returns 400 with application/problem+json")
    void missingQueryParam_returns400ProblemJson() throws Exception {
        // GET /api/v1/companies requires param 'q'
        mockMvc.perform(get("/api/v1/companies"))
            .andExpect(status().isBadRequest())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(400))
            .andExpect(jsonPath("$.title").value("Bad Request"))
            .andExpect(jsonPath("$.errors[0].field").value("q"));
    }

    @Test
    @DisplayName("Missing entity returns 404 with application/problem+json")
    void notFoundException_returns404ProblemJson() throws Exception {
        UUID unknownId = UUID.randomUUID();
        when(experienceService.getExperience(unknownId))
            .thenThrow(new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));

        mockMvc.perform(get("/api/v1/experiences/{id}", unknownId))
            .andExpect(status().isNotFound())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(404))
            .andExpect(jsonPath("$.title").value("Not Found"))
            .andExpect(jsonPath("$.detail").value("Experience not found"));
    }

    @Test
    @DisplayName("Unauthenticated request returns 401 with application/problem+json")
    void unauthenticated_returns401ProblemJson() throws Exception {
        mockMvc.perform(get("/api/v1/me"))
            .andExpect(status().isUnauthorized())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(401))
            .andExpect(jsonPath("$.title").value("Unauthorized"));
    }
}

