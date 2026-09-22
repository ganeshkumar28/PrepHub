package com.prephub.api.controller;

import com.prephub.api.dto.QuestionDto;
import com.prephub.api.dto.QuestionPageDto;
import com.prephub.api.entity.Difficulty;
import com.prephub.api.service.QuestionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/questions")
@Tag(name = "Questions")
public class QuestionController {

    private final QuestionService questionService;

    public QuestionController(QuestionService questionService) {
        this.questionService = questionService;
    }

    @GetMapping
    @Operation(operationId = "listQuestions", summary = "Browse and search questions across experiences")
    public QuestionPageDto listQuestions(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @RequestParam(required = false) String q,
        @RequestParam(required = false) String company,
        @RequestParam(required = false) List<String> topic,
        @RequestParam(required = false) Difficulty difficulty,
        @RequestParam(defaultValue = "newest") String sort
    ) {
        return questionService.listQuestions(page, size, q, company, topic, difficulty, sort);
    }

    @GetMapping("/{questionId}")
    @Operation(operationId = "getQuestion", summary = "Get one question")
    public QuestionDto getQuestion(@PathVariable UUID questionId) {
        return questionService.getQuestion(questionId);
    }
}

