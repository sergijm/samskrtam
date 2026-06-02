package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.dto.AdminUserListResponse;
import sm.selflearn.samskrtam.user.dto.UpdateProfileRequest;
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.model.OutboxEvent;
import sm.selflearn.samskrtam.user.model.OutboxEventType;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.model.UserRole;
import sm.selflearn.samskrtam.user.repository.OutboxEventRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;
import sm.selflearn.samskrtam.user.repository.UserProfileSpecification;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminUserService {

    private final UserProfileRepository userProfileRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;
    private final AvatarService avatarService; // Reusing AvatarService for admin avatar management

    public AdminUserListResponse getAllUsers(int page, int size, String sortBy, String sortDirection,
                                             String search, UserRole role, Boolean blocked) {
        Sort sort = Sort.by(Sort.Direction.fromString(sortDirection), sortBy);
        Pageable pageable = PageRequest.of(page, size, sort);

        Specification<UserProfile> spec = UserProfileSpecification.filterBy(search, role, blocked);

        Page<UserProfile> userPage = userProfileRepository.findAll(spec, pageable);

        return new AdminUserListResponse(
                userPage.getContent().stream().map(UserProfileResponse::from).toList(),
                userPage.getTotalPages(),
                userPage.getTotalElements(),
                userPage.getNumber(),
                userPage.getSize(),
                userPage.isFirst(),
                userPage.isLast()
        );
    }

    public UserProfileResponse getUserProfile(UUID userId) {
        return userProfileRepository.findById(userId)
                .map(UserProfileResponse::from)
                .orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Transactional
    public UserProfileResponse updateProfile(UUID userId, UpdateProfileRequest request) {
        log.trace("adminUpdateProfile: userId={}", userId);

        UserProfile profile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        profile.setFirstName(request.firstName());
        profile.setLastName(request.lastName());
        profile.setUsername(request.username());
        profile.setUpdatedAt(Instant.now());
        userProfileRepository.save(profile);

        outboxEventRepository.save(OutboxEvent.builder()
                .aggregateId(userId)
                .eventType(OutboxEventType.PROFILE_UPDATED)
                .payload(toJson(Map.of(
                        "firstName", request.firstName(),
                        "lastName", request.lastName(),
                        "username", request.username()
                )))
                .build());

        log.debug("Admin updated profile and outbox event created: userId={}", userId);
        return UserProfileResponse.from(profile);
    }

    // Reusing AvatarService for admin operations
    public String generateAvatarUploadUrl(UUID userId, String contentType) {
        return avatarService.generateUploadUrl(userId, contentType).uploadUrl();
    }

    public String confirmAvatarUpload(UUID userId, String objectKey) {
        return avatarService.confirmUpload(userId, objectKey).avatarUrl();
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
