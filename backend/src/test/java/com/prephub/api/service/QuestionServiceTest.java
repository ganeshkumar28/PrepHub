package com.prephub.api.service;

import com.prephub.api.dto.QuestionDto;
import com.prephub.api.dto.QuestionPageDto;
import com.prephub.api.entity.Company;
import com.prephub.api.entity.Difficulty;
import com.prephub.api.entity.Experience;
import com.prephub.api.entity.Question;
import com.prephub.api.entity.QuestionTopic;
import com.prephub.api.entity.QuestionType;
import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.QuestionRepository;
import com.prephub.api.repository.QuestionTopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QuestionServiceTest {

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionTopicRepository questionTopicRepository;

    @InjectMocks
    private QuestionService questionService;

    @Test
    @DisplayName("getQuestion returns QuestionDto with topics and company")
    void getQuestion_found_returnsDto() {
        UUID qId = UUID.randomUUID();
        Question q = new Question();
        q.setId(qId);
        q.setText("What is an LSM Tree?");
        q.setQuestionType(QuestionType.THEORY);
        q.setDifficulty(Difficulty.MEDIUM);

        Experience exp = new Experience();
        exp.setId(UUID.randomUUID());
        Company company = new Company("Google", "google", new ArrayList<>());
        exp.setCompany(company);
        q.setExperience(exp);

        Topic topic = new Topic("Database", "database", TopicKind.DATABASE);
        QuestionTopic qt = new QuestionTopic(q, topic, true);

        when(questionRepository.findById(qId)).thenReturn(Optional.of(q));
        when(questionTopicRepository.findByQuestionIdIn(anyList())).thenReturn(List.of(qt));

        QuestionDto result = questionService.getQuestion(qId);

        assertNotNull(result);
        assertEquals(qId, result.id());
        assertEquals("What is an LSM Tree?", result.text());
        assertEquals(Difficulty.MEDIUM, result.difficulty());
        assertEquals(1, result.topics().size());
        assertEquals("database", result.topics().get(0).slug());
        assertNotNull(result.company());
        assertEquals("google", result.company().slug());
    }

    @Test
    @DisplayName("getQuestion throws 404 when question not found")
    void getQuestion_notFound_throws404() {
        UUID qId = UUID.randomUUID();
        when(questionRepository.findById(qId)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            questionService.getQuestion(qId)
        );
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
        assertEquals("Question not found", ex.getReason());
    }

    @Test
    @DisplayName("listQuestions returns paginated QuestionPageDto")
    void listQuestions_returnsPage() {
        Question q = new Question();
        q.setId(UUID.randomUUID());
        q.setText("Implement LRU Cache");
        q.setQuestionType(QuestionType.CODING);
        q.setDifficulty(Difficulty.HARD);

        when(questionRepository.searchQuestions(anyInt(), anyInt(), any(), any(), any(), any(), any()))
            .thenReturn(new PageImpl<>(List.of(q)));
        when(questionTopicRepository.findByQuestionIdIn(anyList())).thenReturn(List.of());

        QuestionPageDto page = questionService.listQuestions(0, 20, "LRU", null, null, Difficulty.HARD, "newest");

        assertNotNull(page);
        assertEquals(1, page.content().size());
        assertEquals("Implement LRU Cache", page.content().get(0).text());
        assertEquals(0, page.page().page());
        assertEquals(1, page.page().totalElements());
    }
}

