package com.prephub.api.dto;

import java.util.List;

public record ExperiencePageDto(
    List<ExperienceDto> content,
    PageMetaDto page
) {}

