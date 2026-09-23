package com.prephub.api.controller;

import com.prephub.api.dto.FieldErrorDto;
import com.prephub.api.dto.ProblemDto;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final MediaType PROBLEM_JSON = MediaType.parseMediaType("application/problem+json");

    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ProblemDto> handleResponseStatusException(ResponseStatusException ex) {
        HttpStatus status = HttpStatus.resolve(ex.getStatusCode().value());
        int statusCode = status != null ? status.value() : ex.getStatusCode().value();
        String title = status != null ? status.getReasonPhrase() : "Error";
        String detail = ex.getReason() != null ? ex.getReason() : title;

        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            title,
            statusCode,
            detail,
            null,
            null
        );

        return ResponseEntity
            .status(statusCode)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDto> handleValidationException(MethodArgumentNotValidException ex) {
        List<FieldErrorDto> fieldErrors = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.add(new FieldErrorDto(error.getField(), error.getDefaultMessage()));
        }

        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed for request parameters or body",
            null,
            fieldErrors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ProblemDto> handleHandlerMethodValidationException(HandlerMethodValidationException ex) {
        List<FieldErrorDto> fieldErrors = new ArrayList<>();
        ex.getAllValidationResults().forEach(result -> {
            String paramName = result.getMethodParameter().getParameterName();
            result.getResolvableErrors().forEach(err ->
                fieldErrors.add(new FieldErrorDto(paramName != null ? paramName : "parameter", err.getDefaultMessage()))
            );
        });

        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed for request parameters",
            null,
            fieldErrors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ProblemDto> handleConstraintViolationException(ConstraintViolationException ex) {
        List<FieldErrorDto> fieldErrors = new ArrayList<>();
        for (ConstraintViolation<?> violation : ex.getConstraintViolations()) {
            fieldErrors.add(new FieldErrorDto(violation.getPropertyPath().toString(), violation.getMessage()));
        }

        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            "Validation failed for request parameters",
            null,
            fieldErrors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ProblemDto> handleMissingServletRequestParameterException(MissingServletRequestParameterException ex) {
        List<FieldErrorDto> fieldErrors = List.of(
            new FieldErrorDto(ex.getParameterName(), "Required parameter is missing")
        );

        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            "Required request parameter '" + ex.getParameterName() + "' is not present",
            null,
            fieldErrors
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ProblemDto> handleMethodArgumentTypeMismatchException(MethodArgumentTypeMismatchException ex) {
        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            "Failed to convert parameter '" + ex.getName() + "'",
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ProblemDto> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex) {
        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            "Malformed request body or invalid format",
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDto> handleAccessDeniedException(AccessDeniedException ex) {
        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Forbidden",
            HttpStatus.FORBIDDEN.value(),
            ex.getMessage() != null ? ex.getMessage() : "Access denied",
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.FORBIDDEN)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ProblemDto> handleIllegalArgumentException(IllegalArgumentException ex) {
        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Bad Request",
            HttpStatus.BAD_REQUEST.value(),
            ex.getMessage(),
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.BAD_REQUEST)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDto> handleGenericException(Exception ex) {
        ProblemDto problem = new ProblemDto(
            URI.create("about:blank"),
            "Internal Server Error",
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            ex.getMessage() != null ? ex.getMessage() : "An unexpected error occurred",
            null,
            null
        );

        return ResponseEntity
            .status(HttpStatus.INTERNAL_SERVER_ERROR)
            .contentType(PROBLEM_JSON)
            .body(problem);
    }
}
