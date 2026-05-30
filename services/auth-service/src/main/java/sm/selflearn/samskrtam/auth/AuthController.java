package sm.selflearn.samskrtam.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import sm.selflearn.samskrtam.auth.dto.AuthResponse;
import sm.selflearn.samskrtam.auth.dto.CallbackRequest;
import sm.selflearn.samskrtam.auth.dto.LoginRequest;
import sm.selflearn.samskrtam.auth.dto.RefreshRequest;
import sm.selflearn.samskrtam.auth.service.TokenService;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final TokenService tokenService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody LoginRequest loginRequest) throws IOException {
        return ResponseEntity.ok(tokenService.login(loginRequest.getEmail(), loginRequest.getPassword()));
    }

    @PostMapping("/callback")
    public ResponseEntity<AuthResponse> callback(@RequestBody CallbackRequest callbackRequest) throws IOException {
        return ResponseEntity.ok(tokenService.exchangeCode(callbackRequest.getCode(), callbackRequest.getRedirectUri()));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody RefreshRequest refreshRequest) throws IOException {
        return ResponseEntity.ok(tokenService.refresh(refreshRequest.getRefreshToken()));
    }
}
