package com.prephub.api.repository;

import com.prephub.api.entity.Experience;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface ExperienceRepository extends JpaRepository<Experience, UUID>, JpaSpecificationExecutor<Experience> {

    Optional<Experience> findByIdAndAuthorId(UUID id, UUID authorId);

    Optional<Experience> findByExtractionJobId(UUID extractionJobId);
}

