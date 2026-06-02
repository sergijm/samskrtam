package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.exception.UserPasswordUpdateException;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.outbox.KeycloakAdminService;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.ChangePasswordRequest;
import sm.selflearn.samskrtam.user.dto.ForgotPasswordRequest;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final ObjectMapper objectMapper;

    @Transactional
    public void forgotPassword(String email) {
        log.trace("forgotPassword: email={}", email);

        UserProfile userProfile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email " + email + " not found."));

        try {
            keycloakAdminService.sendPasswordResetEmail(userProfile.getId().toString());
            log.debug("Password reset email triggered for user: email={}", email);
        } catch (Exception e) {
            log.error("Failed to trigger password reset for user {}: {}", email, e.getMessage(), e);
            throw new UserPasswordUpdateException("Failed to trigger password reset for user " + email, e);
        }
    }

    @Transactional
    public void changePassword(UUID userId, ChangePasswordRequest request) {
        log.trace("changePassword: userId={}", userId);

        UserProfile userProfile = userProfileRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        try {
            keycloakAdminService.updateUserPassword(userId.toString(), request.newPassword());
            log.debug("Password changed for user: userId={}", userId);
        } catch (Exception e) {
            log.error("Failed to change password for user {}: {}", userId, e.getMessage(), e);
            throw new UserPasswordUpdateException("Failed to change password for user " + userId, e);
        }
    }
}
