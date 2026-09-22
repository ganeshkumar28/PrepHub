package com.prephub.api.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class EntitySchemaReflectionTest {

    private Set<String> getDeclaredFieldNames(Class<?> clazz) {
        return Arrays.stream(clazz.getDeclaredFields())
            .filter(f -> !Modifier.isStatic(f.getModifiers()))
            .map(Field::getName)
            .collect(Collectors.toSet());
    }

    @Test
    @DisplayName("Profile entity matches V1__init.sql field-for-field: id, displayName, createdAt")
    void testProfileFields() {
        Set<String> fields = getDeclaredFieldNames(Profile.class);
        assertEquals(Set.of("id", "displayName", "createdAt"), fields);
        assertFalse(fields.contains("role"), "Profile must NOT contain 'role'");
        assertFalse(fields.contains("avatarUrl"), "Profile must NOT contain 'avatarUrl'");
        assertFalse(fields.contains("email"), "Profile must NOT contain 'email'");
    }

    @Test
    @DisplayName("Company entity matches V1__init.sql field-for-field: id, name, slug, aliases, createdAt")
    void testCompanyFields() {
        Set<String> fields = getDeclaredFieldNames(Company.class);
        assertEquals(Set.of("id", "name", "slug", "aliases", "createdAt"), fields);
    }

    @Test
    @DisplayName("Topic entity matches V1__init.sql field-for-field: id, name, slug, kind, createdAt")
    void testTopicFields() {
        Set<String> fields = getDeclaredFieldNames(Topic.class);
        assertEquals(Set.of("id", "name", "slug", "kind", "createdAt"), fields);
    }

    @Test
    @DisplayName("ExtractionJob entity matches V1__init.sql field-for-field: id, user, rawText, status, result, errorMessage, inputTokens, outputTokens, createdAt, completedAt")
    void testExtractionJobFields() {
        Set<String> fields = getDeclaredFieldNames(ExtractionJob.class);
        assertEquals(Set.of("id", "user", "rawText", "status", "result", "errorMessage", "inputTokens", "outputTokens", "createdAt", "completedAt"), fields);
    }

    @Test
    @DisplayName("Experience entity matches V1__init.sql field-for-field: 18 specific columns")
    void testExperienceFields() {
        Set<String> fields = getDeclaredFieldNames(Experience.class);
        assertEquals(Set.of(
            "id", "author", "isAnonymous", "extractionJob", "company", "companyNameRaw",
            "roleTitle", "level", "yearsOfExperience", "location", "interviewYear",
            "interviewMonth", "interviewMode", "outcome", "summary", "status",
            "createdAt", "updatedAt", "searchTsv"
        ), fields);
    }

    @Test
    @DisplayName("InterviewRound entity matches V1__init.sql field-for-field: id, experience, roundNumber, roundType, durationMinutes, notes")
    void testInterviewRoundFields() {
        Set<String> fields = getDeclaredFieldNames(InterviewRound.class);
        assertEquals(Set.of("id", "experience", "roundNumber", "roundType", "durationMinutes", "notes"), fields);
    }

    @Test
    @DisplayName("Question entity matches V1__init.sql field-for-field: id, experience, round, text, questionType, difficulty, searchTsv, createdAt")
    void testQuestionFields() {
        Set<String> fields = getDeclaredFieldNames(Question.class);
        assertEquals(Set.of("id", "experience", "round", "text", "questionType", "difficulty", "searchTsv", "createdAt"), fields);
    }

    @Test
    @DisplayName("QuestionTopic entity matches V1__init.sql field-for-field: id, question, topic, isPrimary")
    void testQuestionTopicFields() {
        Set<String> fields = getDeclaredFieldNames(QuestionTopic.class);
        assertEquals(Set.of("id", "question", "topic", "isPrimary"), fields);
    }
}

