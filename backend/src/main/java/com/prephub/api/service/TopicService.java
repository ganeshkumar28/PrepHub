package com.prephub.api.service;

import com.prephub.api.dto.TopicDto;
import com.prephub.api.entity.Topic;
import com.prephub.api.entity.TopicKind;
import com.prephub.api.repository.TopicRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Service
public class TopicService {

    private final TopicRepository topicRepository;

    public TopicService(TopicRepository topicRepository) {
        this.topicRepository = topicRepository;
    }

    @Transactional(readOnly = true)
    public List<TopicDto> listTopics(TopicKind kind) {
        List<Topic> topics = (kind != null)
            ? topicRepository.findByKind(kind)
            : topicRepository.findAll();

        return topics.stream()
            .map(t -> new TopicDto(t.getSlug(), t.getName(), t.getKind()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Topic> findBySlug(String slug) {
        return topicRepository.findBySlug(slug);
    }

    @Transactional(readOnly = true)
    public List<Topic> findBySlugIn(Collection<String> slugs) {
        if (slugs == null || slugs.isEmpty()) {
            return List.of();
        }
        return topicRepository.findBySlugIn(slugs);
    }
}

