package com.prephub.api.dto;

import com.prephub.api.entity.RoundType;

import java.util.List;

public record RoundDto(
    Integer roundNumber,
    RoundType roundType,
    Integer durationMinutes,
    String notes,
    List<QuestionDto> questions
) {}

