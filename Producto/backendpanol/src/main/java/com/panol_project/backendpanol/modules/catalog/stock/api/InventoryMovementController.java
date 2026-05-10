package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.modules.catalog.stock.api.dto.InventoryMovementResponse;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.RegisterMovementRequest;
import com.panol_project.backendpanol.modules.catalog.stock.application.InventoryMovementService;
import com.panol_project.backendpanol.modules.catalog.stock.domain.InventoryMovement;
import com.panol_project.backendpanol.modules.catalog.stock.domain.MovementAction;
import com.panol_project.backendpanol.modules.users.application.UserService;
import com.panol_project.backendpanol.shared.error.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/internal/legacy/api/implements")
public class InventoryMovementController {

    private final InventoryMovementService service;
    private final UserService userService;
    private final DSLContext dsl;

    public InventoryMovementController(InventoryMovementService service, UserService userService, DSLContext dsl) {
        this.service = service;
        this.userService = userService;
        this.dsl = dsl;
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('COORDINADOR','DIRECTOR','DOCENTE')")
    public List<InventoryMovementResponse> listarMovimientos() {
        List<InventoryMovement> movements = service.obtenerTodosMovimientos();
        List<UUID> userUuids = movements.stream()
                .map(InventoryMovement::getPerformedByUuid)
                .filter(uuid -> uuid != null)
                .distinct()
                .toList();

        Map<UUID, String> userNames = userService.getNombresUsuariosByUuid(userUuids);

        return movements.stream()
                .map(m -> new InventoryMovementResponse(
                        m.getId(),
                        m.getImplementUuid(),
                        m.getAction(),
                        m.getQuantity(),
                        userNames.getOrDefault(m.getPerformedByUuid(), "Usuario no identificado"),
                        m.getTimestamp(),
                        m.getNotes()
                ))
                .toList();
    }

    @PostMapping("/{id}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    public InventoryMovementResponse registrarMovimiento(
            @PathVariable Integer id,
            @Valid @RequestBody RegisterMovementRequest request,
            Authentication authentication
    ) {
        UUID performedBy = extractUserUuid(authentication);
        UUID implementUuid = extractImplementUuid(id);
        MovementAction domainAction = MovementAction.valueOf(request.action().name());

        InventoryMovement movement = service.registrarMovimiento(
                implementUuid,
                domainAction,
                request.quantity(),
                performedBy,
                request.notes()
        );
        return toResponse(movement);
    }

    private InventoryMovementResponse toResponse(InventoryMovement movement) {
        return new InventoryMovementResponse(
                movement.getId(),
                movement.getImplementUuid(),
                movement.getAction(),
                movement.getQuantity(),
                "Usuario no identificado",
                movement.getTimestamp(),
                movement.getNotes()
        );
    }

    private UUID extractUserUuid(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof Jwt jwt)) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_REQUIRED", "Autenticacion requerida");
        }
        String subject = jwt.getSubject();
        if (subject == null || subject.isBlank()) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_SUBJECT_MISSING", "Token invalido");
        }
        UUID uuid;
        try {
            uuid = UUID.fromString(subject);
        } catch (IllegalArgumentException ex) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_SUBJECT_INVALID", "Token invalido");
        }

        UUID userUuid = dsl.select(field(name("uuid"), UUID.class))
                .from(table(name("user")))
                .where(field(name("uuid")).eq(uuid))
                .fetchOne(0, UUID.class);

        if (userUuid == null) {
            throw new ApiException(HttpStatus.UNAUTHORIZED, "AUTH_USER_NOT_FOUND", "Usuario no encontrado");
        }
        return userUuid;
    }

    private UUID extractImplementUuid(Integer implementId) {
        UUID implementUuid = dsl.select(field(name("uuid"), UUID.class))
                .from(table(name("implement")))
                .where(field(name("id")).eq(implementId))
                .fetchOne(0, UUID.class);
        if (implementUuid == null) {
            throw new ApiException(HttpStatus.NOT_FOUND, "IMPLEMENT_NOT_FOUND", "Implemento no encontrado");
        }
        return implementUuid;
    }
}
