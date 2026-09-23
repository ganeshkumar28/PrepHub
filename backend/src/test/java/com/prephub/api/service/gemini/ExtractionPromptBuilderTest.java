package com.prephub.api.service.gemini;

import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.TopicRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExtractionPromptBuilderTest {

    @Mock
    private TopicRepository topicRepository;

    @InjectMocks
    private ExtractionPromptBuilder promptBuilder;

    @Test
    @DisplayName("Prompt builder dynamically constructs topic list from database topics table")
    void testBuildSystemPromptUsesDatabaseTopics() {
        Topic t1 = new Topic("Java", "java", TopicKind.LANGUAGE);
        Topic t2 = new Topic("Spring Boot", "spring-boot", TopicKind.FRAMEWORK);
        Topic t3 = new Topic("Caching", "caching", TopicKind.SYSTEM_DESIGN);

        when(topicRepository.findAll(any(Sort.class))).thenReturn(List.of(t1, t2, t3));

        String prompt = promptBuilder.buildSystemPrompt();

        assertTrue(prompt.contains("SYSTEM:"));
        assertTrue(prompt.contains("You are extracting structured data"));
        assertTrue(prompt.contains("java: Java (LANGUAGE)"));
        assertTrue(prompt.contains("spring-boot: Spring Boot (FRAMEWORK)"));
        assertTrue(prompt.contains("caching: Caching (SYSTEM_DESIGN)"));
        assertTrue(prompt.contains("isInterviewContent"));
        assertTrue(prompt.contains("suggestedNewTopics"));
    }
}

