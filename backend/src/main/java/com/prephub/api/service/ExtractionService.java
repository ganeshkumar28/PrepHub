package com.prephub.api.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.prephub.api.dto.CreateExtractionRequest;
import com.prephub.api.dto.ExtractionJobDto;
import com.prephub.api.dto.ExtractionResultDto;
import com.prephub.api.entity.ExtractionJob;
import com.prephub.api.entity.JobStatus;
import com.prephub.api.entity.Profile;
import com.prephub.api.repository.ExtractionJobRepository;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

@Service
public class ExtractionService {

    private final ExtractionJobRepository extractionJobRepository;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper;

    public ExtractionService(ExtractionJobRepository extractionJobRepository,
                             ProfileService profileService,
                             ObjectMapper objectMapper) {
        this.extractionJobRepository = extractionJobRepository;
        this.profileService = profileService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public ExtractionJobDto createExtraction(CreateExtractionRequest request, Jwt jwt) {
        Profile user = profileService.getOrCreateProfile(jwt);

        ExtractionJob job = new ExtractionJob();
        job.setUser(user);
        job.setRawText(request.rawText());
        job.setStatus(JobStatus.QUEUED);

        ExtractionJob saved = extractionJobRepository.save(job);
        return mapToDto(saved);
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

