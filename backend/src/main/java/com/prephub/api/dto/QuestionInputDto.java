package com.prephub.api.dto;

import com.prephub.api.entity.Difficulty;
import com.prephub.api.entity.QuestionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record QuestionInputDto(
    @NotBlank
    @Size(min = 5, max = 1000)
    String text,

    @NotNull
    QuestionType questionType,

    Difficulty difficulty,

    @Size(max = 5)
    List<String> topicSlugs
) {}

