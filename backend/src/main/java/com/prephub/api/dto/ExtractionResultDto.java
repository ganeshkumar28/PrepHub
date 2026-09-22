package com.prephub.api.dto;

import java.util.List;

public record ExtractionResultDto(
    boolean isInterviewContent,
    Double confidence,
    ExperienceInputDto experience,
    List<String> suggestedNewTopics,
    List<String> warnings
) {}

