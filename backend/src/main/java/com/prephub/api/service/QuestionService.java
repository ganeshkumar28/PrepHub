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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

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
        Sort sortOrder = Sort.by(Sort.Direction.DESC, "createdAt");

        Specification<Question> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            // Only published experiences
            predicates.add(cb.equal(root.get("experience").get("status"), "PUBLISHED"));

            if (companySlug != null && !companySlug.isBlank()) {
                predicates.add(cb.equal(root.get("experience").get("company").get("slug"), companySlug));
            }
            if (difficulty != null) {
                predicates.add(cb.equal(root.get("difficulty"), difficulty));
            }
            if (q != null && !q.isBlank()) {
                predicates.add(cb.like(cb.lower(root.get("text")), "%" + q.trim().toLowerCase() + "%"));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Question> questionPage = questionRepository.findAll(spec, PageRequest.of(page, size, sortOrder));

        List<QuestionDto> content = questionPage.getContent().stream()
            .map(this::toDto)
            .toList();

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
        List<QuestionTopic> questionTopics = questionTopicRepository.findByQuestionId(q.getId());
        List<TopicDto> topics = questionTopics.stream()
            .map(qt -> new TopicDto(qt.getTopic().getSlug(), qt.getTopic().getName(), qt.getTopic().getKind()))
            .toList();

        CompanyDto companyDto = null;
        if (q.getExperience() != null && q.getExperience().getCompany() != null) {
            companyDto = new CompanyDto(q.getExperience().getCompany().getSlug(), q.getExperience().getCompany().getName());
        }

        return new QuestionDto(
            q.getId(),
            q.getExperience() != null ? q.getExperience().getId() : null,
            q.getText(),
            q.getQuestionType(),
            q.getDifficulty(),
            topics,
            companyDto
        );
    }
}

