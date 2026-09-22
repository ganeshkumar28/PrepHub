package com.prephub.api.dto;

public record PageMetaDto(
    int page,
    int size,
    long totalElements,
    int totalPages
) {}

