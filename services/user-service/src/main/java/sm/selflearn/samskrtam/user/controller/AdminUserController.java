package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import sm.selflearn.samskrtam.user.service.AdminUserService;
import sm.selflearn.samskrtam.user.service.UserBlockService;

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.AdminUserListResponse;
import sm.selflearn.samskrtam.user.dto.BlockUserResponse;
import sm.selflearn.samskrtam.user.dto.UpdateProfileRequest;
import sm.selflearn.samskrtam.user.dto.UserProfileResponse;
import sm.selflearn.samskrtam.user.model.UserRole; // UserRole остается в модели user-service

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/admin/users")
@Tag(name = "Admin User Management", description = "APIs for administrative user management")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;
    private final UserBlockService userBlockService;

    // GET /api/v1/admin/users
    @GetMapping
    @Operation(summary = "Get a paginated list of users with filtering and sorting (Admin only)")
    @ApiResponse(responseCode = "200", description = "List of users retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    public ResponseEntity<AdminUserListResponse> getAllUsers(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "createdAt") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDirection,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) UserRole role,
            @RequestParam(required = false) Boolean blocked
    ) {
        AdminUserListResponse response = adminUserService.getAllUsers(page, size, sortBy, sortDirection, search, role, blocked);
        return ResponseEntity.ok(response);
    }

    // GET /api/v1/admin/users/{id}
    @GetMapping("/{id}")
    @Operation(summary = "Get full user profile by ID (Admin only)")
    @ApiResponse(responseCode = "200", description = "User profile retrieved successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserProfileResponse> getUserProfile(@PathVariable UUID id) {
        UserProfileResponse response = adminUserService.getUserProfile(id);
        return ResponseEntity.ok(response);
    }

    // PUT /api/v1/admin/users/{id}
    @PutMapping("/{id}")
    @Operation(summary = "Update user profile by ID (Admin only)")
    @ApiResponse(responseCode = "200", description = "User profile updated successfully")
    @ApiResponse(responseCode = "400", description = "Invalid input")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @PathVariable UUID id,
            @Valid @RequestBody UpdateProfileRequest request
    ) {
        UserProfileResponse response = adminUserService.updateProfile(id, request);
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/admin/users/{id}/avatar/upload-url
    @PostMapping("/{id}/avatar/upload-url")
    @Operation(summary = "Generate a presigned URL for uploading user avatar (Admin only)")
    @ApiResponse(responseCode = "200", description = "Presigned URL generated")
    @ApiResponse(responseCode = "400", description = "Invalid file type")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    public ResponseEntity<String> generateAvatarUploadUrl(
            @PathVariable UUID id,
            @RequestHeader("Content-Type") String contentType
    ) {
        String uploadUrl = adminUserService.generateAvatarUploadUrl(id, contentType);
        return ResponseEntity.ok(uploadUrl);
    }

    // POST /api/v1/admin/users/{id}/avatar/confirm
    @PostMapping("/{id}/avatar/confirm")
    @Operation(summary = "Confirm avatar upload and update user's avatar URL (Admin only)")
    @ApiResponse(responseCode = "200", description = "Avatar upload confirmed")
    @ApiResponse(responseCode = "400", description = "Invalid object key or object not found")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<String> confirmAvatarUpload(
            @PathVariable UUID id,
            @Valid @RequestBody String objectKey
    ) {
        String avatarUrl = adminUserService.confirmAvatarUpload(id, objectKey);
        return ResponseEntity.ok(avatarUrl);
    }

    // POST /api/v1/admin/users/{id}/block
    @PostMapping("/{id}/block")
    @Operation(summary = "Block a user by ID (Admin only)")
    @ApiResponse(responseCode = "200", description = "User blocked successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "User already blocked")
    public ResponseEntity<BlockUserResponse> blockUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID id
    ) {
        BlockUserResponse response = userBlockService.blockUser(id, adminId);
        return ResponseEntity.ok(response);
    }

    // POST /api/v1/admin/users/{id}/unblock
    @PostMapping("/{id}/unblock")
    @Operation(summary = "Unblock a user by ID (Admin only)")
    @ApiResponse(responseCode = "200", description = "User unblocked successfully")
    @ApiResponse(responseCode = "403", description = "Forbidden (requires ADMIN role)")
    @ApiResponse(responseCode = "404", description = "User not found")
    @ApiResponse(responseCode = "409", description = "User not blocked")
    public ResponseEntity<BlockUserResponse> unblockUser(
            @RequestHeader("X-User-Id") UUID adminId,
            @PathVariable UUID id
    ) {
        BlockUserResponse response = userBlockService.unblockUser(id, adminId);
        return ResponseEntity.ok(response);
    }
}
