package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.modules.catalog.stock.api.dto.InventoryMovementResponse;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.InventoryMovementV2Response;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.RegisterMovementRequest;
import com.panol_project.backendpanol.shared.error.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/api/v2/implements")
public class InventoryMovementV2Controller {

    private final InventoryMovementController legacyController;
    private final DSLContext dsl;

    public InventoryMovementV2Controller(InventoryMovementController legacyController, DSLContext dsl) {
        this.legacyController = legacyController;
        this.dsl = dsl;
    }

    @GetMapping("/movements")
    public List<InventoryMovementV2Response> listarMovimientos() { return toV2List(legacyController.listarMovimientos()); }

    @PostMapping("/{implementUuid}/movements")
    @ResponseStatus(HttpStatus.CREATED)
    public InventoryMovementV2Response registrarMovimiento(@PathVariable UUID implementUuid, @Valid @RequestBody RegisterMovementRequest request, Authentication authentication) {
        return toV2Response(legacyController.registrarMovimiento(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), request, authentication));
    }

    private List<InventoryMovementV2Response> toV2List(List<InventoryMovementResponse> rows) { return rows.stream().map(this::toV2Response).toList(); }
    private InventoryMovementV2Response toV2Response(InventoryMovementResponse row) { return new InventoryMovementV2Response(row.id(), row.implementUuid(), row.action(), row.quantity(), row.performedBy(), row.timestamp(), row.notes()); }

    private Integer findIdByUuid(String tableName, UUID uuid, String code) { Integer id = dsl.select(field(name("id"), Integer.class)).from(table(name(tableName))).where(field(name("uuid")).eq(uuid)).fetchOne(0, Integer.class); if (id == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado"); return id; }
}
