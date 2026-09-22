package com.prephub.api.dto;

import com.prephub.api.entity.TopicKind;

public record TopicDto(
    String slug,
    String name,
    TopicKind kind
) {}

