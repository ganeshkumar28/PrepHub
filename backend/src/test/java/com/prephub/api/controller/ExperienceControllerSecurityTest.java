package com.prephub.api.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prephub.api.config.CorsConfig;
import com.prephub.api.config.SecurityConfig;
import com.prephub.api.dto.ExperienceDto;
import com.prephub.api.dto.ExperienceInputDto;
import com.prephub.api.dto.QuestionInputDto;
import com.prephub.api.dto.RoundInputDto;
import com.prephub.api.entity.InterviewMode;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import com.prephub.api.entity.QuestionType;
import com.prephub.api.entity.RoundType;
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

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest
@Import({SecurityConfig.class, CorsConfig.class, GlobalExceptionHandler.class})
class ExperienceControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private JwtDecoder jwtDecoder;

    @MockBean
    private ExperienceService experienceService;

    @MockBean
    private ExtractionService extractionService;

    @MockBean
    private ProfileService profileService;

    @MockBean
    private CompanyService companyService;

    @MockBean
    private TopicService topicService;

    @MockBean
    private QuestionService questionService;

    private ExperienceInputDto createValidInput() {
        QuestionInputDto question = new QuestionInputDto(
            "What is an inverted index?",
            QuestionType.THEORY,
            null,
            List.of("database")
        );
        RoundInputDto round = new RoundInputDto(
            1,
            RoundType.TECHNICAL,
            45,
            "Good round",
            List.of(question)
        );
        return new ExperienceInputDto(
            "Google",
            "Software Engineer",
            Level.MID,
            new BigDecimal("3.0"),
            "Mountain View",
            2024,
            5,
            InterviewMode.ONSITE,
            Outcome.SELECTED,
            "Great experience",
            false,
            List.of(round)
        );
    }

    @Test
    @DisplayName("PUT /api/v1/experiences/{id} with author JWT succeeds (200)")
    void putExperience_withAuthorJwt_returnsOk() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();
        ExperienceInputDto input = createValidInput();

        ExperienceDto expectedDto = new ExperienceDto(
            expId,
            null,
            false,
            null,
            input.roleTitle(),
            input.level(),
            input.yearsOfExperience(),
            input.location(),
            input.interviewYear(),
            input.interviewMonth(),
            input.interviewMode(),
            input.outcome(),
            input.summary(),
            List.of(),
            Instant.now()
        );

        when(experienceService.updateExperience(eq(expId), any(ExperienceInputDto.class), any()))
            .thenReturn(expectedDto);

        mockMvc.perform(put("/api/v1/experiences/{id}", expId)
                .with(jwt().jwt(builder -> builder.subject(authorId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(expId.toString()))
            .andExpect(jsonPath("$.roleTitle").value(input.roleTitle()));
    }

    @Test
    @DisplayName("PUT /api/v1/experiences/{id} with different user returns 403 Forbidden with problem+json")
    void putExperience_withNonAuthorJwt_returnsForbidden() throws Exception {
        UUID nonAuthorId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();
        ExperienceInputDto input = createValidInput();

        when(experienceService.updateExperience(eq(expId), any(ExperienceInputDto.class), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this experience"));

        mockMvc.perform(put("/api/v1/experiences/{id}", expId)
                .with(jwt().jwt(builder -> builder.subject(nonAuthorId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.detail").value("Not allowed to edit this experience"));
    }

    @Test
    @DisplayName("DELETE /api/v1/experiences/{id} with different user returns 403 Forbidden with problem+json")
    void deleteExperience_withNonAuthorJwt_returnsForbidden() throws Exception {
        UUID nonAuthorId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();

        doThrow(new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this experience"))
            .when(experienceService).deleteExperience(eq(expId), any());

        mockMvc.perform(delete("/api/v1/experiences/{id}", expId)
                .with(jwt().jwt(builder -> builder.subject(nonAuthorId.toString()))))
            .andExpect(status().isForbidden())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(403))
            .andExpect(jsonPath("$.title").value("Forbidden"))
            .andExpect(jsonPath("$.detail").value("Not allowed to delete this experience"));
    }

    @Test
    @DisplayName("DELETE /api/v1/experiences/{id} with author JWT succeeds (204 No Content)")
    void deleteExperience_withAuthorJwt_returnsNoContent() throws Exception {
        UUID authorId = UUID.randomUUID();
        UUID expId = UUID.randomUUID();

        doNothing().when(experienceService).deleteExperience(eq(expId), any());

        mockMvc.perform(delete("/api/v1/experiences/{id}", expId)
                .with(jwt().jwt(builder -> builder.subject(authorId.toString()))))
            .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("POST /api/v1/extractions/{jobId}/publish returns 409 Conflict when job not owned by caller")
    void publish_mismatchedUser_returnsConflict() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        ExperienceInputDto input = createValidInput();

        when(experienceService.publishExtraction(eq(jobId), any(ExperienceInputDto.class), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Extraction job does not belong to the calling user"));

        mockMvc.perform(post("/api/v1/extractions/{jobId}/publish", jobId)
                .with(jwt().jwt(builder -> builder.subject(callerId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.title").value("Conflict"))
            .andExpect(jsonPath("$.detail").value("Extraction job does not belong to the calling user"));
    }

    @Test
    @DisplayName("POST /api/v1/extractions/{jobId}/publish returns 409 Conflict when job is not SUCCEEDED")
    void publish_notSucceeded_returnsConflict() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        ExperienceInputDto input = createValidInput();

        when(experienceService.publishExtraction(eq(jobId), any(ExperienceInputDto.class), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Extraction job is not completed"));

        mockMvc.perform(post("/api/v1/extractions/{jobId}/publish", jobId)
                .with(jwt().jwt(builder -> builder.subject(callerId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Extraction job is not completed"));
    }

    @Test
    @DisplayName("POST /api/v1/extractions/{jobId}/publish returns 409 Conflict when already published")
    void publish_alreadyPublished_returnsConflict() throws Exception {
        UUID callerId = UUID.randomUUID();
        UUID jobId = UUID.randomUUID();
        ExperienceInputDto input = createValidInput();

        when(experienceService.publishExtraction(eq(jobId), any(ExperienceInputDto.class), any()))
            .thenThrow(new ResponseStatusException(HttpStatus.CONFLICT, "Extraction job already published"));

        mockMvc.perform(post("/api/v1/extractions/{jobId}/publish", jobId)
                .with(jwt().jwt(builder -> builder.subject(callerId.toString())))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(input)))
            .andExpect(status().isConflict())
            .andExpect(content().contentType("application/problem+json"))
            .andExpect(jsonPath("$.status").value(409))
            .andExpect(jsonPath("$.detail").value("Extraction job already published"));
    }
}

