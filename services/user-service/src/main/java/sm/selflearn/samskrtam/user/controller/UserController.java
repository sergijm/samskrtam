package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.user.dto.*;
import sm.selflearn.samskrtam.user.service.AvatarService;
import sm.selflearn.samskrtam.user.service.PasswordService;
import sm.selflearn.samskrtam.user.service.UserProfileService;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Profile Management", description = "APIs for managing user profiles and settings")
@RequiredArgsConstructor
public class UserController {

    private final UserProfileService userProfileService;
    private final AvatarService avatarService; // Will be created next
    private final PasswordService passwordService;

    // GET /api/v1/users/me
    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user's profile")
    @ApiResponse(responseCode = "200", description = "Current user profile found")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserProfileResponse> getMe(@RequestHeader("X-User-Id") UUID userId) {
        return ResponseEntity.ok(userProfileService.getUserProfile(userId));
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
        // Assuming userProfileService has a method to get public profile
        return ResponseEntity.ok(userProfileService.getUserProfile(id).toPublicProfileResponse());
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
            @Valid @RequestBody AvatarConfirmRequest request) { // AvatarConfirmRequest will be created
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

    // --- Existing methods that are not part of the current task's scope or are inconsistent with the spec ---
    // Commenting them out for now. They can be re-evaluated or moved later if needed.

    /*
    @PatchMapping("/me")
    @Operation(summary = "Update current authenticated user's profile settings")
    @ApiResponse(responseCode = "200", description = "User profile updated")
    @ApiResponse(responseCode = "401", description = "Unauthorized")
    public ResponseEntity<UserDto> updateMe(@RequestHeader("X-User-Id") UUID userId,
                                            @RequestBody UserUpdateDto updateDto) {
        return ResponseEntity.ok(userService.updateMe(userId, updateDto));
    }

    @PostMapping("/oauth2/sync")
    @Operation(summary = "Synchronize OAuth2 user profile")
    @ApiResponse(responseCode = "200", description = "User synchronized")
    public ResponseEntity<Void> syncOAuth2User(@RequestBody OAuthSyncRequest request) {
        userService.syncOAuth2User(request);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{userId}/groups")
    @Operation(summary = "Get groups for a specific user")
    @ApiResponse(responseCode = "200", description = "User groups found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<List<UserGroupSummary>> getUserGroups(@RequestHeader("X-User-Id") UUID currentUserId,
                                                                @PathVariable UUID userId) {
        // TODO: Add authorization logic if currentUserId is not userId and not ADMIN
        return ResponseEntity.ok(userService.getUserGroups(userId));
    }

    @GetMapping("/search")
    @Operation(summary = "Search users by username or email")
    @ApiResponse(responseCode = "200", description = "List of users matching the query")
    public ResponseEntity<List<UserDto>> searchUsers(@RequestParam String query) {
        return ResponseEntity.ok(userService.searchUsers(query));
    }
    */
}
