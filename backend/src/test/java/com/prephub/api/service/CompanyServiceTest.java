package com.prephub.api.service;

import com.prephub.api.entity.Company;
import com.prephub.api.repository.CompanyRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CompanyServiceTest {

    @Mock
    private CompanyRepository companyRepository;

    @InjectMocks
    private CompanyService companyService;

    @Test
    @DisplayName("Returns exact case-insensitive matched company")
    void testExactMatch() {
        Company google = new Company("Google", "google", new ArrayList<>());
        when(companyRepository.findByNameIgnoreCase("Google")).thenReturn(Optional.of(google));

        Company result = companyService.matchOrCreateCompany("Google");

        assertNotNull(result);
        assertEquals("Google", result.getName());
        verify(companyRepository, never()).findMostSimilarByName(any());
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Matches existing company via trigram similarity if exact match not found")
    void testTrigramSimilarityMatch() {
        Company google = new Company("Google", "google", new ArrayList<>());
        when(companyRepository.findByNameIgnoreCase("Google LLC")).thenReturn(Optional.empty());
        when(companyRepository.findMostSimilarByName("Google LLC")).thenReturn(Optional.of(google));

        Company result = companyService.matchOrCreateCompany("Google LLC");

        assertNotNull(result);
        assertEquals("Google", result.getName());
        verify(companyRepository, never()).save(any());
    }

    @Test
    @DisplayName("Creates new company when neither exact nor trigram match is found")
    void testCreateNewCompanyWhenNoMatch() {
        when(companyRepository.findByNameIgnoreCase("StartupXYZ")).thenReturn(Optional.empty());
        when(companyRepository.findMostSimilarByName("StartupXYZ")).thenReturn(Optional.empty());
        when(companyRepository.findBySlug("startupxyz")).thenReturn(Optional.empty());

        Company created = new Company("StartupXYZ", "startupxyz", new ArrayList<>());
        when(companyRepository.save(any(Company.class))).thenReturn(created);

        Company result = companyService.matchOrCreateCompany("StartupXYZ");

        assertNotNull(result);
        assertEquals("StartupXYZ", result.getName());
        assertEquals("startupxyz", result.getSlug());
        verify(companyRepository).save(any(Company.class));
    }
}

