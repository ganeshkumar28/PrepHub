package com.prephub.api.controller;

import com.prephub.api.dto.UserDto;
import com.prephub.api.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/me")
@Tag(name = "Profile")
public class ProfileController {

    private final ProfileService profileService;

    public ProfileController(ProfileService profileService) {
        this.profileService = profileService;
    }

    @GetMapping
    @Operation(operationId = "getCurrentUser", summary = "Current user profile (created on first call if it doesn't exist yet)")
    public UserDto getCurrentUser(@AuthenticationPrincipal Jwt jwt) {
        return profileService.getCurrentUser(jwt);
    }
}

