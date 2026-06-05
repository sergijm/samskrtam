package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.dto.UserGroupSummary;
import sm.selflearn.samskrtam.user.dto.UserSearchResponse;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.outbox.KeycloakAdminService;
import sm.selflearn.samskrtam.user.repository.GroupMemberRepository;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileSpecification; // Import UserProfileSpecification
import org.springframework.data.jpa.domain.Specification; // Import Specification

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.UpdateProfileRequest;
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.dto.PublicProfileResponse;
import sm.selflearn.samskrtam.user.model.UserRole;

import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProfileService {

    private final UserProfileRepository profileRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;
    private final KeycloakAdminService keycloakAdminService;
    private final GroupMemberRepository groupMemberRepository;

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
    public UserProfile getUserProfile(UUID userId) {
        Optional<UserProfile> existingProfile = profileRepository.findById(userId);

        if (existingProfile.isPresent()) {
            log.debug("UserProfile found locally for userId: {}. Roles: {}", userId, existingProfile.get().getRoles());
            return existingProfile.get();
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
                    .roles(determineUserRoles(keycloakUser))
                    .build();

            profileRepository.save(newProfile);
            log.info("Provisioned new UserProfile from Keycloak for userId: {}. Roles: {}", userId, newProfile.getRoles());
            return newProfile;
        }
    }

    public List<UserGroupSummary> getUserGroups(UUID userId) {
        log.debug("Fetching groups for user: {}", userId);
        return groupMemberRepository.findByUserId(userId).stream()
                .map(groupMember -> UserGroupSummary.builder()
                        .groupId(groupMember.getGroup().getId())
                        .groupName(groupMember.getGroup().getName())
                        .groupRole(groupMember.getGroup().getCurator().getId().equals(userId) ? "CURATOR" : "MEMBER")
                        .joinedAt(groupMember.getJoinedAt())
                        .build())
                .collect(Collectors.toList());
    }

    public List<UserSearchResponse> searchUsers(String query) {
        log.debug("Searching users with query: {}", query);
        Specification<UserProfile> spec = UserProfileSpecification.filterBy(query, null, null); // Use existing spec
        return profileRepository.findAll(spec).stream()
                .map(userProfile -> UserSearchResponse.builder()
                        .id(userProfile.getId())
                        .username(userProfile.getUsername())
                        .firstName(userProfile.getFirstName())
                        .lastName(userProfile.getLastName())
                        .email(userProfile.getEmail())
                        .build())
                .collect(Collectors.toList());
    }

    public UserProfileResponse getProfileResponse(UUID userId) {
        UserProfile userProfile = getUserProfile(userId);
        return mapUserProfileToResponse(userProfile);
    }

    public UserProfileResponse mapUserProfileToResponse(UserProfile userProfile) {
        return new UserProfileResponse(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getEmail(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getAvatarUrl(),
                userProfile.getRoles(),
                userProfile.isBlocked(),
                userProfile.getCreatedAt()
        );
    }

    public PublicProfileResponse mapUserProfileToPublicResponse(UserProfile userProfile) {
        return new PublicProfileResponse(
                userProfile.getId(),
                userProfile.getUsername(),
                userProfile.getFirstName(),
                userProfile.getLastName(),
                userProfile.getAvatarUrl(),
                userProfile.getRoles(),
                userProfile.getCreatedAt()
        );
    }

    private Set<UserRole> determineUserRoles(Map<String, Object> keycloakUser) {
        Set<UserRole> roles = new HashSet<>();
        @SuppressWarnings("unchecked")
        List<String> realmRoles = (List<String>) keycloakUser.get("realmRoles");

        log.debug("UserProfileService: Keycloak realmRoles received for user: {}", realmRoles);

        if (realmRoles != null) {
            if (realmRoles.contains("ADMIN")) {
                roles.add(UserRole.ADMIN);
            }
            // Always add STUDENT role if no specific roles are found or if ADMIN is present
            roles.add(UserRole.STUDENT);
        } else {
            // Default to STUDENT if no roles are found at all
            roles.add(UserRole.STUDENT);
        }
        log.debug("UserProfileService: Determined roles for user: {}", roles);
        return roles;
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
