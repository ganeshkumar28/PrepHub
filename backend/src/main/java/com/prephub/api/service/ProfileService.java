package com.prephub.api.service;

import com.prephub.api.dto.UserDto;
import com.prephub.api.entity.Profile;
import com.prephub.api.repository.ProfileRepository;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.UUID;

@Service
public class ProfileService {

    private final ProfileRepository profileRepository;

    public ProfileService(ProfileRepository profileRepository) {
        this.profileRepository = profileRepository;
    }

    @Transactional
    public Profile getOrCreateProfile(Jwt jwt) {
        UUID userId = UUID.fromString(jwt.getSubject());
        return profileRepository.findById(userId).orElseGet(() -> {
            String displayName = extractDisplayName(jwt);
            Profile newProfile = new Profile(userId, displayName);
            return profileRepository.save(newProfile);
        });
    }

    @Transactional
    public UserDto getCurrentUser(Jwt jwt) {
        Profile profile = getOrCreateProfile(jwt);
        return new UserDto(profile.getId(), profile.getDisplayName());
    }

    private String extractDisplayName(Jwt jwt) {
        // Check standard claims or Supabase user_metadata
        if (jwt.hasClaim("user_metadata")) {
            Object metadataObj = jwt.getClaim("user_metadata");
            if (metadataObj instanceof Map<?, ?> metadata) {
                if (metadata.get("full_name") instanceof String fullName && !fullName.isBlank()) {
                    return fullName;
                }
                if (metadata.get("name") instanceof String name && !name.isBlank()) {
                    return name;
                }
                if (metadata.get("user_name") instanceof String userName && !userName.isBlank()) {
                    return userName;
                }
            }
        }
        if (jwt.hasClaim("name") && jwt.getClaimAsString("name") != null && !jwt.getClaimAsString("name").isBlank()) {
            return jwt.getClaimAsString("name");
        }
        if (jwt.hasClaim("email") && jwt.getClaimAsString("email") != null && !jwt.getClaimAsString("email").isBlank()) {
            String email = jwt.getClaimAsString("email");
            return email.contains("@") ? email.substring(0, email.indexOf("@")) : email;
        }
        return "User " + jwt.getSubject().substring(0, 8);
    }
}

