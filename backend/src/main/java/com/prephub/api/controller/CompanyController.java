package com.prephub.api.controller;

import com.prephub.api.dto.CompanyDto;
import com.prephub.api.service.CompanyService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.constraints.NotNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/companies")
@Tag(name = "Reference")
public class CompanyController {

    private final CompanyService companyService;

    public CompanyController(CompanyService companyService) {
        this.companyService = companyService;
    }

    @GetMapping
    @Operation(operationId = "searchCompanies", summary = "Search companies by name or alias (autocomplete)")
    public List<CompanyDto> searchCompanies(@RequestParam @NotNull String q) {
        return companyService.searchCompanies(q);
    }
}

