package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.user.dto.OAuthSyncRequest; // Import OAuthSyncRequest
import sm.selflearn.samskrtam.user.service.AvatarService;
import sm.selflearn.samskrtam.user.service.PasswordService;
import sm.selflearn.samskrtam.user.service.UserProfileService;
import sm.selflearn.samskrtam.user.service.RegistrationService; // Import RegistrationService

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.dto.UpdateProfileRequest;
import sm.selflearn.samskrtam.user.dto.PublicProfileResponse;
import sm.selflearn.samskrtam.user.dto.UploadUrlResponse;
import sm.selflearn.samskrtam.user.dto.AvatarConfirmRequest;
import sm.selflearn.samskrtam.user.dto.AvatarConfirmResponse;
import sm.selflearn.samskrtam.user.dto.ChangePasswordRequest;
import sm.selflearn.samskrtam.user.dto.UserGroupSummary;
import sm.selflearn.samskrtam.user.dto.UserSearchResponse; // Import UserSearchResponse
import sm.selflearn.samskrtam.user.dto.ForgotPasswordRequest; // Import ForgotPasswordRequest
import sm.selflearn.samskrtam.user.dto.ResetPasswordRequest; // Import ResetPasswordRequest
import sm.selflearn.samskrtam.user.dto.RegisterRequest; // Import RegisterRequest

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile Management", description = "APIs for managing user profiles and settings")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final AvatarService avatarService;
    private final PasswordService passwordService;
    private final RegistrationService registrationService; // Add RegistrationService

    // POST /api/v1/users/register
    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid registration data")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        // log.info("Attempting to register new user: {}", request.username()); // Logger is not available here, consider adding @Slf4j if needed
        registrationService.registerNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // GET /api/v1/users/me
    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile")
    @ApiResponse(responseCode = "200", description = "Current user profile found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserProfileResponse> getMe(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userProfileService.getProfileResponse(userId));
    }

    // PUT /api/v1/users/me
    @PutMapping("/me")
    @Operation(summary = "Update current authenticated user's profile (username, firstName, lastName)")
    @ApiResponse(responseCode = "200", description = "User profile updated")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserProfileResponse> updateMe(@RequestHeader("X-User-Id") UUID userId,
                                                        @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userProfileService.updateProfile(userId, request));
    }

    // GET /api/v1/users/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get public profile of a user by ID")
    @ApiResponse(responseCode = "200", description = "Public user profile found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<PublicProfileResponse> getPublicProfile(@PathVariable UUID id) {
        return ResponseEntity.ok(userProfileService.getPublicProfileResponse(id));
    }

    // GET /api/v1/users/{id}/groups
    @GetMapping("/{id}/groups")
    @Operation(summary = "Get groups a user is a member of")
    @ApiResponse(responseCode = "200", description = "List of user's groups retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<List<UserGroupSummary>> getUserGroups(@PathVariable UUID id) {
        return ResponseEntity.ok(userProfileService.getUserGroups(id));
    }

    // GET /api/v1/users/search
    @GetMapping("/search")
    @Operation(summary = "Search for users by username, first name, last name, or email")
    @ApiResponse(responseCode = "200", description = "List of matching users retrieved successfully")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<List<UserSearchResponse>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userProfileService.searchUsers(query));
    }

    // POST /api/v1/users/oauth2/sync
    @PostMapping("/oauth2/sync")
    @Operation(summary = "Synchronize user profile after OAuth2 login (internal API Gateway call)")
    @ApiResponse(responseCode = "200", description = "User profile synchronized successfully")
    @ApiResponse(responseCode = "400", description = "Invalid request or token")
    public ResponseEntity<UserProfileResponse> syncOAuth2Profile(@Valid @RequestBody OAuthSyncRequest request) {
        UserProfileResponse response = userProfileService.syncOAuth2Profile(request.getKeycloakAccessToken(), request.getProvider());
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/users/me/avatar/upload-url
    @PostMapping("/me/avatar/upload-url")
    @Operation(summary = "Generate a presigned URL for uploading user avatar")
    @ApiResponse(responseCode = "200", description = "Presigned URL generated")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<UploadUrlResponse> generateAvatarUploadUrl(
            @RequestHeader("X-User-Id") UUID userId,
            @RequestHeader("Content-Type") String contentType) {
        return ResponseEntity.ok(avatarService.generateUploadUrl(userId, contentType));
    }

    // POST /api/v1/users/me/avatar/confirm
    @PostMapping("/me/avatar/confirm")
    @Operation(summary = "Confirm avatar upload and update user's avatar URL")
    @ApiResponse(responseCode = "200", description = "Avatar upload confirmed")
    @ApiResponse(responseCode = "400", description = "Invalid object key or object not found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<AvatarConfirmResponse> confirmAvatarUpload(
            @RequestHeader("X-User-Id") UUID userId,
            @Valid @RequestBody AvatarConfirmRequest request) {
        return ResponseEntity.ok(avatarService.confirmUpload(userId, request.objectKey()));
    }

    // POST /api/v1/users/me/change-password
    @PostMapping("/me/change-password")
    @Operation(summary = "Change current authenticated user's password")
    @ApiResponse(responseCode = "204", description = "Password changed successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input or current password mismatch")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<Void> changePassword(@RequestHeader("X-User-Id") UUID userId,
                                               @Valid @RequestBody ChangePasswordRequest request) {
        passwordService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/users/forgot-password
    @PostMapping("/forgot-password")
    @Operation(summary = "Initiate password reset process by sending a reset email")
    @ApiResponse(responseCode = "204", description = "Password reset email sent (if user exists)")
    @ApiResponse(responseCode = "400", description = "Invalid email format")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        passwordService.forgotPassword(request.email());
        return ResponseEntity.noContent().build();
    }

    // POST /api/v1/users/reset-password
    @PostMapping("/reset-password")
    @Operation(summary = "Reset user password using a valid token")
    @ApiResponse(responseCode = "204", description = "Password reset successfully")
    @ApiResponse(responseCode = "400", description = "Invalid token or new password format")
    @ApiResponse(responseCode = "404", description = "User not found or token expired")
    public ResponseEntity<Void> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordService.resetPassword(request);
        return ResponseEntity.noContent().build();
    }
}
