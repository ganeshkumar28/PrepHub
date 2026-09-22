package com.prephub.api.repository;

import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByKind(TopicKind kind);

    Optional<Topic> findBySlug(String slug);

    List<Topic> findBySlugIn(Collection<String> slugs);
}

