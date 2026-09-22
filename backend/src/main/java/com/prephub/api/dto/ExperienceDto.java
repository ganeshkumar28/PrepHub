package com.prephub.api.dto;

import com.prephub.api.entity.InterviewMode;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record ExperienceDto(
    UUID id,
    UserDto author,
    Boolean isAnonymous,
    CompanyDto company,
    String roleTitle,
    Level level,
    BigDecimal yearsOfExperience,
    String location,
    Integer interviewYear,
    Integer interviewMonth,
    InterviewMode interviewMode,
    Outcome outcome,
    String summary,
    List<RoundDto> rounds,
    Instant createdAt
) {}

