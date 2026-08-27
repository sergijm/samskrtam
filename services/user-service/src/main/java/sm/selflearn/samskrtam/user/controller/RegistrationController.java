package sm.selflearn.samskrtam.user.controller;

// Импорты DTO из нового shared модуля


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
