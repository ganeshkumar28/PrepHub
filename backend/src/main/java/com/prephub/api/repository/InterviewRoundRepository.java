package com.prephub.api.repository;

import com.prephub.api.entity.InterviewRound;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface InterviewRoundRepository extends JpaRepository<InterviewRound, UUID> {

    List<InterviewRound> findByExperienceIdOrderByRoundNumberAsc(UUID experienceId);

    void deleteByExperienceId(UUID experienceId);
}

