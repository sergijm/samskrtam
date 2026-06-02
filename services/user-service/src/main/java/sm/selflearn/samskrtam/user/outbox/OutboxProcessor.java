package sm.selflearn.samskrtam.user.outbox;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.OutboxStatus;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxProcessor {

    private static final int MAX_RETRIES = 5;

    private final OutboxEventRepository outboxRepository;
    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final ObjectMapper objectMapper;

    @Scheduled(fixedDelayString = "${outbox.processor.interval-ms}")
    @Transactional
    public void process() {
        List<OutboxEvent> pending = outboxRepository.findPendingEvents();
        if (pending.isEmpty()) {
            log.trace("No pending outbox events to process.");
            return;
        }

        log.debug("Processing {} outbox events", pending.size());

        for (OutboxEvent event : pending) {
            try {
                if (event.getEventType() == OutboxEventType.USER_REGISTERED) {
                    // Special handling for USER_REGISTERED: create user in Keycloak and update UserProfile ID
                    Map<String, Object> payload = keycloakAdminService.parsePayload(event.getPayload());
                    UUID keycloakUserId = keycloakAdminService.createKeycloakUser(payload);

                    // Find the UserProfile by its temporary ID and update it with the actual Keycloak ID
                    UserProfile userProfile = userProfileRepository.findById(event.getAggregateId())
                            .orElseThrow(() -> new UserNotFoundException("UserProfile not found for outbox event: " + event.getAggregateId()));
                    userProfile.setId(keycloakUserId); // Update with Keycloak's sub
                    userProfileRepository.save(userProfile);

                    // Update the outbox event's aggregateId to reflect the actual Keycloak ID
                    event.setAggregateId(keycloakUserId);
                    log.debug("User registered in Keycloak and UserProfile ID updated: oldId={}, newId={}",
                            event.getAggregateId(), keycloakUserId);
                } else {
                    // For all other event types, call the apply method in KeycloakAdminService
                    keycloakAdminService.apply(event);
                }

                event.setStatus(OutboxStatus.PROCESSED);
                event.setProcessedAt(Instant.now());
                log.debug("Outbox event processed: id={}, type={}", event.getId(), event.getEventType());
            } catch (Exception e) {
                event.setRetryCount(event.getRetryCount() + 1);
                event.setErrorMessage(e.getMessage());
                if (event.getRetryCount() >= MAX_RETRIES) {
                    event.setStatus(OutboxStatus.FAILED);
                    log.error("Outbox event failed after {} retries: id={}, type={}, error={}",
                            MAX_RETRIES, event.getId(), event.getEventType(), e.getMessage(), e);
                } else {
                    log.warn("Outbox event retry {}/{}: id={}, type={}, error={}",
                            event.getRetryCount(), MAX_RETRIES, event.getId(), event.getEventType(), e.getMessage());
                }
            }
            outboxRepository.save(event);
        }
    }
}
