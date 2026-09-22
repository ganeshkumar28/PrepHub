package com.prephub.api.controller;

import com.prephub.api.dto.TopicDto;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.service.TopicService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/topics")
@Tag(name = "Reference")
public class TopicController {

    private final TopicService topicService;

    public TopicController(TopicService topicService) {
        this.topicService = topicService;
    }

    @GetMapping
    @Operation(operationId = "listTopics", summary = "Topic taxonomy")
    public List<TopicDto> listTopics(@RequestParam(required = false) TopicKind kind) {
        return topicService.listTopics(kind);
    }
}

