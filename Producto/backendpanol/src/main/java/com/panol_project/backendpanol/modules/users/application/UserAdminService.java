package com.panol_project.backendpanol.modules.users.application;

import com.panol_project.backendpanol.modules.auth.application.AuditLogService;
import com.panol_project.backendpanol.modules.users.api.dto.CreateUserRequest;
import com.panol_project.backendpanol.modules.users.api.dto.UpdateUserRequest;
import com.panol_project.backendpanol.modules.users.api.dto.UserAdminSummaryResponse;
import com.panol_project.backendpanol.shared.error.ApiException;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.bcrypt.BCrypt;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.stereotype.Service;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@Service
public class UserAdminService {

    private static final Set<String> ALLOWED_ROLES = Set.of("DIRECTOR", "COORDINADOR", "DOCENTE");

    private final DSLContext dsl;
    private final AuditLogService auditLogService;

    public UserAdminService(DSLContext dsl, AuditLogService auditLogService) {
        this.dsl = dsl;
        this.auditLogService = auditLogService;
    }

    public void createUser(CreateUserRequest request, Jwt jwt) {
        String role = normalizeRole(request.role());
        String normalizedRut = normalizeRut(request.rut());
        String normalizedEmail = normalizeEmail(request.email());
        UUID roleUuid = findRoleUuid(role);
        if (roleUuid == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_SUPPORTED", "Rol invalido");
        }

        var normalizedRutField = field(
                "replace(replace(replace({0}, '.', ''), '-', ''), ' ', '')",
                String.class,
                field(name("rut")));

        var duplicateCondition = normalizedRutField.eq(normalizedRut);
        if (normalizedEmail != null) {
            duplicateCondition = duplicateCondition.or(field(name("email")).eq(normalizedEmail));
        }

        Integer duplicated = dsl.selectCount().from(table(name("user")))
                .where(duplicateCondition)
                .fetchOne(0, Integer.class);

        if (duplicated != null && duplicated > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_DUPLICATED", "No fue posible procesar la solicitud");
        }

        dsl.insertInto(table(name("user")))
                .columns(field(name("name")), field(name("rut")), field(name("email")), field(name("password_hash")), field(name("role_uuid")), field(name("active")))
                .values(
                        request.name().trim(),
                        normalizedRut,
                        normalizedEmail,
                        BCrypt.hashpw(request.password(), BCrypt.gensalt()),
                        roleUuid,
                        true)
                .execute();

        auditLogService.log("user_created", getUserUuid(jwt), null, Map.of("rut", normalizedRut, "email", normalizedEmail == null ? "" : normalizedEmail, "role", role));
    }

    public void changeRole(UUID userUuid, String roleInput, Jwt jwt) {
        String role = normalizeRole(roleInput);
        UUID roleUuid = findRoleUuid(role);
        if (roleUuid == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_SUPPORTED", "Rol invalido");
        }
        int updated = dsl.update(table(name("user")))
                .set(field(name("role_uuid"), UUID.class), roleUuid)
                .where(field(name("uuid"), UUID.class).eq(userUuid))
                .execute();

        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado");
        }

