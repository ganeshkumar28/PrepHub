package com.prephub.api.controller;

import com.prephub.api.dto.ExperienceDto;
import com.prephub.api.dto.ExperienceInputDto;
import com.prephub.api.dto.ExperiencePageDto;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import com.prephub.api.service.ExperienceService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/experiences")
@Tag(name = "Experiences")
public class ExperienceController {

    private final ExperienceService experienceService;

    public ExperienceController(ExperienceService experienceService) {
        this.experienceService = experienceService;
    }

    @GetMapping
    @Operation(operationId = "listExperiences", summary = "Browse and search published experiences")
    public ExperiencePageDto listExperiences(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String company,
        @RequestParam(required = false) List<String> topic,
        @RequestParam(required = false) Level level,
        @RequestParam(required = false) Outcome outcome,
        @RequestParam(required = false) Integer year,
        @RequestParam(defaultValue = "newest") String sort
    ) {
        return experienceService.listExperiences(page, size, q, company, topic, level, outcome, year, sort);
    }

    @GetMapping("/{experienceId}")
    @Operation(operationId = "getExperience", summary = "Get one experience with rounds and questions")
    public ExperienceDto getExperience(@PathVariable UUID experienceId) {
        return experienceService.getExperience(experienceId);
    }

    @PutMapping("/{experienceId}")
    @Operation(operationId = "updateExperience", summary = "Replace an experience you authored")
    public ExperienceDto updateExperience(
        @PathVariable UUID experienceId,
        @Valid @RequestBody ExperienceInputDto input,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return experienceService.updateExperience(experienceId, input, jwt);
    }

    @DeleteMapping("/{experienceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(operationId = "deleteExperience", summary = "Delete an experience you authored")
    public void deleteExperience(
        @PathVariable UUID experienceId,
        @AuthenticationPrincipal Jwt jwt
    ) {
        experienceService.deleteExperience(experienceId, jwt);
    }
}

