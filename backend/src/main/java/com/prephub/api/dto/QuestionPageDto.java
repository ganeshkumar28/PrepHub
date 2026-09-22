package com.prephub.api.dto;

import java.util.List;

public record QuestionPageDto(
    List<QuestionDto> content,
    PageMetaDto page
) {}

