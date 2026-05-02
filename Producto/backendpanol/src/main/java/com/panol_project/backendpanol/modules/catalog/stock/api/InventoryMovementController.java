package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.modules.catalog.stock.api.dto.RegisterMovementRequest;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.InventoryMovementResponse;
import com.panol_project.backendpanol.modules.catalog.stock.application.InventoryMovementService;
import com.panol_project.backendpanol.modules.catalog.stock.domain.InventoryMovement;
import com.panol_project.backendpanol.modules.catalog.stock.domain.MovementAction;
import com.panol_project.backendpanol.modules.users.application.UserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/implements")
public class InventoryMovementController {

    private final InventoryMovementService service;
    private final UserService userService;

    public InventoryMovementController(InventoryMovementService service, UserService userService) {
        this.service = service;
        this.userService = userService;
    }

    @GetMapping("/movements")
    @PreAuthorize("hasAnyRole('COORDINADOR','DIRECTOR','DOCENTE')")
    public List<InventoryMovementResponse> listarMovimientos() {
        List<InventoryMovement> movements = service.obtenerTodosMovimientos();
        List<Integer> userIds = movements.stream()
                .map(InventoryMovement::getPerformedBy)
                .filter(id -> id != null)
                .distinct()
                .toList();

        Map<Integer, String> userNames = userService.getNombresUsuarios(userIds);

        return movements.stream()
                .map(m -> new InventoryMovementResponse(
                        m.getId(),
                        m.getImplementId(),
                        m.getAction(),
                        m.getQuantity(),
                        userNames.getOrDefault(m.getPerformedBy(), "Usuario " + m.getPerformedBy()),
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
        Integer performedBy = extractUserId(authentication);
        MovementAction domainAction = MovementAction.valueOf(request.action().name());
        
        InventoryMovement movement = service.registrarMovimiento(
                id, 
                domainAction, 
                request.quantity(), 
                performedBy, 
                request.notes()
        );
        return toResponse(movement);
    }

    private InventoryMovementResponse toResponse(InventoryMovement m) {
        // Al registrar, de momento devolvemos null o string genérico para performed_by en la respuesta DTO 
        // porque el DTO response exige String (nombre). El Endpoint GET es el que cruza los datos.
        return new InventoryMovementResponse(
                m.getId(),
                m.getImplementId(),
                m.getAction(),
                m.getQuantity(),
                "Usuario " + m.getPerformedBy(),
                m.getTimestamp(),
                m.getNotes()
        );
    }

    private Integer extractUserId(Authentication authentication) {
        // En Sprint 1, como no está la HU-01 de Autenticación conectada a Supabase,
        // hardcodeamos el user_id del Coordinador (usualmente ID 1 en PostgreSQL).
        // En Sprint 2, aquí se extraerá el UUID del JWT y se buscará el Integer asociado.
        return 1;
    }
}
