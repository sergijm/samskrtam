package sm.selflearn.samskrtam.user.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import sm.selflearn.samskrtam.user.dto.OAuthSyncRequest;
import sm.selflearn.samskrtam.user.dto.UserDto;
import sm.selflearn.samskrtam.user.repository.UserRepository;
import java.util.Optional;
import java.util.UUID;

@Service
@Slf4j
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;

    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Optional<UserDto> findById(UUID userId) {
        log.trace("Finding user by id: {}", userId);
        return userRepository.findById(userId)
                .map(user -> new UserDto(
                        user.getId(),
                        user.getUsername(),
                        user.getEmail(),
                        user.getFirstName(),
                        user.getLastName(),
                        null, // TODO: get from user_profiles
                        null  // TODO: get from user_profiles
                ));
    }

    @Override
    public void syncOAuth2User(OAuthSyncRequest request) { // Changed return type to void
        log.info("Syncing OAuth2 user: provider={}, keycloakToken={}", request.getProvider(), request.getKeycloakToken());

        // TODO: Implement actual logic:
        // 1. Use keycloakToken to fetch user info from Keycloak (e.g., via UserInfo endpoint or introspection)
        // 2. Find or create user in your database based on Keycloak user ID/email
        // No longer generating an app token here.
    }
}
