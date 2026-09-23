package com.prephub.api.dto;

import com.prephub.api.entity.InterviewMode;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.math.BigDecimal;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ExperienceInputDto(
    @Size(max = 120)
    String companyName,

    @Size(max = 120)
    String roleTitle,

    Level level,

    @DecimalMin("0.0")
    @DecimalMax("60.0")
    BigDecimal yearsOfExperience,

    @Size(max = 120)
    String location,

    @Min(2000)
    @Max(2100)
    Integer interviewYear,

    @Min(1)
    @Max(12)
    Integer interviewMonth,

    InterviewMode interviewMode,

    @NotNull
    Outcome outcome,

    @Size(max = 1000)
    String summary,

    Boolean isAnonymous,

    @NotNull
    @Size(min = 1, max = 15)
    @Valid
    List<RoundInputDto> rounds
) {}

