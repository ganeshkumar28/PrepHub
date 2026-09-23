package com.prephub.api.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.prephub.api.entity.RoundType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record RoundInputDto(
    @NotNull
    @Min(1)
    Integer roundNumber,

    @NotNull
    RoundType roundType,

    @Min(1)
    Integer durationMinutes,

    @Size(max = 1000)
    String notes,

    @NotNull
    @Valid
    @Size(max = 50)
    List<QuestionInputDto> questions
) {}

