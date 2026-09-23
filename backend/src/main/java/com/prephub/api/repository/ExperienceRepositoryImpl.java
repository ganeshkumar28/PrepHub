package com.prephub.api.repository;

import com.prephub.api.entity.Experience;
import com.prephub.api.entity.Level;
import com.prephub.api.entity.Outcome;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Repository
public class ExperienceRepositoryImpl implements ExperienceRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Experience> searchExperiences(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Level level,
        Outcome outcome,
        Integer year,
        String sort
    ) {
        try {
            return searchNative(page, size, q, companySlug, topicSlugs, level, outcome, year, sort);
        } catch (Exception e) {
            // Fallback to criteria search (e.g. for non-Postgres test environments)
            return searchCriteria(page, size, q, companySlug, topicSlugs, level, outcome, year, sort);
        }
    }

    @SuppressWarnings("unchecked")
    private Page<Experience> searchNative(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Level level,
        Outcome outcome,
        Integer year,
        String sort
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT e.* FROM experiences e ");
        sql.append("LEFT JOIN companies c ON c.id = e.company_id ");

        boolean hasTopics = topicSlugs != null && !topicSlugs.isEmpty();
        if (hasTopics) {
            sql.append("JOIN questions q_sub ON q_sub.experience_id = e.id ");
            sql.append("JOIN question_topics qt ON qt.question_id = q_sub.id ");
            sql.append("JOIN topics t ON t.id = qt.topic_id ");
        }

        sql.append("WHERE e.status = 'PUBLISHED' ");

        Map<String, Object> params = new HashMap<>();

        if (q != null && !q.isBlank()) {
            sql.append("AND e.search_tsv @@ plainto_tsquery('english', :q) ");
            params.put("q", q.trim());
        }

        if (companySlug != null && !companySlug.isBlank()) {
            sql.append("AND c.slug = :companySlug ");
            params.put("companySlug", companySlug.trim());
        }

        if (level != null) {
            sql.append("AND e.level = :level ");
            params.put("level", level.name());
        }

        if (outcome != null) {
            sql.append("AND e.outcome = :outcome ");
            params.put("outcome", outcome.name());
        }

        if (year != null) {
            sql.append("AND e.interview_year = :year ");
            params.put("year", year);
        }

        if (hasTopics) {
            sql.append("AND t.slug IN (:topicSlugs) ");
            params.put("topicSlugs", topicSlugs);
        }

        sql.append("GROUP BY e.id ");

        if ("oldest".equalsIgnoreCase(sort)) {
            sql.append("ORDER BY e.created_at ASC ");
        } else {
            sql.append("ORDER BY e.created_at DESC ");
        }

        Query query = entityManager.createNativeQuery(sql.toString(), Experience.class);
        params.forEach(query::setParameter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Experience> content = query.getResultList();

        // Count query
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT count(DISTINCT e.id) FROM experiences e ");
        countSql.append("LEFT JOIN companies c ON c.id = e.company_id ");
        if (hasTopics) {
            countSql.append("JOIN questions q_sub ON q_sub.experience_id = e.id ");
            countSql.append("JOIN question_topics qt ON qt.question_id = q_sub.id ");
            countSql.append("JOIN topics t ON t.id = qt.topic_id ");
        }
        countSql.append("WHERE e.status = 'PUBLISHED' ");

        if (q != null && !q.isBlank()) {
            countSql.append("AND e.search_tsv @@ plainto_tsquery('english', :q) ");
        }
        if (companySlug != null && !companySlug.isBlank()) {
            countSql.append("AND c.slug = :companySlug ");
        }
        if (level != null) {
            countSql.append("AND e.level = :level ");
        }
        if (outcome != null) {
            countSql.append("AND e.outcome = :outcome ");
        }
        if (year != null) {
            countSql.append("AND e.interview_year = :year ");
        }
        if (hasTopics) {
            countSql.append("AND t.slug IN (:topicSlugs) ");
        }

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private Page<Experience> searchCriteria(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Level level,
        Outcome outcome,
        Integer year,
        String sort
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Experience> query = cb.createQuery(Experience.class);
        Root<Experience> root = query.from(Experience.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("status"), "PUBLISHED"));

        if (companySlug != null && !companySlug.isBlank()) {
            predicates.add(cb.equal(root.get("company").get("slug"), companySlug.trim()));
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
            Predicate roleTitlePred = cb.like(cb.lower(root.get("roleTitle")), pattern);
            Predicate summaryPred = cb.like(cb.lower(root.get("summary")), pattern);
            predicates.add(cb.or(roleTitlePred, summaryPred));
        }

        query.where(predicates.toArray(new Predicate[0]));

        if ("oldest".equalsIgnoreCase(sort)) {
            query.orderBy(cb.asc(root.get("createdAt")));
        } else {
            query.orderBy(cb.desc(root.get("createdAt")));
        }

        List<Experience> content = entityManager.createQuery(query)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Experience> countRoot = countQuery.from(Experience.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }
}

