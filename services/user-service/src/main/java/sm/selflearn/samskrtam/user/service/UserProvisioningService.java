package sm.selflearn.samskrtam.user.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.outbox.KeycloakAdminService;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserProvisioningService {

    private final UserProfileRepository profileRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final RoleResolver roleResolver;

    @Transactional
    public UserProfile provisionFromKeycloak(UUID userId) {
        log.info("Provisioning user from Keycloak for userId: {}", userId);
        Map<String, Object> keycloakUser;
        try {
            keycloakUser = keycloakAdminService.findUserById(userId);
        } catch (Exception e) {
            log.error("Failed to fetch user {} from Keycloak for provisioning: {}", userId, e.getMessage(), e);
            throw new UserNotFoundException("User not found in Keycloak or failed to fetch for ID: " + userId, e);
        }

        Set<sm.selflearn.samskrtam.user.model.UserRole> roles = roleResolver.fromKeycloakMap(keycloakUser);

        UserProfile newProfile = UserProfile.builder()
                .id(userId)
                .username((String) keycloakUser.get("username"))
                .email((String) keycloakUser.get("email"))
                .firstName((String) keycloakUser.get("firstName"))
                .lastName((String) keycloakUser.get("lastName"))
                .avatarUrl((String) keycloakUser.get("picture"))
                .blocked(!(Boolean) keycloakUser.getOrDefault("enabled", true))
                .roles(roles)
                .build();

        profileRepository.save(newProfile);
        log.info("Provisioned new UserProfile from Keycloak for userId: {}. Roles: {}", userId, roles);
        return newProfile;
    }
}