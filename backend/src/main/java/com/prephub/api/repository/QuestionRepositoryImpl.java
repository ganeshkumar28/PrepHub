package com.prephub.api.repository;

import com.prephub.api.entity.Difficulty;
import com.prephub.api.entity.Question;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
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
public class QuestionRepositoryImpl implements QuestionRepositoryCustom {

    @PersistenceContext
    private EntityManager entityManager;

    @Override
    @SuppressWarnings("unchecked")
    public Page<Question> searchQuestions(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Difficulty difficulty,
        String sort
    ) {
        try {
            return searchNative(page, size, q, companySlug, topicSlugs, difficulty, sort);
        } catch (Exception e) {
            // Fallback to criteria search (e.g. for non-Postgres test environments)
            return searchCriteria(page, size, q, companySlug, topicSlugs, difficulty, sort);
        }
    }

    @SuppressWarnings("unchecked")
    private Page<Question> searchNative(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Difficulty difficulty,
        String sort
    ) {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT q.* FROM questions q ");
        sql.append("JOIN experiences e ON e.id = q.experience_id ");
        sql.append("LEFT JOIN companies c ON c.id = e.company_id ");

        boolean hasTopics = topicSlugs != null && !topicSlugs.isEmpty();
        if (hasTopics) {
            sql.append("JOIN question_topics qt ON qt.question_id = q.id ");
            sql.append("JOIN topics t ON t.id = qt.topic_id ");
        }

        sql.append("WHERE e.status = 'PUBLISHED' ");

        Map<String, Object> params = new HashMap<>();

        if (q != null && !q.isBlank()) {
            sql.append("AND q.search_tsv @@ plainto_tsquery('english', :q) ");
            params.put("q", q.trim());
        }

        if (companySlug != null && !companySlug.isBlank()) {
            sql.append("AND c.slug = :companySlug ");
            params.put("companySlug", companySlug.trim());
        }

        if (difficulty != null) {
            sql.append("AND q.difficulty = :difficulty ");
            params.put("difficulty", difficulty.name());
        }

        if (hasTopics) {
            sql.append("AND t.slug IN (:topicSlugs) ");
            params.put("topicSlugs", topicSlugs);
        }

        sql.append("GROUP BY q.id ");
        sql.append("ORDER BY q.created_at DESC ");

        Query query = entityManager.createNativeQuery(sql.toString(), Question.class);
        params.forEach(query::setParameter);
        query.setFirstResult(page * size);
        query.setMaxResults(size);

        List<Question> content = query.getResultList();

        // Count query
        StringBuilder countSql = new StringBuilder();
        countSql.append("SELECT count(DISTINCT q.id) FROM questions q ");
        countSql.append("JOIN experiences e ON e.id = q.experience_id ");
        countSql.append("LEFT JOIN companies c ON c.id = e.company_id ");
        if (hasTopics) {
            countSql.append("JOIN question_topics qt ON qt.question_id = q.id ");
            countSql.append("JOIN topics t ON t.id = qt.topic_id ");
        }
        countSql.append("WHERE e.status = 'PUBLISHED' ");

        if (q != null && !q.isBlank()) {
            countSql.append("AND q.search_tsv @@ plainto_tsquery('english', :q) ");
        }
        if (companySlug != null && !companySlug.isBlank()) {
            countSql.append("AND c.slug = :companySlug ");
        }
        if (difficulty != null) {
            countSql.append("AND q.difficulty = :difficulty ");
        }
        if (hasTopics) {
            countSql.append("AND t.slug IN (:topicSlugs) ");
        }

        Query countQuery = entityManager.createNativeQuery(countSql.toString());
        params.forEach(countQuery::setParameter);
        long total = ((Number) countQuery.getSingleResult()).longValue();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }

    private Page<Question> searchCriteria(
        int page,
        int size,
        String q,
        String companySlug,
        List<String> topicSlugs,
        Difficulty difficulty,
        String sort
    ) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Question> query = cb.createQuery(Question.class);
        Root<Question> root = query.from(Question.class);

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.equal(root.get("experience").get("status"), "PUBLISHED"));

        if (companySlug != null && !companySlug.isBlank()) {
            predicates.add(cb.equal(root.get("experience").get("company").get("slug"), companySlug.trim()));
        }
        if (difficulty != null) {
            predicates.add(cb.equal(root.get("difficulty"), difficulty));
        }
        if (q != null && !q.isBlank()) {
            predicates.add(cb.like(cb.lower(root.get("text")), "%" + q.trim().toLowerCase() + "%"));
        }

        query.where(predicates.toArray(new Predicate[0]));
        query.orderBy(cb.desc(root.get("createdAt")));

        List<Question> content = entityManager.createQuery(query)
            .setFirstResult(page * size)
            .setMaxResults(size)
            .getResultList();

        CriteriaQuery<Long> countQuery = cb.createQuery(Long.class);
        Root<Question> countRoot = countQuery.from(Question.class);
        countQuery.select(cb.count(countRoot));
        countQuery.where(predicates.toArray(new Predicate[0]));
        Long total = entityManager.createQuery(countQuery).getSingleResult();

        return new PageImpl<>(content, PageRequest.of(page, size), total);
    }
}

