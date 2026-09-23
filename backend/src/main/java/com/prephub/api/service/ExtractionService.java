package com.prephub.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prephub.api.dto.CreateExtractionRequest;
import com.prephub.api.dto.ExperienceInputDto;
import com.prephub.api.dto.ExtractionJobDto;
import com.prephub.api.dto.ExtractionResultDto;
import com.prephub.api.dto.QuestionInputDto;
import com.prephub.api.dto.RoundInputDto;
import com.prephub.api.entity.Company;
import com.prephub.api.entity.ExtractionJob;
import com.prephub.api.entity.JobStatus;
import com.prephub.api.entity.Outcome;
import com.prephub.api.entity.Profile;
import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.ExtractionJobRepository;
import com.prephub.api.repository.TopicRepository;
import com.prephub.api.service.gemini.ExtractionPromptBuilder;
import com.prephub.api.service.gemini.GeminiClient;
import com.prephub.api.service.gemini.GeminiResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class ExtractionService {

    private static final Logger log = LoggerFactory.getLogger(ExtractionService.class);

    private final ExtractionJobRepository extractionJobRepository;
    private final ProfileService profileService;
    private final CompanyService companyService;
    private final TopicRepository topicRepository;
    private final ExtractionPromptBuilder promptBuilder;
    private final GeminiClient geminiClient;
    private final ObjectMapper objectMapper;

    public ExtractionService(ExtractionJobRepository extractionJobRepository,
                             ProfileService profileService,
                             CompanyService companyService,
                             TopicRepository topicRepository,
                             ExtractionPromptBuilder promptBuilder,
                             GeminiClient geminiClient,
                             ObjectMapper objectMapper) {
        this.extractionJobRepository = extractionJobRepository;
        this.profileService = profileService;
        this.companyService = companyService;
        this.topicRepository = topicRepository;
        this.promptBuilder = promptBuilder;
        this.geminiClient = geminiClient;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExtractionJobDto createExtraction(CreateExtractionRequest request, Jwt jwt) {
        Profile user = profileService.getOrCreateProfile(jwt);

        ExtractionJob job = new ExtractionJob();
        job.setUser(user);
        job.setRawText(request.rawText());
        job.setStatus(JobStatus.RUNNING);
        job = extractionJobRepository.save(job);

        try {
            String systemPrompt = promptBuilder.buildSystemPrompt();
            GeminiResponse response = geminiClient.generateExtraction(systemPrompt, request.rawText());

            job.setInputTokens(response.inputTokens());
            job.setOutputTokens(response.outputTokens());

            ExtractionResultDto rawResult = objectMapper.readValue(response.content(), ExtractionResultDto.class);

            // 1. On isInterviewContent: false -> mark job FAILED with user-facing message
            if (!rawResult.isInterviewContent()) {
                job.setStatus(JobStatus.FAILED);
                job.setErrorMessage("This doesn't look like an interview experience");
                job.setCompletedAt(Instant.now());
                job = extractionJobRepository.save(job);
                return mapToDto(job);
            }

            // 2. Company matching: trigram similarity > 0.4 or create
            ExperienceInputDto exp = rawResult.experience();
            String canonicalCompanyName = null;
            if (exp != null && exp.companyName() != null && !exp.companyName().isBlank()) {
                Company matchedCompany = companyService.matchOrCreateCompany(exp.companyName());
                if (matchedCompany != null) {
                    canonicalCompanyName = matchedCompany.getName();
                }
            }

            // 3. Topic matching: validate topicSlugs exist, drop invalid with log warning
            Set<String> validSlugs = topicRepository.findAll().stream()
                .map(Topic::getSlug)
                .collect(Collectors.toSet());

            List<RoundInputDto> sanitizedRounds = new ArrayList<>();
            if (exp != null && exp.rounds() != null) {
                for (RoundInputDto round : exp.rounds()) {
                    List<QuestionInputDto> sanitizedQuestions = new ArrayList<>();
                    if (round.questions() != null) {
                        for (QuestionInputDto q : round.questions()) {
                            List<String> validQuestionSlugs = new ArrayList<>();
                            if (q.topicSlugs() != null) {
                                for (String slug : q.topicSlugs()) {
                                    if (validSlugs.contains(slug)) {
                                        validQuestionSlugs.add(slug);
                                    } else {
                                        log.warn("Dropping invalid topic slug: {}", slug);
                                    }
                                }
                            }
                            sanitizedQuestions.add(new QuestionInputDto(
                                q.text(),
                                q.questionType(),
                                q.difficulty(),
                                validQuestionSlugs
                            ));
                        }
                    }
                    sanitizedRounds.add(new RoundInputDto(
                        round.roundNumber(),
                        round.roundType(),
                        round.durationMinutes(),
                        round.notes(),
                        sanitizedQuestions
                    ));
                }
            }

            // 4. suggestedNewTopics: auto-create as new topics with kind 'OTHER'
            processSuggestedNewTopics(rawResult.suggestedNewTopics());

            Outcome outcome = (exp != null && exp.outcome() != null) ? exp.outcome() : Outcome.UNKNOWN;
            ExperienceInputDto sanitizedExp = (exp != null) ? new ExperienceInputDto(
                canonicalCompanyName,
                exp.roleTitle(),
                exp.level(),
                exp.yearsOfExperience(),
                exp.location(),
                exp.interviewYear(),
                exp.interviewMonth(),
                exp.interviewMode(),
                outcome,
                exp.summary(),
                exp.isAnonymous(),
                sanitizedRounds
            ) : null;

            ExtractionResultDto finalResult = new ExtractionResultDto(
                rawResult.isInterviewContent(),
                rawResult.confidence(),
                sanitizedExp,
                rawResult.suggestedNewTopics(),
                rawResult.warnings() != null ? rawResult.warnings() : List.of()
            );

            job.setResult(objectMapper.writeValueAsString(finalResult));
            job.setStatus(JobStatus.SUCCEEDED);
            job.setCompletedAt(Instant.now());
            job = extractionJobRepository.save(job);
            return mapToDto(job);

        } catch (Exception e) {
            log.error("Extraction processing failed for job {}", job.getId(), e);
            job.setStatus(JobStatus.FAILED);
            job.setErrorMessage(e.getMessage() != null ? e.getMessage() : "Extraction failed");
            job.setCompletedAt(Instant.now());
            job = extractionJobRepository.save(job);
            return mapToDto(job);
        }
    }

    private void processSuggestedNewTopics(List<String> suggestedTopics) {
        if (suggestedTopics == null || suggestedTopics.isEmpty()) {
            return;
        }
        for (String topicName : suggestedTopics) {
            if (topicName == null || topicName.isBlank()) {
                continue;
            }
            String trimmed = topicName.trim();
            String slug = trimmed.toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("^-|-$", "");
            if (slug.isBlank()) {
                continue;
            }
            if (topicRepository.findBySlug(slug).isEmpty()) {
                Topic newTopic = new Topic(trimmed, slug, TopicKind.OTHER);
                try {
                    topicRepository.save(newTopic);
                } catch (Exception ex) {
                    log.warn("Could not save suggested topic '{}': {}", trimmed, ex.getMessage());
                }
            }
        }
    }

    @Transactional(readOnly = true)
    public ExtractionJobDto getExtraction(UUID jobId, Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        ExtractionJob job = extractionJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction job not found"));

        if (!job.getUser().getId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction job not found");
        }

        return mapToDto(job);
    }

    @Transactional(readOnly = true)
    public ExtractionJob getExtractionJobEntity(UUID jobId) {
        return extractionJobRepository.findById(jobId)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Extraction job not found"));
    }

    public ExtractionJobDto mapToDto(ExtractionJob job) {
        ExtractionResultDto resultDto = null;
        if (job.getResult() != null && !job.getResult().isBlank()) {
            try {
                resultDto = objectMapper.readValue(job.getResult(), ExtractionResultDto.class);
            } catch (Exception ignored) {
                // If deserialization fails, keep resultDto null
            }
        }

        return new ExtractionJobDto(
            job.getId(),
            job.getStatus(),
            resultDto,
            job.getErrorMessage(),
            job.getCreatedAt(),
            job.getCompletedAt()
        );
    }
}
