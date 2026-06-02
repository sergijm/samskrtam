package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.dto.BlockUserResponse;
import sm.selflearn.samskrtam.user.exception.UserAlreadyBlockedException;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserBlockService {

    private final UserProfileRepository profileRepository;
    private final OutboxEventRepository outboxRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public BlockUserResponse blockUser(UUID targetId, UUID adminId) {
        log.trace("blockUser: targetId={}, adminId={}", targetId, adminId);

        UserProfile profile = profileRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        if (profile.isBlocked()) {
            throw new UserAlreadyBlockedException(targetId);
        }

        profile.setBlocked(true);
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(targetId)
                .eventType(OutboxEventType.USER_BLOCKED)
                .payload(toJson(Map.of("enabled", false)))
                .build());

        log.debug("User blocked: targetId={}, by adminId={}", targetId, adminId);
        return new BlockUserResponse(profile.getId(), profile.isBlocked(), profile.getUpdatedAt());
    }

    @Transactional
    public BlockUserResponse unblockUser(UUID targetId, UUID adminId) {
        log.trace("unblockUser: targetId={}, adminId={}", targetId, adminId);

        UserProfile profile = profileRepository.findById(targetId)
                .orElseThrow(() -> new UserNotFoundException(targetId));

        if (!profile.isBlocked()) {
            throw new UserAlreadyBlockedException("User " + targetId + " is not blocked.");
        }

        profile.setBlocked(false);
        profile.setUpdatedAt(Instant.now());
        profileRepository.save(profile);

        outboxRepository.save(OutboxEvent.builder()
                .aggregateId(targetId)
                .eventType(OutboxEventType.USER_UNBLOCKED)
                .payload(toJson(Map.of("enabled", true)))
                .build());

        log.debug("User unblocked: targetId={}, by adminId={}", targetId, adminId);
        return new BlockUserResponse(profile.getId(), profile.isBlocked(), profile.getUpdatedAt());
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
