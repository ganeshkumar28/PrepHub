package com.prephub.api.repository;

import com.prephub.api.entity.Experience;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import org.springframework.data.domain.Page;

import java.util.List;

public interface ExperienceRepositoryCustom {

    Page<Experience> searchExperiences(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Level level,
        Outcome outcome,
        Integer year,
        String sort
    );
}

