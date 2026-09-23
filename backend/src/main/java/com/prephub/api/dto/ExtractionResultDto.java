package com.prephub.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExtractionResultDto(
    boolean isInterviewContent,
    Double confidence,
    ExperienceInputDto experience,
    List<String> suggestedNewTopics,
    List<String> warnings
) {}

