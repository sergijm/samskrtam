package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.user.service.UserService;
import sm.selflearn.samskrtam.user.dto.UserDto;
import sm.selflearn.samskrtam.user.dto.OAuthSyncRequest;
// Removed import for OAuthSyncResponse

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "User Management", description = "APIs for user management")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user by ID")
    @ApiResponse(responseCode = "200", description = "User found")
    @ApiResponse(responseCode = "404", description = "User not found")
    public ResponseEntity<UserDto> getUserById(@PathVariable UUID userId) {
        return userService.findById(userId)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/oauth2/sync")
    @Operation(summary = "Synchronize OAuth2 user profile")
    @ApiResponse(responseCode = "200", description = "User synchronized")
    public ResponseEntity<Void> syncOAuth2User(@RequestBody OAuthSyncRequest request) { // Changed return type to Void
        userService.syncOAuth2User(request);
        return ResponseEntity.ok().build(); // Return 200 OK with no body
    }
}
