package com.panol_project.backendpanol.modules.auth.application;

import com.panol_project.backendpanol.modules.auth.api.dto.LoginRequest;
import com.panol_project.backendpanol.modules.auth.api.dto.LoginResponse;
import com.panol_project.backendpanol.modules.auth.infrastructure.AuthUserRow;
import com.panol_project.backendpanol.modules.auth.infrastructure.TokenRevocationRepository;
import com.panol_project.backendpanol.modules.auth.infrastructure.UserAuthRepository;
import com.panol_project.backendpanol.shared.error.ApiException;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Map;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserAuthRepository userAuthRepository;
    private final TokenRevocationRepository tokenRevocationRepository;
    private final JwtEncoder jwtEncoder;
    private final AuditLogService auditLogService;
    private final int maxFailedAttempts;
    private final int lockMinutes;
    private final int tokenExpirationSeconds;
    private final String jwtIssuer;

    public AuthService(
            UserAuthRepository userAuthRepository,
            TokenRevocationRepository tokenRevocationRepository,
            JwtEncoder jwtEncoder,
            AuditLogService auditLogService,
            @Value("${app.auth.max-failed-attempts:5}") int maxFailedAttempts,
            @Value("${app.auth.lock-minutes:15}") int lockMinutes,
            @Value("${app.auth.jwt.expiration-seconds:3600}") int tokenExpirationSeconds,
            @Value("${app.auth.jwt.issuer:panol-backend}") String jwtIssuer
    ) {
        this.userAuthRepository = userAuthRepository;
        this.tokenRevocationRepository = tokenRevocationRepository;
        this.jwtEncoder = jwtEncoder;
        this.auditLogService = auditLogService;
        this.maxFailedAttempts = maxFailedAttempts;
        this.lockMinutes = lockMinutes;
        this.tokenExpirationSeconds = tokenExpirationSeconds;
        this.jwtIssuer = jwtIssuer;
    }

    public LoginResponse login(LoginRequest request) {
        String rut = normalizeRut(request.rut());
        AuthUserRow user = userAuthRepository.findAuthUserByRut(rut)
                .orElseThrow(() -> invalidCredentials(rut));

        if (user.blockedUntil() != null && user.blockedUntil().isAfter(OffsetDateTime.now())) {
            throw new ApiException(HttpStatus.TOO_MANY_REQUESTS, "AUTH_TEMPORARILY_BLOCKED", "Credenciales incorrectas");
        }

        boolean validPassword = BCrypt.checkpw(request.password(), user.passwordHash());
        if (!validPassword) {
            int next = user.failedLoginAttempts() + 1;
            OffsetDateTime blockedUntil = next >= maxFailedAttempts
                    ? OffsetDateTime.now().plusMinutes(lockMinutes)
                    : null;
            userAuthRepository.registerFailedAttempt(user.uuid(), next, blockedUntil);
            throw invalidCredentials(rut);
        }

        userAuthRepository.resetLoginAttempts(user.uuid(), OffsetDateTime.now());
        String normalizedRole = normalizeRole(user.roleName());

        Instant now = Instant.now();
        Instant exp = now.plusSeconds(tokenExpirationSeconds);
        String jti = UUID.randomUUID().toString();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(jwtIssuer)
                .issuedAt(now)
                .expiresAt(exp)
                .subject(user.uuid().toString())
                .claim("role", normalizedRole)
                .claim("jti", jti)
                .build();

        String token = jwtEncoder.encode(
                JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims))
                .getTokenValue();

        auditLogService.log("user_logged_in", user.uuid(), user.uuid(), Map.of("rut", rut, "role", normalizedRole));
        return new LoginResponse(token, normalizedRole, tokenExpirationSeconds);
    }

    public void logout(Jwt jwt) {
        if (jwt == null) {
            return;
        }
        String jti = jwt.getId();
        UUID userUuid = null;
        try {
            String subject = jwt.getSubject();
            if (subject != null && !subject.isBlank()) {
                userUuid = UUID.fromString(subject);
            }
        } catch (IllegalArgumentException ignored) {
            // transitional token with legacy subject
        }
        OffsetDateTime expiresAt = OffsetDateTime.ofInstant(jwt.getExpiresAt(), ZoneOffset.UTC);
        tokenRevocationRepository.revokeToken(jti, userUuid, expiresAt);
        auditLogService.log("user_logged_out", null, null, Map.of("jti", jti));
    }

    private ApiException invalidCredentials(String rut) {
        auditLogService.log("login_failed", null, null, Map.of("rut", rut));
        return new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "Credenciales incorrectas");
    }

    private String normalizeRole(String rawRole) {
        if (rawRole == null) return "DOCENTE";
        String role = rawRole.trim().toUpperCase();
        if (role.contains("DIRECTOR")) return "DIRECTOR";
        if (role.contains("COORD")) return "COORDINADOR";
        if (role.contains("DOCENTE")) return "DOCENTE";
        return "DOCENTE";
    }

    private String normalizeRut(String rutRaw) {
        if (rutRaw == null) return "";
        return rutRaw.replaceAll("\\D", "").trim();
    }
}

