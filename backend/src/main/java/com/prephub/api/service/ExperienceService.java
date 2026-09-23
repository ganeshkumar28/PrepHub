package com.prephub.api.service;

import com.prephub.api.dto.CompanyDto;
import com.prephub.api.dto.ExperienceDto;
import com.prephub.api.dto.ExperienceInputDto;
import com.prephub.api.dto.ExperiencePageDto;
import com.prephub.api.dto.PageMetaDto;
import com.prephub.api.dto.QuestionDto;
import com.prephub.api.dto.QuestionInputDto;
import com.prephub.api.dto.RoundDto;
import com.prephub.api.dto.RoundInputDto;
import com.prephub.api.dto.TopicDto;
import com.prephub.api.dto.UserDto;
import com.prephub.api.entity.Company;
import com.prephub.api.entity.Experience;
import com.prephub.api.entity.ExtractionJob;
import com.prephub.api.entity.InterviewRound;
import com.prephub.api.entity.JobStatus;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import com.prephub.api.entity.Profile;
import com.prephub.api.entity.Question;
import com.prephub.api.entity.QuestionTopic;
import com.prephub.api.entity.Topic;
import com.prephub.api.repository.ExperienceRepository;
import com.prephub.api.repository.ExtractionJobRepository;
import com.prephub.api.repository.InterviewRoundRepository;
import com.prephub.api.repository.QuestionRepository;
import com.prephub.api.repository.QuestionTopicRepository;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExperienceService {

    private final ExperienceRepository experienceRepository;
    private final InterviewRoundRepository interviewRoundRepository;
    private final QuestionRepository questionRepository;
    private final QuestionTopicRepository questionTopicRepository;
    private final ExtractionJobRepository extractionJobRepository;
    private final ProfileService profileService;
    private final CompanyService companyService;
    private final TopicService topicService;

    public ExperienceService(ExperienceRepository experienceRepository,
                             InterviewRoundRepository interviewRoundRepository,
                             QuestionRepository questionRepository,
                             QuestionTopicRepository questionTopicRepository,
                             ExtractionJobRepository extractionJobRepository,
                             ProfileService profileService,
                             CompanyService companyService,
                             TopicService topicService) {
        this.experienceRepository = experienceRepository;
        this.interviewRoundRepository = interviewRoundRepository;
        this.questionRepository = questionRepository;
        this.questionTopicRepository = questionTopicRepository;
        this.extractionJobRepository = extractionJobRepository;
        this.profileService = profileService;
        this.companyService = companyService;
        this.topicService = topicService;
    }

    @Transactional(readOnly = true)
    public ExperiencePageDto listExperiences(int page, int size, String q, String companySlug,
                                            List<String> topicSlugs, Level level, Outcome outcome,
                                            Integer year, String sort) {
        Page<Experience> expPage = experienceRepository.searchExperiences(
            page, size, q, companySlug, topicSlugs, level, outcome, year, sort
        );

        List<ExperienceDto> content = toDtos(expPage.getContent());

        PageMetaDto pageMeta = new PageMetaDto(
            expPage.getNumber(),
            expPage.getSize(),
            expPage.getTotalElements(),
            expPage.getTotalPages()
        );

        return new ExperiencePageDto(content, pageMeta);
    }

    @Transactional(readOnly = true)
    public ExperienceDto getExperience(UUID experienceId) {
        Experience experience = experienceRepository.findById(experienceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));

        return toDto(experience);
    }

    @Transactional
    public ExperienceDto publishExtraction(UUID jobId, ExperienceInputDto input, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ExtractionJob job = extractionJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction job not found"));

        if (!job.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Extraction job does not belong to the calling user");
        }

        if (job.getStatus() != JobStatus.SUCCEEDED) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Extraction job is not completed");
        }

        if (experienceRepository.findByExtractionJobId(jobId).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Extraction job already published");
        }

        Profile author = profileService.getOrCreateProfile(jwt);
        Company company = companyService.getOrCreateCompanyByName(input.companyName());

        Experience experience = new Experience();
        experience.setAuthor(author);
        experience.setExtractionJob(job);
        experience.setCompany(company);
        applyInputToExperience(experience, input);
        experience.setStatus("PUBLISHED");

        Experience saved = experienceRepository.save(experience);
        saveRoundsAndQuestions(saved, input.rounds());

        return toDto(saved);
    }

    @Transactional
    public ExperienceDto updateExperience(UUID experienceId, ExperienceInputDto input, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Experience experience = experienceRepository.findById(experienceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));

        if (experience.getAuthor() == null || !experience.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this experience");
        }

        Company company = companyService.getOrCreateCompanyByName(input.companyName());
        experience.setCompany(company);
        applyInputToExperience(experience, input);
        experience.setUpdatedAt(Instant.now());

        Experience saved = experienceRepository.save(experience);

        // Wholesale replacement of rounds and questions
        List<Question> existingQuestions = questionRepository.findByExperienceId(saved.getId());
        if (!existingQuestions.isEmpty()) {
            List<UUID> qIds = existingQuestions.stream().map(Question::getId).toList();
            questionTopicRepository.deleteByQuestionIdIn(qIds);
            questionRepository.deleteByExperienceId(saved.getId());
        }
        interviewRoundRepository.deleteByExperienceId(saved.getId());

        saveRoundsAndQuestions(saved, input.rounds());

        return toDto(saved);
    }

    @Transactional
    public void deleteExperience(UUID experienceId, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Experience experience = experienceRepository.findById(experienceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));

        if (experience.getAuthor() == null || !experience.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this experience");
        }

        List<Question> existingQuestions = questionRepository.findByExperienceId(experience.getId());
        if (!existingQuestions.isEmpty()) {
            List<UUID> qIds = existingQuestions.stream().map(Question::getId).toList();
            questionTopicRepository.deleteByQuestionIdIn(qIds);
            questionRepository.deleteByExperienceId(experience.getId());
        }
        interviewRoundRepository.deleteByExperienceId(experience.getId());
        experienceRepository.delete(experience);
    }

    private void applyInputToExperience(Experience experience, ExperienceInputDto input) {
        experience.setCompanyNameRaw(input.companyName());
        experience.setRoleTitle(input.roleTitle());
        experience.setLevel(input.level());
        experience.setYearsOfExperience(input.yearsOfExperience());
        experience.setLocation(input.location());
        experience.setInterviewYear(input.interviewYear());
        experience.setInterviewMonth(input.interviewMonth());
        experience.setInterviewMode(input.interviewMode());
        experience.setOutcome(input.outcome());
        experience.setSummary(input.summary());
        experience.setIsAnonymous(Boolean.TRUE.equals(input.isAnonymous()));
    }

    private void saveRoundsAndQuestions(Experience experience, List<RoundInputDto> roundInputs) {
        if (roundInputs == null) {
            return;
        }

        for (RoundInputDto roundInput : roundInputs) {
            InterviewRound round = new InterviewRound();
            round.setExperience(experience);
            round.setRoundNumber(roundInput.roundNumber());
            round.setRoundType(roundInput.roundType());
            round.setDurationMinutes(roundInput.durationMinutes());
            round.setNotes(roundInput.notes());
            InterviewRound savedRound = interviewRoundRepository.save(round);

            if (roundInput.questions() != null) {
                for (QuestionInputDto qInput : roundInput.questions()) {
                    Question question = new Question();
                    question.setExperience(experience);
                    question.setRound(savedRound);
                    question.setText(qInput.text());
                    question.setQuestionType(qInput.questionType());
                    question.setDifficulty(qInput.difficulty());
                    Question savedQuestion = questionRepository.save(question);

                    if (qInput.topicSlugs() != null && !qInput.topicSlugs().isEmpty()) {
                        List<Topic> topics = topicService.findBySlugIn(qInput.topicSlugs());
                        boolean first = true;
                        for (Topic topic : topics) {
                            QuestionTopic qt = new QuestionTopic(savedQuestion, topic, first);
                            questionTopicRepository.save(qt);
                            first = false;
                        }
                    }
                }
            }
        }
    }

    public ExperienceDto toDto(Experience exp) {
        return toDtos(List.of(exp)).get(0);
    }

    public List<ExperienceDto> toDtos(List<Experience> experiences) {
        if (experiences.isEmpty()) {
            return List.of();
        }

        List<UUID> expIds = experiences.stream().map(Experience::getId).toList();
        List<InterviewRound> allRounds = interviewRoundRepository.findByExperienceIdInOrderByRoundNumberAsc(expIds);
        List<Question> allQuestions = questionRepository.findByExperienceIdIn(expIds);
        List<UUID> qIds = allQuestions.stream().map(Question::getId).toList();
        List<QuestionTopic> allQuestionTopics = qIds.isEmpty() ? List.of() : questionTopicRepository.findByQuestionIdIn(qIds);

        Map<UUID, List<TopicDto>> topicsByQuestion = allQuestionTopics.stream()
            .collect(Collectors.groupingBy(
                qt -> qt.getQuestion().getId(),
                Collectors.mapping(
                    qt -> new TopicDto(qt.getTopic().getSlug(), qt.getTopic().getName(), qt.getTopic().getKind()),
                    Collectors.toList()
                )
            ));

        Map<UUID, List<InterviewRound>> roundsByExp = allRounds.stream()
            .collect(Collectors.groupingBy(r -> r.getExperience().getId()));

        Map<UUID, List<Question>> questionsByRound = allQuestions.stream()
            .filter(q -> q.getRound() != null)
            .collect(Collectors.groupingBy(q -> q.getRound().getId()));

        return experiences.stream().map(exp -> {
            UserDto authorDto = null;
            if (!Boolean.TRUE.equals(exp.getIsAnonymous()) && exp.getAuthor() != null) {
                authorDto = new UserDto(exp.getAuthor().getId(), exp.getAuthor().getDisplayName());
            }

            final CompanyDto companyDto = exp.getCompany() != null
                ? new CompanyDto(exp.getCompany().getSlug(), exp.getCompany().getName())
                : null;

            List<InterviewRound> rounds = roundsByExp.getOrDefault(exp.getId(), List.of());
            List<RoundDto> roundDtos = rounds.stream().map(r -> {
                List<Question> roundQuestions = questionsByRound.getOrDefault(r.getId(), List.of());
                List<QuestionDto> qDtos = roundQuestions.stream().map(q -> new QuestionDto(
                    q.getId(),
                    exp.getId(),
                    q.getText(),
                    q.getQuestionType(),
                    q.getDifficulty(),
                    topicsByQuestion.getOrDefault(q.getId(), List.of()),
                    companyDto
                )).toList();

                return new RoundDto(
                    r.getRoundNumber(),
                    r.getRoundType(),
                    r.getDurationMinutes(),
                    r.getNotes(),
                    qDtos
                );
            }).toList();

            return new ExperienceDto(
                exp.getId(),
                authorDto,
                exp.getIsAnonymous(),
                companyDto,
                exp.getRoleTitle(),
                exp.getLevel(),
                exp.getYearsOfExperience(),
                exp.getLocation(),
                exp.getInterviewYear(),
                exp.getInterviewMonth(),
                exp.getInterviewMode(),
                exp.getOutcome(),
                exp.getSummary(),
                roundDtos,
                exp.getCreatedAt()
            );
        }).toList();
    }
}
