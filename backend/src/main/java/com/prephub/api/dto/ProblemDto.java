package com.prephub.api.dto;

import java.net.URI;
import java.util.List;

public record ProblemDto(
    URI type,
    String title,
    int status,
    String detail,
    URI instance,
    List<FieldErrorDto> errors
) {
    public static ProblemDto of(int status, String title, String detail) {
        return new ProblemDto(
            URI.create("about:blank"),
            title,
            status,
            detail,
            null,
            null
        );
    }

    public static ProblemDto of(int status, String title, String detail, List<FieldErrorDto> errors) {
        return new ProblemDto(
            URI.create("about:blank"),
            title,
            status,
            detail,
            null,
            errors
        );
    }
}

