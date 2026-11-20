package ru.delivery.courierdelivery.service;

import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import ru.delivery.courierdelivery.domain.Roles;
import ru.delivery.courierdelivery.dto.TokenPairResponse;
import ru.delivery.courierdelivery.model.User;
import ru.delivery.courierdelivery.repository.UserRepository;

@Service
public class AuthService {

    private static final Pattern STRONG = Pattern.compile(
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d).{8,}$"
    );

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenPairService tokenPairService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            TokenPairService tokenPairService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.tokenPairService = tokenPairService;
    }

    @Transactional
    public TokenPairResponse register(String name, String email, String rawPassword, String deviceId) {
        if (userRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email уже зарегистрирован");
        }
        if (rawPassword == null || rawPassword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Пароль обязателен");
        }
        if (!STRONG.matcher(rawPassword).matches()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Пароль: минимум 8 символов, хотя бы одна заглавная, строчная буква и цифра"
            );
        }
        User user = new User();
        user.setName(name.trim());
        user.setEmail(email.trim().toLowerCase());
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(Roles.SENDER);
        user.setStatus("ACTIVE");
        user = userRepository.saveAndFlush(user);
        return tokenPairService.issuePair(user, deviceId);
    }

    public User authenticate(String email, String password) {
        User user = userRepository.findByEmailIgnoreCase(email.trim().toLowerCase())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверные учётные данные"));
        if (!user.isActive()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Учётная запись недоступна");
        }
        String hash = user.getPasswordHash();
        if (hash == null || hash.isBlank() || !passwordEncoder.matches(password, hash)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверные учётные данные");
        }
        return user;
    }

    @Transactional
    public TokenPairResponse login(String email, String password, String deviceId) {
        User user = authenticate(email, password);
        return tokenPairService.issuePair(user, deviceId);
    }
}
