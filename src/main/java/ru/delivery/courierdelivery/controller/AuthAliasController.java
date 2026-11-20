package ru.delivery.courierdelivery.controller;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.delivery.courierdelivery.dto.LoginRequest;
import ru.delivery.courierdelivery.dto.RefreshRequest;
import ru.delivery.courierdelivery.dto.RegisterRequest;
import ru.delivery.courierdelivery.dto.TokenPairResponse;
import ru.delivery.courierdelivery.service.AuthService;
import ru.delivery.courierdelivery.service.TokenPairService;

@RestController
@RequestMapping("/auth")
public class AuthAliasController {

    private final AuthService authService;
    private final TokenPairService tokenPairService;

    public AuthAliasController(AuthService authService, TokenPairService tokenPairService) {
        this.authService = authService;
        this.tokenPairService = tokenPairService;
    }

    @PostMapping("/login")
    public ResponseEntity<TokenPairResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request.email(), request.password(), request.deviceId()));
    }

    @PostMapping("/register")
    public ResponseEntity<TokenPairResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(
                authService.register(request.name(), request.email(), request.password(), null)
        );
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenPairResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(tokenPairService.refresh(request.refreshToken()));
    }
}
