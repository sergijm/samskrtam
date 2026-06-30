package sm.selflearn.samskrtam.user.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import sm.selflearn.samskrtam.user.dto.PublicProfileResponse;
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.model.UserRole;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserProfileMapper {

    public UserProfileResponse toResponse(UserProfile userProfile) {
        String avatarUrl = normalizeAvatarUrl(userProfile.getAvatarUrl());

        return UserProfileResponse.builder()
                .id(userProfile.getId())
                .username(userProfile.getUsername())
                .email(userProfile.getEmail())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .avatarUrl(avatarUrl)
                .roles(userProfile.getRoles().stream().map(UserRole::name).collect(Collectors.toSet()))
                .quizSize(userProfile.getQuizSize())
                .build();
    }

    public PublicProfileResponse toPublicResponse(UserProfile userProfile) {
        String avatarUrl = normalizeAvatarUrl(userProfile.getAvatarUrl());

        return PublicProfileResponse.builder()
                .id(userProfile.getId())
                .username(userProfile.getUsername())
                .firstName(userProfile.getFirstName())
                .lastName(userProfile.getLastName())
                .avatarUrl(avatarUrl)
                .roles(userProfile.getRoles())
                .createdAt(userProfile.getCreatedAt())
                .build();
    }

    private String normalizeAvatarUrl(String avatarUrl) {
        if (avatarUrl != null && !avatarUrl.startsWith("http")) {
            return "http://" + avatarUrl;
        }
        return avatarUrl;
    }
}