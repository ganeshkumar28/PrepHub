package com.prephub.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;

import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class QuestionTopicId implements Serializable {

    @Column(name = "question_id", nullable = false)
    private UUID questionId;

    @Column(name = "topic_id", nullable = false)
    private UUID topicId;

    public QuestionTopicId() {
    }

    public QuestionTopicId(UUID questionId, UUID topicId) {
        this.questionId = questionId;
        this.topicId = topicId;
    }

    public UUID getQuestionId() {
        return questionId;
    }

    public void setQuestionId(UUID questionId) {
        this.questionId = questionId;
    }

    public UUID getTopicId() {
        return topicId;
    }

    public void setTopicId(UUID topicId) {
        this.topicId = topicId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        QuestionTopicId that = (QuestionTopicId) o;
        return Objects.equals(questionId, that.questionId) && Objects.equals(topicId, that.topicId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(questionId, topicId);
    }
}

