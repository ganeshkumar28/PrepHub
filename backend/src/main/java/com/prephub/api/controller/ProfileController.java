package com.prephub.api.controller;

import com.prephub.api.dto.ExperiencePageDto;
import com.prephub.api.dto.UserDto;
import com.prephub.api.service.ExperienceService;
import com.prephub.api.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profileService;
    private final ExperienceService experienceService;

    public ProfileController(ProfileService profileService, ExperienceService experienceService) {
        this.profileService = profileService;
        this.experienceService = experienceService;
    }

    @GetMapping
    @Operation(operationId = "getCurrentUser", summary = "Current user profile (created on first call if it doesn't exist yet)")
    public UserDto getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getCurrentUser(jwt);
    }

    @GetMapping("/experiences")
    @Operation(operationId = "getMyExperiences", summary = "List experiences authored by the current user")
    public ExperiencePageDto getMyExperiences(
        @RequestParam(defaultValue = "0") int page,
        @RequestParam(defaultValue = "20") int size,
        @AuthenticationPrincipal Jwt jwt
    ) {
        return experienceService.listMyExperiences(page, size, jwt);
    }
}

