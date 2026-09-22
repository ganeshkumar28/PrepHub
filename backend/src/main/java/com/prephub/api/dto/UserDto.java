package com.prephub.api.dto;

import java.util.UUID;

public record UserDto(
    UUID id,
    String displayName
) {}

