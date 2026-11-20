package ru.delivery.courierdelivery.service;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;
import ru.delivery.courierdelivery.domain.SessionStatus;
import ru.delivery.courierdelivery.dto.TokenPairResponse;
import ru.delivery.courierdelivery.model.User;
import ru.delivery.courierdelivery.model.UserSession;
import ru.delivery.courierdelivery.repository.UserRepository;
import ru.delivery.courierdelivery.repository.UserSessionRepository;
import ru.delivery.courierdelivery.security.jwt.JwtService;

@Service
public class TokenPairService {

    private final JwtService jwtService;
    private final UserSessionRepository sessionRepository;
    private final UserRepository userRepository;
    private final TransactionTemplate transactionTemplate;

    public TokenPairService(
            JwtService jwtService,
            UserSessionRepository sessionRepository,
            UserRepository userRepository,
            PlatformTransactionManager transactionManager
    ) {
        this.jwtService = jwtService;
        this.sessionRepository = sessionRepository;
        this.userRepository = userRepository;
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Transactional
    public TokenPairResponse issuePair(User user, String deviceId) {
        JwtService.IssuedToken access = jwtService.createAccessToken(user.getEmail(), user.getRole());
        JwtService.IssuedToken refresh = jwtService.createRefreshToken(user.getEmail(), user.getRole());

        UserSession session = new UserSession();
        session.setUsername(user.getEmail());
        session.setDeviceId(deviceId);
        session.setAccessToken(access.token());
        session.setRefreshToken(refresh.token());
        session.setAccessTokenExpiry(access.expiresAt());
        session.setRefreshTokenExpiry(refresh.expiresAt());
        session.setStatus(SessionStatus.ACTIVE);
        sessionRepository.save(session);

        return new TokenPairResponse(access.token(), refresh.token());
    }

    /**
     * Транзакция коммитится всегда (в т.ч. при USED→REVOKED).
     * 401 кидается уже после commit — иначе rollback съест REVOKED.
     * Под PESSIMISTIC_WRITE два параллельных refresh: один ротирует, второй ставит REVOKED.
     */
    public TokenPairResponse refresh(String refreshTokenValue) {
        Claims claims;
        try {
            claims = jwtService.parseAndValidate(refreshTokenValue, JwtService.TYPE_REFRESH);
        } catch (JwtException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Неверный или просроченный refresh");
        }

        Object outcome = transactionTemplate.execute(status -> doRefreshInTx(refreshTokenValue, claims));

        if (outcome instanceof String message) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, message);
        }
        return (TokenPairResponse) outcome;
    }

    private Object doRefreshInTx(String refreshTokenValue, Claims claims) {
        UserSession session = sessionRepository.findByRefreshTokenForUpdate(refreshTokenValue)
                .orElse(null);
        if (session == null) {
            return "Сессия не найдена";
        }

        if (session.getStatus() == SessionStatus.REVOKED) {
            return "Сессия отозвана";
        }

        if (session.getStatus() == SessionStatus.USED) {
            session.setStatus(SessionStatus.REVOKED);
            sessionRepository.saveAndFlush(session);
            return "Refresh уже использован";
        }

        // ACTIVE — ещё раз expiry под lock (после ожидания блокировки токен мог истечь)
        if (!session.getRefreshTokenExpiry().isAfter(Instant.now())) {
            session.setStatus(SessionStatus.REVOKED);
            sessionRepository.saveAndFlush(session);
            return "Refresh истёк";
        }

        String username = claims.getSubject();
        if (!username.equalsIgnoreCase(session.getUsername())) {
            return "Несоответствие владельца";
        }

        User user = userRepository.findByEmailIgnoreCase(username).orElse(null);
        if (user == null) {
            return "Пользователь не найден";
        }
        if (!user.isActive()) {
            return "Учётная запись недоступна";
        }

        session.setStatus(SessionStatus.USED);
        sessionRepository.save(session);

        JwtService.IssuedToken access = jwtService.createAccessToken(user.getEmail(), user.getRole());
        JwtService.IssuedToken refresh = jwtService.createRefreshToken(user.getEmail(), user.getRole());

        UserSession next = new UserSession();
        next.setUsername(user.getEmail());
        next.setDeviceId(session.getDeviceId());
        next.setAccessToken(access.token());
        next.setRefreshToken(refresh.token());
        next.setAccessTokenExpiry(access.expiresAt());
        next.setRefreshTokenExpiry(refresh.expiresAt());
        next.setStatus(SessionStatus.ACTIVE);
        sessionRepository.save(next);

        return new TokenPairResponse(access.token(), refresh.token());
    }
}
