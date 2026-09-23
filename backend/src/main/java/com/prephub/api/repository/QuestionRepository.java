package com.prephub.api.repository;

import com.prephub.api.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionRepository extends JpaRepository<Question, UUID>, JpaSpecificationExecutor<Question>, QuestionRepositoryCustom {

    List<Question> findByExperienceId(UUID experienceId);

    List<Question> findByExperienceIdIn(Collection<UUID> experienceIds);

    List<Question> findByRoundId(UUID roundId);

    void deleteByExperienceId(UUID experienceId);
}

