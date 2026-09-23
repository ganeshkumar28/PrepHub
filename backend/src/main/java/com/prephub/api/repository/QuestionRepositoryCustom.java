package com.prephub.api.repository;

import com.prephub.api.entity.Difficulty;
import com.prephub.api.entity.Question;
import org.springframework.data.domain.Page;

import java.util.List;

public interface QuestionRepositoryCustom {

    Page<Question> searchQuestions(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Difficulty difficulty,
        String sort
    );
}

