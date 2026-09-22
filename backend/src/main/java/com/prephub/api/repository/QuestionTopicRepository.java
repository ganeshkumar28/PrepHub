package com.prephub.api.repository;

import com.prephub.api.entity.QuestionTopic;
import com.prephub.api.entity.QuestionTopicId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

@Repository
public interface QuestionTopicRepository extends JpaRepository<QuestionTopic, QuestionTopicId> {

    List<QuestionTopic> findByQuestionId(UUID questionId);

    List<QuestionTopic> findByQuestionIdIn(Collection<UUID> questionIds);

    void deleteByQuestionId(UUID questionId);
}

