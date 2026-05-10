package com.panol_project.backendpanol.modules.auth.infrastructure;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.stereotype.Repository;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Repository
public class AuthJooqRepository implements UserAuthRepository, TokenRevocationRepository {

    private final DSLContext dsl;

    public AuthJooqRepository(DSLContext dsl) {
        this.dsl = dsl;
    }

    @Override
    public Optional<AuthUserRow> findAuthUserByRut(String rut) {
        var userTable = table(name("user"));
        var roleTable = table(name("role"));
        var normalizedRutField = field(
                "replace(replace(replace({0}, '.', ''), '-', ''), ' ', '')",
                String.class,
                field(name("user", "rut")));
        return dsl.select(
                        field(name("user", "uuid"), UUID.class),
                        field(name("user", "rut"), String.class),
                        field(name("user", "password_hash"), String.class),
                        field(name("role", "name"), String.class),
                        field(name("user", "failed_login_attempts"), Integer.class),
                        field(name("user", "blocked_until"), OffsetDateTime.class))
                .from(userTable)
                .join(roleTable).on(field(name("role", "uuid")).eq(field(name("user", "role_uuid"))))
                .where(normalizedRutField.eq(rut))
                .and(field(name("user", "active")).eq(true))
                .fetchOptional(record -> new AuthUserRow(
                        record.value1(),
                        record.value2(),
                        record.value3(),
                        record.value4(),
                        record.value5() == null ? 0 : record.value5(),
                        record.value6()
                ));
    }

    @Override
    public void registerFailedAttempt(UUID userUuid, int attempts, OffsetDateTime blockedUntil) {
        dsl.update(table(name("user")))
                .set(field(name("failed_login_attempts")), attempts)
                .set(field(name("blocked_until")), blockedUntil)
                .where(field(name("uuid")).eq(userUuid))
                .execute();
    }

    @Override
    public void resetLoginAttempts(UUID userUuid, OffsetDateTime lastLoginAt) {
        dsl.update(table(name("user")))
                .set(field(name("failed_login_attempts")), 0)
                .set(field(name("blocked_until")), (OffsetDateTime) null)
                .set(field(name("last_login_at")), lastLoginAt)
                .where(field(name("uuid")).eq(userUuid))
                .execute();
    }

    @Override
    public void revokeToken(String jti, UUID userUuid, OffsetDateTime expiresAt) {
        dsl.insertInto(table(name("token_revocation")))
                .columns(
                        field(name("jti")),
                        field(name("user_uuid")),
                        field(name("expires_at")))
                .values(jti, userUuid, expiresAt)
                .onConflict(field(name("jti")))
                .doNothing()
                .execute();
    }

    @Override
    public boolean isRevoked(String jti) {
        if (jti == null || jti.isBlank()) {
            return false;
        }
        Integer count = dsl.selectCount()
                .from(table(name("token_revocation")))
                .where(field(name("jti")).eq(jti))
                .fetchOne(0, Integer.class);
        return count != null && count > 0;
    }
}

