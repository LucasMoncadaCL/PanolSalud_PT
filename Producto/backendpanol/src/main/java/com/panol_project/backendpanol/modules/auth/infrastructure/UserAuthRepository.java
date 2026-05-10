package com.panol_project.backendpanol.modules.auth.infrastructure;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;

public interface UserAuthRepository {
    Optional<AuthUserRow> findAuthUserByRut(String rut);
    void registerFailedAttempt(UUID userUuid, int attempts, OffsetDateTime blockedUntil);
    void resetLoginAttempts(UUID userUuid, OffsetDateTime lastLoginAt);
}

