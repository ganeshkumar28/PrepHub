package com.prephub.api.controller;

import com.prephub.api.dto.CreateExtractionRequest;
import com.prephub.api.dto.ExperienceDto;
import com.prephub.api.dto.ExperienceInputDto;
import com.prephub.api.dto.ExtractionJobDto;
import com.prephub.api.service.ExperienceService;
import com.prephub.api.service.ExtractionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/extractions")
@Tag(name = "Extractions")
public class ExtractionController {

    private final ExtractionService extractionService;
    private final ExperienceService experienceService;

    public ExtractionController(ExtractionService extractionService,
                                ExperienceService experienceService) {
        this.extractionService = extractionService;
        this.experienceService = experienceService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(operationId = "createExtraction", summary = "Submit raw pasted text for AI extraction")
    public ExtractionJobDto createExtraction(@Valid @RequestBody CreateExtractionRequest request,
                                             @AuthenticationPrincipal Jwt jwt) {
        return extractionService.createExtraction(request, jwt);
    }

    @GetMapping("/{jobId}")
    @Operation(operationId = "getExtraction", summary = "Get extraction job status and result")
    public ExtractionJobDto getExtraction(@PathVariable UUID jobId,
                                          @AuthenticationPrincipal Jwt jwt) {
        return extractionService.getExtraction(jobId, jwt);
    }

    @PostMapping("/{jobId}/publish")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(operationId = "publishExtraction", summary = "Publish the (user-reviewed/edited) extraction result as an experience")
    public ExperienceDto publishExtraction(@PathVariable UUID jobId,
                                           @Valid @RequestBody ExperienceInputDto input,
                                           @AuthenticationPrincipal Jwt jwt) {
        return experienceService.publishExtraction(jobId, input, jwt);
    }
}

