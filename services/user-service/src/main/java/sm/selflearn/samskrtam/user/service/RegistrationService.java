package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.dto.RegisterRequest;
import sm.selflearn.samskrtam.user.exception.UserAlreadyExistsException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.model.UserRole;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;


import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RegistrationService {

    private final UserProfileRepository userProfileRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public UUID registerNewUser(RegisterRequest request) {
        log.trace("registerNewUser: username={}, email={}", request.username(), request.email());

        if (userProfileRepository.existsByUsername(request.username())) {
            throw new UserAlreadyExistsException("User with username '" + request.username() + "' already exists.");
        }
        if (userProfileRepository.existsByEmail(request.email())) {
            throw new UserAlreadyExistsException("User with email '" + request.email() + "' already exists.");
        }

        UserProfile newUser = UserProfile.builder()
                .id(UUID.randomUUID())
                .username(request.username())
                .email(request.email())
                .firstName(request.firstName())
                .lastName(request.lastName())
                .role(UserRole.STUDENT)
                .blocked(false)
                .build();

        userProfileRepository.save(newUser);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateId(newUser.getId())
                .eventType(OutboxEventType.USER_REGISTERED)
                .payload(toJson(Map.of(
                        "username", request.username(),
                        "email", request.email(),
                        "firstName", request.firstName() != null ? request.firstName() : "",
                        "lastName", request.lastName() != null ? request.lastName() : "",
                        "password", request.password()
                )))
                .build());

        log.debug("New user registered and outbox event created: userId={}", newUser.getId());
        return newUser.getId();
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
