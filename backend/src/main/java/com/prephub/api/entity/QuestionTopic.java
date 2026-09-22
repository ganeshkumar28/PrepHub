package com.prephub.api.entity;

import jakarta.persistence.Column;
import jakarta.persistence.EmbeddedId;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapsId;
import jakarta.persistence.Table;

@Entity
@Table(name = "question_topics")
public class QuestionTopic {

    @EmbeddedId
    private QuestionTopicId id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("questionId")
    @JoinColumn(name = "question_id", nullable = false)
    private Question question;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @MapsId("topicId")
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary = false;

    public QuestionTopic() {
    }

    public QuestionTopic(Question question, Topic topic, Boolean isPrimary) {
        this.id = new QuestionTopicId(question.getId(), topic.getId());
        this.question = question;
        this.topic = topic;
        this.isPrimary = isPrimary != null ? isPrimary : false;
    }

    public QuestionTopicId getId() {
        return id;
    }

    public void setId(QuestionTopicId id) {
        this.id = id;
    }

    public Question getQuestion() {
        return question;
    }

    public void setQuestion(Question question) {
        this.question = question;
    }

    public Topic getTopic() {
        return topic;
    }

    public void setTopic(Topic topic) {
        this.topic = topic;
    }

    public Boolean getIsPrimary() {
        return isPrimary;
    }

    public void setIsPrimary(Boolean primary) {
        isPrimary = primary;
    }
}

