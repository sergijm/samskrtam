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
import sm.selflearn.samskrtam.user.dto.RegisterRequest;

import sm.selflearn.samskrtam.user.service.RegistrationService;

// This controller will be removed as its functionality is moved to UserController
// @RestController
// @RequestMapping("/api/v1/users")
// @Tag(name = "Registration & Password Recovery", description = "APIs for user registration and password recovery")
// @RequiredArgsConstructor
// @Slf4j
// public class RegistrationController {

//     private final RegistrationService registrationService;

//     @PostMapping("/register")
//     @Operation(summary = "Register a new user")
//     @ApiResponse(responseCode = "201", description = "User registered successfully")
//     @ApiResponse(responseCode = "400", description = "Invalid registration data")
//     @ApiResponse(responseCode = "409", description = "Username or email already exists")
//     public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
//         log.info("Attempting to register new user: {}", request.username());
//         registrationService.registerNewUser(request);
//         return ResponseEntity.status(HttpStatus.CREATED).build();
//     }
// }
