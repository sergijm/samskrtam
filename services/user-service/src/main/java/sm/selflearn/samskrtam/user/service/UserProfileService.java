package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.dto.PublicProfileResponse;
import sm.selflearn.samskrtam.user.dto.UpdateProfileRequest;
import sm.selflearn.samskrtam.user.dto.UserGroupSummary;
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.dto.UserSearchResponse;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.model.UserRole;
import sm.selflearn.samskrtam.user.repository.GroupMemberRepository;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileSpecification;

import java.time.Instant;

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
    private final GroupMemberRepository groupMemberRepository;
    private final JwtDecoder jwtDecoder;
    private final UserProvisioningService provisioningService;
    private final UserProfileMapper profileMapper;
    private final RoleResolver roleResolver;

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.trace("updateProfile: userId={}", userId);

        UserProfile profile = profileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        profile.setFirstName(request.getFirstName());
        profile.setLastName(request.getLastName());
        profile.setUsername(request.getUsername());
        profile.setQuizSize(request.getQuizSize());
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(userId)
                .eventType(OutboxEventType.PROFILE_UPDATED)
                .payload(toJson(Map.of(
                        "firstName", request.getFirstName(),
                        "lastName",  request.getLastName(),
                        "username",  request.getUsername(),
                        "quizSize",  request.getQuizSize()
                )))
                .build());

        log.debug("Profile updated and outbox event created: userId={}", userId);
                return profileMapper.toResponse(profile);
    }

    @Transactional
    public UserProfile getUserProfile(UUID userId) {
        Optional<UserProfile> existingProfile = profileRepository.findById(userId);

        if (existingProfile.isPresent()) {
            log.debug("UserProfile found locally for userId: {}. Roles: {}", userId, existingProfile.get().getRoles());
            return existingProfile.get();
        } else {
            log.info("UserProfile not found locally for userId: {}. Provisioning from Keycloak.", userId);
            return provisioningService.provisionFromKeycloak(userId);
        }
    }

    @Transactional
    public UserProfileResponse syncOAuth2Profile(String keycloakAccessToken, String provider) {
        log.debug("Syncing OAuth2 profile for provider: {}", provider);

        if (keycloakAccessToken == null || keycloakAccessToken.isBlank()) {
            log.error("Keycloak access token is null or empty for OAuth2 sync. Provider: {}", provider);
            throw new IllegalArgumentException("Keycloak access token cannot be null or empty for OAuth2 sync.");
        }

        Jwt jwt = jwtDecoder.decode(keycloakAccessToken);

        UUID userId = UUID.fromString(jwt.getSubject());
        String username = jwt.getClaimAsString("preferred_username");
        String email = jwt.getClaimAsString("email");
        String firstName = jwt.getClaimAsString("given_name");
        String lastName = jwt.getClaimAsString("family_name");
        String avatarUrl = jwt.getClaimAsString("picture");

        Set<UserRole> roles = roleResolver.fromJwt(jwt.getClaimAsMap("realm_access"));

        Optional<UserProfile> existingProfile = profileRepository.findById(userId);
        UserProfile userProfile;

        if (existingProfile.isPresent()) {
            userProfile = existingProfile.get();
            log.debug("Updating existing user profile for OAuth2 sync: userId={}", userId);
            userProfile.setUsername(username);
            userProfile.setEmail(email);
            userProfile.setFirstName(firstName);
            userProfile.setLastName(lastName);
            userProfile.setAvatarUrl(avatarUrl);
            userProfile.setRoles(roles);
            userProfile.setUpdatedAt(Instant.now());
        } else {
            log.debug("Creating new user profile for OAuth2 sync: userId={}", userId);
            userProfile = UserProfile.builder()
                    .id(userId)
                    .username(username)
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .avatarUrl(avatarUrl)
                    .roles(roles)
                    .blocked(false)
                    .createdAt(Instant.now())
                    .updatedAt(Instant.now())
                    .build();
        }

        UserProfile savedProfile = profileRepository.save(userProfile);
        log.info("OAuth2 profile synced successfully for userId: {}", savedProfile.getId());
        return profileMapper.toResponse(savedProfile);
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
        Specification<UserProfile> spec = UserProfileSpecification.filterBy(query, null, null);
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
        return profileMapper.toResponse(userProfile);
    }

    public PublicProfileResponse getPublicProfileResponse(UUID userId) {
        UserProfile userProfile = getUserProfile(userId);
        return profileMapper.toPublicResponse(userProfile);
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
