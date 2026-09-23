package com.prephub.api.service;

import com.prephub.api.dto.CompanyDto;
import com.prephub.api.dto.PageMetaDto;
import com.prephub.api.dto.QuestionDto;
import com.prephub.api.dto.QuestionPageDto;
import com.prephub.api.dto.TopicDto;
import com.prephub.api.entity.Difficulty;
import com.prephub.api.entity.Question;
import com.prephub.api.entity.QuestionTopic;
import com.prephub.api.repository.QuestionRepository;
import com.prephub.api.repository.QuestionTopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class QuestionService {

    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;

    public QuestionService(QuestionRepository questionRepository,
                           QuestionTopicRepository questionTopicRepository) {
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
    }

    @Transactional(readOnly = true)
    public QuestionPageDto listQuestions(int page, int size, String q, String companySlug,
                                         List<String> topicSlugs, Difficulty difficulty, String sort) {
        Page<Question> questionPage = questionRepository.searchQuestions(
            page, size, q, companySlug, topicSlugs, difficulty, sort
        );

        List<QuestionDto> content = toDtos(questionPage.getContent());

        PageMetaDto pageMeta = new PageMetaDto(
            questionPage.getNumber(),
            questionPage.getSize(),
            questionPage.getTotalElements(),
            questionPage.getTotalPages()
        );

        return new QuestionPageDto(content, pageMeta);
    }

    @Transactional(readOnly = true)
    public QuestionDto getQuestion(UUID questionId) {
        Question question = questionRepository.findById(questionId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Question not found"));

        return toDto(question);
    }

    public QuestionDto toDto(Question q) {
        return toDtos(List.of(q)).get(0);
    }

    public List<QuestionDto> toDtos(List<Question> questions) {
        if (questions.isEmpty()) {
            return List.of();
        }

        List<UUID> qIds = questions.stream().map(Question::getId).toList();
        List<QuestionTopic> questionTopics = questionTopicRepository.findByQuestionIdIn(qIds);

        Map<UUID, List<TopicDto>> topicsByQuestion = questionTopics.stream()
            .collect(Collectors.groupingBy(
                qt -> qt.getQuestion().getId(),
                Collectors.mapping(
                    qt -> new TopicDto(qt.getTopic().getSlug(), qt.getTopic().getName(), qt.getTopic().getKind()),
                    Collectors.toList()
                )
            ));

        return questions.stream().map(q -> {
            CompanyDto companyDto = null;
            if (q.getExperience() != null && q.getExperience().getCompany() != null) {
                companyDto = new CompanyDto(
                    q.getExperience().getCompany().getSlug(),
                    q.getExperience().getCompany().getName()
                );
            }

            return new QuestionDto(
                q.getId(),
                q.getExperience() != null ? q.getExperience().getId() : null,
                q.getText(),
                q.getQuestionType(),
                q.getDifficulty(),
                topicsByQuestion.getOrDefault(q.getId(), List.of()),
                companyDto
            );
        }).toList();
    }
}
