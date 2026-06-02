package sm.selflearn.samskrtam.user.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

// Импорты DTO из нового shared модуля
import sm.selflearn.samskrtam.user.dto.ForgotPasswordRequest;
import sm.selflearn.samskrtam.user.dto.RegisterRequest;

import sm.selflearn.samskrtam.user.service.PasswordService;
import sm.selflearn.samskrtam.user.service.RegistrationService;

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Registration & Password Recovery", description = "APIs for user registration and password recovery")
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RegistrationService registrationService;
    private final PasswordService passwordService;

    @PostMapping("/register")
    @Operation(summary = "Register a new user")
    @ApiResponse(responseCode = "201", description = "User registered successfully")
    @ApiResponse(responseCode = "400", description = "Invalid registration data")
    @ApiResponse(responseCode = "409", description = "Username or email already exists")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        log.info("Attempting to register new user: {}", request.username());
        registrationService.registerNewUser(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset")
    @ApiResponse(responseCode = "204", description = "Password reset process initiated")
    @ApiResponse(responseCode = "404", description = "User with email not found")
    public ResponseEntity<Void> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        log.info("Attempting to send password reset link for email: {}", request.getEmail());
        passwordService.forgotPassword(request.getEmail());
        return ResponseEntity.noContent().build();
    }
}
