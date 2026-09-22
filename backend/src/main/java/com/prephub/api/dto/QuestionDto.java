package com.prephub.api.dto;

import com.prephub.api.entity.Difficulty;
import com.prephub.api.entity.QuestionType;

import java.util.List;
import java.util.UUID;

public record QuestionDto(
    UUID id,
    UUID experienceId,
    String text,
    QuestionType questionType,
    Difficulty difficulty,
    List<TopicDto> topics,
    CompanyDto company
) {}

