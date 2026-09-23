package com.prephub.api.service;

import com.prephub.api.dto.CompanyDto;
import com.prephub.api.entity.Company;
import com.prephub.api.repository.CompanyRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@Service
public class CompanyService {

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    @Transactional(readOnly = true)
    public List<CompanyDto> searchCompanies(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        String trimmed = query.trim();
        List<Company> results;
        try {
            results = companyRepository.searchByTrigramSimilarity(trimmed);
        } catch (Exception e) {
            // Fallback for non-Postgres / test environments without pg_trgm
            results = companyRepository.searchByNameOrAlias(trimmed);
            if (results.size() > 10) {
                results = results.subList(0, 10);
            }
        }
        return results.stream()
            .map(c -> new CompanyDto(c.getSlug(), c.getName()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Company> findBySlug(String slug) {
        return companyRepository.findBySlug(slug);
    }

    @Transactional
    public Company getOrCreateCompanyByName(String companyName) {
        return matchOrCreateCompany(companyName);
    }

    @Transactional
    public Company matchOrCreateCompany(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return null;
        }
        String trimmed = companyName.trim();
        // 1. Exact case-insensitive match
        Optional<Company> exactMatch = companyRepository.findByNameIgnoreCase(trimmed);
        if (exactMatch.isPresent()) {
            return exactMatch.get();
        }

        // 2. Trigram similarity match (> 0.4)
        try {
            Optional<Company> similarMatch = companyRepository.findMostSimilarByName(trimmed);
            if (similarMatch.isPresent()) {
                return similarMatch.get();
            }
        } catch (Exception ignored) {
            // Fallback for non-Postgres environments or environments without pg_trgm
        }

        // 3. Create new company
        String slug = generateSlug(trimmed);
        Company company = new Company(trimmed, slug, new ArrayList<>());
        return companyRepository.save(company);
    }

    private String generateSlug(String name) {
        String baseSlug = name.toLowerCase(Locale.ROOT)
            .replaceAll("[^a-z0-9]+", "-")
            .replaceAll("^-|-$", "");
        if (baseSlug.isBlank()) {
            baseSlug = "company-" + System.currentTimeMillis();
        }
        String slug = baseSlug;
        int counter = 1;
        while (companyRepository.findBySlug(slug).isPresent()) {
            slug = baseSlug + "-" + counter++;
        }
        return slug;
    }
}
