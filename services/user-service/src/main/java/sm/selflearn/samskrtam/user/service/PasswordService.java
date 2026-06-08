package sm.selflearn.samskrtam.user.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import sm.selflearn.samskrtam.user.exception.InvalidTokenException;
import sm.selflearn.samskrtam.user.exception.UserNotFoundException;
import sm.selflearn.samskrtam.user.exception.UserPasswordUpdateException;
import sm.selflearn.samskrtam.user.model.UserProfile;
import sm.selflearn.samskrtam.user.outbox.KeycloakAdminService;
import sm.selflearn.samskrtam.user.repository.UserProfileRepository;

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.ChangePasswordRequest;
import sm.selflearn.samskrtam.user.dto.ForgotPasswordRequest;
import sm.selflearn.samskrtam.user.dto.ResetPasswordRequest; // Import new DTO

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordService {

    private final UserProfileRepository userProfileRepository;
    private final KeycloakAdminService keycloakAdminService;
    private final EmailService emailService; // Inject EmailService
    private final ObjectMapper objectMapper;

    @Transactional
    public void forgotPassword(String email) {
        log.trace("forgotPassword: email={}", email);

        UserProfile userProfile = userProfileRepository.findByEmail(email)
                .orElseThrow(() -> new UserNotFoundException("User with email " + email + " not found."));

        // Generate a password reset token and set expiry
        String resetToken = UUID.randomUUID().toString();
        Instant expiryDate = Instant.now().plus(1, ChronoUnit.HOURS); // Token valid for 1 hour

        userProfile.setPasswordResetToken(resetToken);
        userProfile.setPasswordResetTokenExpiry(expiryDate);
        userProfileRepository.save(userProfile); // Save the updated user profile

        try {
            // Send password reset email using the new EmailService
            emailService.sendPasswordResetEmail(userProfile.getEmail(), resetToken);
            log.debug("Password reset email with custom link triggered for user: email={}", email);
        } catch (Exception e) {
            log.error("Failed to send custom password reset email for user {}: {}", email, e.getMessage(), e);
            throw new UserPasswordUpdateException("Failed to send custom password reset email for user " + email, e);
        }
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        log.trace("resetPassword: token={}", request.token());

        UserProfile userProfile = userProfileRepository.findByPasswordResetToken(request.token())
                .orElseThrow(() -> new InvalidTokenException("Invalid or expired password reset token."));

        if (userProfile.getPasswordResetTokenExpiry() == null || userProfile.getPasswordResetTokenExpiry().isBefore(Instant.now())) {
            userProfile.setPasswordResetToken(null); // Clear expired token
            userProfile.setPasswordResetTokenExpiry(null);
            userProfileRepository.save(userProfile);
            throw new InvalidTokenException("Invalid or expired password reset token.");
        }

        try {
            keycloakAdminService.updateUserPassword(userProfile.getId().toString(), request.newPassword());

            // Clear the token after successful password reset
            userProfile.setPasswordResetToken(null);
            userProfile.setPasswordResetTokenExpiry(null);
            userProfileRepository.save(userProfile);

            log.debug("Password successfully reset for user: userId={}", userProfile.getId());
        } catch (Exception e) {
            log.error("Failed to reset password for user {}: {}", userProfile.getId(), e.getMessage(), e);
            throw new UserPasswordUpdateException("Failed to reset password for user " + userProfile.getId(), e);
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
