package com.prephub.api.dto;

import com.prephub.api.entity.JobStatus;

import java.time.Instant;
import java.util.UUID;

public record ExtractionJobDto(
    UUID id,
    JobStatus status,
    ExtractionResultDto result,
    String errorCode,
    Instant createdAt,
    Instant completedAt
) {}