        auditLogService.log("user_role_changed", getUserUuid(jwt), userUuid, Map.of("new_role", role));
    }

    public void changeRole(String userRef, String roleInput, Jwt jwt) {
        changeRole(resolveUserUuid(userRef), roleInput, jwt);
    }

    public List<UserAdminSummaryResponse> listUsers() {
        return dsl.select(
                        field(name("user", "uuid"), UUID.class),
                        field(name("user", "name"), String.class),
                        field(name("user", "rut"), String.class),
                        field(name("user", "email"), String.class),
                        field(name("role", "name"), String.class),
                        field(name("user", "active"), Boolean.class),
                        field(name("user", "created_at"), OffsetDateTime.class))
                .from(table(name("user")))
                .join(table(name("role"))).on(field(name("role", "uuid")).eq(field(name("user", "role_uuid"))))
                .orderBy(field(name("user", "created_at")).desc())
                .fetch(record -> new UserAdminSummaryResponse(
                        record.value1() == null ? null : record.value1().toString(),
                        record.value2(),
                        record.value3(),
                        record.value4(),
                        normalizeRoleForResponse(record.value5()),
                        Boolean.TRUE.equals(record.value6()),
                        record.value7()));
    }

    public void setActive(UUID userUuid, boolean active, Jwt jwt) {
        UUID actorUuid = getUserUuid(jwt);
        if (actorUuid != null && actorUuid.equals(userUuid) && !active) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_SELF_DEACTIVATION_NOT_ALLOWED", "No puedes desactivar tu propio usuario");
        }

        int updated = dsl.update(table(name("user")))
                .set(field(name("active")), active)
                .where(field(name("uuid"), UUID.class).eq(userUuid))
                .execute();

        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado");
        }

        auditLogService.log(
                active ? "user_activated" : "user_deactivated",
                actorUuid,
                userUuid,
                Map.of("active", active));
    }

    public void setActive(String userRef, boolean active, Jwt jwt) {
        setActive(resolveUserUuid(userRef), active, jwt);
    }

    public void updateUser(UUID userUuid, UpdateUserRequest request, Jwt jwt) {
        if (findUserIdOrNullByUuid(userUuid) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado");
        }
        UUID actorUuid = getUserUuid(jwt);
        String normalizedRut = normalizeRut(request.rut());
        String normalizedEmail = normalizeEmail(request.email());

        var normalizedRutField = field(
                "replace(replace(replace({0}, '.', ''), '-', ''), ' ', '')",
                String.class,
                field(name("rut")));

        var duplicateCondition = normalizedRutField.eq(normalizedRut);
        if (normalizedEmail != null) {
            duplicateCondition = duplicateCondition.or(field(name("email")).eq(normalizedEmail));
        }

        Integer duplicated = dsl.selectCount()
                .from(table(name("user")))
                .where(duplicateCondition)
                .and(field(name("uuid"), UUID.class).ne(userUuid))
                .fetchOne(0, Integer.class);

        if (duplicated != null && duplicated > 0) {
            throw new ApiException(HttpStatus.CONFLICT, "USER_DUPLICATED", "No fue posible procesar la solicitud");
        }

        int updated = dsl.update(table(name("user")))
                .set(field(name("name")), request.name().trim())
                .set(field(name("rut")), normalizedRut)
                .set(field(name("email")), normalizedEmail)
                .where(field(name("uuid"), UUID.class).eq(userUuid))
                .execute();

        if (updated == 0) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado");
        }

        auditLogService.log("user_updated", actorUuid, userUuid, Map.of("rut", normalizedRut, "email", normalizedEmail == null ? "" : normalizedEmail));
    }

    public void updateUser(String userRef, UpdateUserRequest request, Jwt jwt) {
        updateUser(resolveUserUuid(userRef), request, jwt);
    }

    public void deleteUser(UUID userUuid, Jwt jwt) {
        setActive(userUuid, false, jwt);
    }

    public void deleteUser(String userRef, Jwt jwt) {
        deleteUser(resolveUserUuid(userRef), jwt);
    }

    private UUID findRoleUuid(String normalizedRole) {
        return dsl.select(field(name("uuid"), UUID.class))
                .from(table(name("role")))
                .where(field(name("name")).likeIgnoreCase('%' + roleKey(normalizedRole) + '%'))
                .fetchOne(0, UUID.class);
    }

    private String normalizeRole(String roleRaw) {
        String role = roleRaw == null ? "" : roleRaw.trim().toUpperCase();
        if (role.contains("DIRECTOR")) role = "DIRECTOR";
        else if (role.contains("COORD")) role = "COORDINADOR";
        else if (role.contains("DOCENTE")) role = "DOCENTE";
        if (!ALLOWED_ROLES.contains(role)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "ROLE_NOT_SUPPORTED", "Rol invalido");
        }
        return role;
    }

    private String normalizeRoleForResponse(String roleRaw) {
        if (roleRaw == null || roleRaw.isBlank()) {
            return "DOCENTE";
        }
        String role = roleRaw.trim().toUpperCase();
        if (role.contains("DIRECTOR")) return "DIRECTOR";
        if (role.contains("COORD")) return "COORDINADOR";
        if (role.contains("DOCENTE")) return "DOCENTE";
        return role;
    }

    private String roleKey(String normalizedRole) {
        return switch (normalizedRole) {
            case "COORDINADOR" -> "COORD";
            default -> normalizedRole;
        };
    }

    private String normalizeRut(String rutRaw) {
        if (rutRaw == null) return "";
        return rutRaw.replaceAll("\\D", "").trim();
    }

    private String normalizeEmail(String emailRaw) {
        if (emailRaw == null) return null;
        String normalized = emailRaw.trim().toLowerCase();
        return normalized.isBlank() ? null : normalized;
    }

    private UUID getUserUuid(Jwt jwt) {
        if (jwt == null) return null;
        String subject = jwt.getSubject();
        if (subject != null && !subject.isBlank()) {
            UUID uuid = tryParseUuid(subject);
            if (uuid != null && findUserIdOrNullByUuid(uuid) != null) {
                return uuid;
            }
        }
        return null;
    }

    private UUID resolveUserUuid(String userRef) {
        if (userRef == null || userRef.isBlank()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_ID_INVALID", "Usuario no encontrado");
        }

        UUID uuid = tryParseUuid(userRef);
        if (uuid == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "USER_ID_INVALID", "Usuario no encontrado");
        }

        if (findUserIdOrNullByUuid(uuid) == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "Usuario no encontrado");
        }
        return uuid;
    }

    private Integer findUserIdOrNullByUuid(UUID uuid) {
        Integer userId = dsl.selectCount()
                .from(table(name("user")))
                .where(field(name("uuid")).eq(uuid))
                .fetchOne(0, Integer.class);
        return (userId != null && userId > 0) ? 1 : null;
    }

    private UUID tryParseUuid(String value) {
        try {
            return UUID.fromString(value);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }
}

