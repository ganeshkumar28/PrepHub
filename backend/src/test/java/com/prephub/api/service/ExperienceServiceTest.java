package com.prephub.api.service;

import com.prephub.api.dto.ExperienceDto;
import com.prephub.api.dto.ExperienceInputDto;
import com.prephub.api.dto.QuestionInputDto;
import com.prephub.api.dto.RoundInputDto;
import com.prephub.api.entity.Company;
import com.prephub.api.entity.Experience;
import com.prephub.api.entity.ExtractionJob;
import com.prephub.api.entity.InterviewMode;
import com.prephub.api.entity.InterviewRound;
import com.prephub.api.entity.JobStatus;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import com.prephub.api.entity.Profile;
import com.prephub.api.entity.Question;
import com.prephub.api.entity.QuestionType;
import com.prephub.api.entity.RoundType;
import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.ExperienceRepository;
import com.prephub.api.repository.ExtractionJobRepository;
import com.prephub.api.repository.InterviewRoundRepository;
import com.prephub.api.repository.QuestionRepository;
import com.prephub.api.repository.QuestionTopicRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ExperienceServiceTest {

    @Mock
    private ExperienceRepository experienceRepository;

    @Mock
    private InterviewRoundRepository interviewRoundRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private QuestionTopicRepository questionTopicRepository;

    @Mock
    private ExtractionJobRepository extractionJobRepository;

    @Mock
    private ProfileService profileService;

    @Mock
    private CompanyService companyService;

    @Mock
    private TopicService topicService;

    @InjectMocks
    private ExperienceService experienceService;

    private UUID userId;
    private UUID otherUserId;
    private Profile authorProfile;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        otherUserId = UUID.randomUUID();
        authorProfile = new Profile(userId, "Jane Doe");

        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
    }

    private ExperienceInputDto createSampleInput() {
        QuestionInputDto q = new QuestionInputDto("Explain ACID", QuestionType.THEORY, null, List.of("database"));
        RoundInputDto r = new RoundInputDto(1, RoundType.TECHNICAL, 45, "Tech round", List.of(q));
        return new ExperienceInputDto(
            "Amazon",
            "SDE II",
            Level.MID,
            new BigDecimal("4.0"),
            "Seattle",
            2024,
            6,
            InterviewMode.REMOTE,
            Outcome.SELECTED,
            "Good overall",
            false,
            List.of(r)
        );
    }

    @Test
    @DisplayName("publishExtraction fails with 409 if job does not belong to calling user")
    void publish_wrongUser_throws409() {
        UUID jobId = UUID.randomUUID();
        Profile otherUser = new Profile(otherUserId, "Other User");
        ExtractionJob job = new ExtractionJob();
        job.setId(jobId);
        job.setUser(otherUser);
        job.setStatus(JobStatus.SUCCEEDED);

        when(extractionJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            experienceService.publishExtraction(jobId, createSampleInput(), jwt)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Extraction job does not belong to the calling user", ex.getReason());
    }

    @Test
    @DisplayName("publishExtraction fails with 409 if job status is not SUCCEEDED")
    void publish_notSucceeded_throws409() {
        UUID jobId = UUID.randomUUID();
        ExtractionJob job = new ExtractionJob();
        job.setId(jobId);
        job.setUser(authorProfile);
        job.setStatus(JobStatus.RUNNING);

        when(extractionJobRepository.findById(jobId)).thenReturn(Optional.of(job));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            experienceService.publishExtraction(jobId, createSampleInput(), jwt)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Extraction job is not completed", ex.getReason());
    }

    @Test
    @DisplayName("publishExtraction fails with 409 if already published")
    void publish_alreadyPublished_throws409() {
        UUID jobId = UUID.randomUUID();
        ExtractionJob job = new ExtractionJob();
        job.setId(jobId);
        job.setUser(authorProfile);
        job.setStatus(JobStatus.SUCCEEDED);

        when(extractionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(experienceRepository.findByExtractionJobId(jobId)).thenReturn(Optional.of(new Experience()));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            experienceService.publishExtraction(jobId, createSampleInput(), jwt)
        );
        assertEquals(HttpStatus.CONFLICT, ex.getStatusCode());
        assertEquals("Extraction job already published", ex.getReason());
    }

    @Test
    @DisplayName("publishExtraction succeeds and creates experience with PUBLISHED status")
    void publish_valid_succeeds() {
        UUID jobId = UUID.randomUUID();
        ExtractionJob job = new ExtractionJob();
        job.setId(jobId);
        job.setUser(authorProfile);
        job.setStatus(JobStatus.SUCCEEDED);

        Company company = new Company("Amazon", "amazon", new ArrayList<>());
        Topic topic = new Topic("Database", "database", TopicKind.DATABASE);

        when(extractionJobRepository.findById(jobId)).thenReturn(Optional.of(job));
        when(experienceRepository.findByExtractionJobId(jobId)).thenReturn(Optional.empty());
        when(profileService.getOrCreateProfile(jwt)).thenReturn(authorProfile);
        when(companyService.getOrCreateCompanyByName("Amazon")).thenReturn(company);
        when(topicService.findBySlugIn(List.of("database"))).thenReturn(List.of(topic));

        UUID expId = UUID.randomUUID();
        when(experienceRepository.save(any(Experience.class))).thenAnswer(invocation -> {
            Experience exp = invocation.getArgument(0);
            exp.setId(expId);
            return exp;
        });

        InterviewRound savedRound = new InterviewRound();
        savedRound.setId(UUID.randomUUID());
        savedRound.setRoundNumber(1);
        when(interviewRoundRepository.save(any())).thenReturn(savedRound);

        Question savedQuestion = new Question();
        savedQuestion.setId(UUID.randomUUID());
        savedQuestion.setText("Explain ACID");
        when(questionRepository.save(any())).thenReturn(savedQuestion);

        when(interviewRoundRepository.findByExperienceIdInOrderByRoundNumberAsc(anyList()))
            .thenReturn(List.of());
        when(questionRepository.findByExperienceIdIn(anyList()))
            .thenReturn(List.of());

        ExperienceDto result = experienceService.publishExtraction(jobId, createSampleInput(), jwt);

        assertNotNull(result);
        assertEquals(expId, result.id());
        assertEquals("SDE II", result.roleTitle());
        verify(experienceRepository).save(any(Experience.class));
    }

    @Test
    @DisplayName("updateExperience fails with 403 when caller is not the author")
    void update_wrongAuthor_throws403() {
        UUID expId = UUID.randomUUID();
        Profile otherUser = new Profile(otherUserId, "Other User");
        Experience exp = new Experience();
        exp.setId(expId);
        exp.setAuthor(otherUser);

        when(experienceRepository.findById(expId)).thenReturn(Optional.of(exp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            experienceService.updateExperience(expId, createSampleInput(), jwt)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Not allowed to edit this experience", ex.getReason());
    }

    @Test
    @DisplayName("updateExperience replaces rounds and questions wholesale")
    void update_validAuthor_replacesWholesale() {
        UUID expId = UUID.randomUUID();
        Experience exp = new Experience();
        exp.setId(expId);
        exp.setAuthor(authorProfile);

        Question oldQuestion = new Question();
        oldQuestion.setId(UUID.randomUUID());
        oldQuestion.setExperience(exp);

        when(experienceRepository.findById(expId)).thenReturn(Optional.of(exp));
        when(companyService.getOrCreateCompanyByName("Amazon")).thenReturn(new Company("Amazon", "amazon", new ArrayList<>()));
        when(experienceRepository.save(any(Experience.class))).thenReturn(exp);
        when(questionRepository.findByExperienceId(expId)).thenReturn(List.of(oldQuestion));

        InterviewRound savedRound = new InterviewRound();
        savedRound.setId(UUID.randomUUID());
        savedRound.setRoundNumber(1);
        when(interviewRoundRepository.save(any())).thenReturn(savedRound);

        Question savedQuestion = new Question();
        savedQuestion.setId(UUID.randomUUID());
        when(questionRepository.save(any())).thenReturn(savedQuestion);

        when(interviewRoundRepository.findByExperienceIdInOrderByRoundNumberAsc(anyList()))
            .thenReturn(List.of());
        when(questionRepository.findByExperienceIdIn(anyList()))
            .thenReturn(List.of());

        experienceService.updateExperience(expId, createSampleInput(), jwt);

        // Verify wholesale deletion of old questions, topics, rounds
        verify(questionTopicRepository).deleteByQuestionIdIn(anyList());
        verify(questionRepository).deleteByExperienceId(expId);
        verify(interviewRoundRepository).deleteByExperienceId(expId);
    }

    @Test
    @DisplayName("deleteExperience fails with 403 when caller is not the author")
    void delete_wrongAuthor_throws403() {
        UUID expId = UUID.randomUUID();
        Profile otherUser = new Profile(otherUserId, "Other User");
        Experience exp = new Experience();
        exp.setId(expId);
        exp.setAuthor(otherUser);

        when(experienceRepository.findById(expId)).thenReturn(Optional.of(exp));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class, () ->
            experienceService.deleteExperience(expId, jwt)
        );
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
        assertEquals("Not allowed to delete this experience", ex.getReason());
    }

    @Test
    @DisplayName("deleteExperience succeeds for author")
    void delete_validAuthor_succeeds() {
        UUID expId = UUID.randomUUID();
        Experience exp = new Experience();
        exp.setId(expId);
        exp.setAuthor(authorProfile);

        when(experienceRepository.findById(expId)).thenReturn(Optional.of(exp));
        when(questionRepository.findByExperienceId(expId)).thenReturn(List.of());

        experienceService.deleteExperience(expId, jwt);

        verify(interviewRoundRepository).deleteByExperienceId(expId);
        verify(experienceRepository).delete(exp);
    }
}

