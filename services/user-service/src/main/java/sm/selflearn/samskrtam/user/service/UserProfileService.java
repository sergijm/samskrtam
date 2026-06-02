package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.outbox.KeycloakAdminService;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.UpdateProfileRequest;
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.dto.PublicProfileResponse;
import sm.selflearn.samskrtam.user.model.UserRole; // UserRole остается в модели user-service

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KeycloakAdminService keycloakAdminService;

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.trace("updateProfile: userId={}", userId);

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setUsername(request.username());
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(userId)
                .eventType(OutboxEventType.PROFILE_UPDATED)
                .payload(toJson(Map.of(
                        "firstName", request.firstName(),
                        "lastName",  request.lastName(),
                        "username",  request.username()
                )))
                .build());

        log.debug("Profile updated and outbox event created: userId={}", userId);
        return mapUserProfileToResponse(profile);
    }

    @Transactional
    public UserProfile getUserProfile(UUID userId) { // Изменен возвращаемый тип на UserProfile
        Optional<UserProfile> existingProfile = profileRepository.findById(userId);

        if (existingProfile.isPresent()) {
            return existingProfile.get(); // Возвращаем сущность
        } else {
            log.info("UserProfile not found locally for userId: {}. Attempting to provision from Keycloak.", userId);
            Map<String, Object> keycloakUser;
            try {
                keycloakUser = keycloakAdminService.findUserById(userId);
            } catch (Exception e) {
                log.error("Failed to fetch user {} from Keycloak for provisioning: {}", userId, e.getMessage(), e);
                throw new UserNotFoundException("User not found in Keycloak or failed to fetch for ID: " + userId, e);
            }

            UserProfile newProfile = UserProfile.builder()
                    .id(userId)
                    .username((String) keycloakUser.get("username"))
                    .email((String) keycloakUser.get("email"))
                    .firstName((String) keycloakUser.get("firstName"))
                    .lastName((String) keycloakUser.get("lastName"))
                    .blocked(!(Boolean) keycloakUser.getOrDefault("enabled", true))
                    .role(determineUserRole(keycloakUser))
                    .build();

            profileRepository.save(newProfile);
            log.info("Provisioned new UserProfile from Keycloak for userId: {}", userId);
            return newProfile; // Возвращаем сущность
        }
    }

    // Новый метод для получения UserProfileResponse
    public UserProfileResponse getProfileResponse(UUID userId) {
        UserProfile userProfile = getUserProfile(userId); // Получаем сущность
        return mapUserProfileToResponse(userProfile); // Маппим в DTO
    }

    // Метод для маппинга UserProfile в UserProfileResponse
    public UserProfileResponse mapUserProfileToResponse(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getEmail(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getAvatarUrl(),
                userProfile.getRole(),
                userProfile.isBlocked(),
                userProfile.getCreatedAt()
        );
    }

    // Метод для маппинга UserProfile в PublicProfileResponse
    public PublicProfileResponse mapUserProfileToPublicResponse(UserProfile userProfile) {
        return new PublicProfileResponse(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getAvatarUrl(),
                userProfile.getRole(),
                userProfile.getCreatedAt()
        );
    }


    private UserRole determineUserRole(Map<String, Object> keycloakUser) {
        List<String> realmRoles = (List<String>) keycloakUser.get("realmRoles");
        if (realmRoles != null && realmRoles.contains("ADMIN")) {
            return UserRole.ADMIN;
        }
        return UserRole.STUDENT;
    }

    private String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (JsonProcessingException e) {
            log.error("Failed to convert object to JSON: {}", object, e);
            throw new RuntimeException("Failed to convert object to JSON", e);
        }
    }
}
