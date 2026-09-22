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
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
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
        Sort sortOrder = "oldest".equalsIgnoreCase(sort)
            ? Sort.by(Sort.Direction.ASC, "createdAt")
            : Sort.by(Sort.Direction.DESC, "createdAt");

        Specification<Experience> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), "PUBLISHED"));

            if (companySlug != null && !companySlug.isBlank()) {
                predicates.add(cb.equal(root.get("company").get("slug"), companySlug));
            }
            if (level != null) {
                predicates.add(cb.equal(root.get("level"), level));
            }
            if (outcome != null) {
                predicates.add(cb.equal(root.get("outcome"), outcome));
            }
            if (year != null) {
                predicates.add(cb.equal(root.get("interviewYear"), year));
            }
            if (q != null && !q.isBlank()) {
                String pattern = "%" + q.trim().toLowerCase() + "%";
                Predicate searchRole = cb.like(cb.lower(root.get("roleTitle")), pattern);
                Predicate searchSummary = cb.like(cb.lower(root.get("summary")), pattern);
                predicates.add(cb.or(searchRole, searchSummary));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Experience> expPage = experienceRepository.findAll(spec, PageRequest.of(page, size, sortOrder));

        List<ExperienceDto> content = expPage.getContent().stream()
            .map(this::toDto)
            .toList();

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
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction job not found");
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

        if (!experience.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to edit this experience");
        }

        Company company = companyService.getOrCreateCompanyByName(input.companyName());
        experience.setCompany(company);
        applyInputToExperience(experience, input);
        experience.setUpdatedAt(Instant.now());

        Experience saved = experienceRepository.save(experience);

        // Replace rounds and questions
        interviewRoundRepository.deleteByExperienceId(saved.getId());
        saveRoundsAndQuestions(saved, input.rounds());

        return toDto(saved);
    }

    @Transactional
    public void deleteExperience(UUID experienceId, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        Experience experience = experienceRepository.findById(experienceId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Experience not found"));

        if (!experience.getAuthor().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this experience");
        }

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
        UserDto authorDto = null;
        if (!Boolean.TRUE.equals(exp.getIsAnonymous()) && exp.getAuthor() != null) {
            authorDto = new UserDto(exp.getAuthor().getId(), exp.getAuthor().getDisplayName());
        }

        final CompanyDto companyDto = exp.getCompany() != null
            ? new CompanyDto(exp.getCompany().getSlug(), exp.getCompany().getName())
            : null;

        List<InterviewRound> rounds = interviewRoundRepository.findByExperienceIdOrderByRoundNumberAsc(exp.getId());
        List<Question> questions = questionRepository.findByExperienceId(exp.getId());
        List<QuestionTopic> questionTopics = questions.isEmpty()
            ? Collections.emptyList()
            : questionTopicRepository.findByQuestionIdIn(questions.stream().map(Question::getId).toList());

        Map<UUID, List<TopicDto>> topicsByQuestion = questionTopics.stream()
            .collect(Collectors.groupingBy(
                qt -> qt.getQuestion().getId(),
                Collectors.mapping(
                    qt -> new TopicDto(qt.getTopic().getSlug(), qt.getTopic().getName(), qt.getTopic().getKind()),
                    Collectors.toList()
                )
            ));

        List<RoundDto> roundDtos = rounds.stream()
            .map(r -> {
                List<QuestionDto> roundQuestions = questions.stream()
                    .filter(q -> q.getRound() != null && q.getRound().getId().equals(r.getId()))
                    .map(q -> new QuestionDto(
                        q.getId(),
                        exp.getId(),
                        q.getText(),
                        q.getQuestionType(),
                        q.getDifficulty(),
                        topicsByQuestion.getOrDefault(q.getId(), List.of()),
                        companyDto
                    ))
                    .toList();

                return new RoundDto(
                    r.getRoundNumber(),
                    r.getRoundType(),
                    r.getDurationMinutes(),
                    r.getNotes(),
                    roundQuestions
                );
            })
            .toList();

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
    }
}
