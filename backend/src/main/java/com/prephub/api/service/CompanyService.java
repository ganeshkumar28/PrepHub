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
        return companyRepository.searchByNameOrAlias(query.trim())
            .stream()
            .map(c -> new CompanyDto(c.getSlug(), c.getName()))
            .toList();
    }

    @Transactional(readOnly = true)
    public Optional<Company> findBySlug(String slug) {
        return companyRepository.findBySlug(slug);
    }

    @Transactional
    public Company getOrCreateCompanyByName(String companyName) {
        if (companyName == null || companyName.isBlank()) {
            return null;
        }
        String trimmed = companyName.trim();
        return companyRepository.findByNameIgnoreCase(trimmed).orElseGet(() -> {
            String slug = trimmed.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("^-|-$", "");
            if (slug.isBlank()) {
                slug = "company-" + System.currentTimeMillis();
            }
            Company company = new Company(trimmed, slug, new ArrayList<>());
            return companyRepository.save(company);
        });
    }
}

