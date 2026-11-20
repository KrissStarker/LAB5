package ru.delivery.courierdelivery.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.delivery.courierdelivery.dto.LoginRequest;
import ru.delivery.courierdelivery.dto.RefreshRequest;
import ru.delivery.courierdelivery.dto.RegisterRequest;
import ru.delivery.courierdelivery.dto.TokenPairResponse;
import ru.delivery.courierdelivery.dto.UserResponse;
import ru.delivery.courierdelivery.security.AppUserDetails;
import ru.delivery.courierdelivery.service.AuthService;
import ru.delivery.courierdelivery.service.TokenPairService;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;
    private final TokenPairService tokenPairService;

    public AuthController(AuthService authService, TokenPairService tokenPairService) {
        this.authService = authService;
        this.tokenPairService = tokenPairService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        TokenPairResponse pair = authService.login(request.email(), request.password(), request.deviceId());
        return ResponseEntity.ok(pair);
    }

    @PostMapping("/register")
    public ResponseEntity<TokenPairResponse> register(@Valid @RequestBody RegisterRequest request) {
        TokenPairResponse pair = authService.register(
                request.name(),
                request.email(),
                request.password(),
                null
        );
        return ResponseEntity.status(HttpStatus.CREATED).body(pair);
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        TokenPairResponse pair = tokenPairService.refresh(request.refreshToken());
        return ResponseEntity.ok(pair);
    }

    @GetMapping("/csrf")
    public java.util.Map<String, String> csrf(CsrfToken token) {
        return java.util.Map.of(
                "token", token.getToken(),
                "headerName", token.getHeaderName(),
                "parameterName", token.getParameterName()
        );
    }

    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal AppUserDetails principal) {
        var user = principal.getUser();
        return new UserResponse(
                user.getId(),
                user.getName(),
                user.getEmail(),
                user.getRole(),
                user.getStatus(),
                user.getCreatedAt()
        );
    }
}
