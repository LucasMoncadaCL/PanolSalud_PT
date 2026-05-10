package com.panol_project.backendpanol.modules.catalog.implement.api;

import com.panol_project.backendpanol.modules.catalog.implement.api.dto.*;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.InventoryMovementV2Response;
import com.panol_project.backendpanol.shared.error.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/api/v2/implements")
public class ImplementV2Controller {

    private final ImplementController legacyController;
    private final DSLContext dsl;

    public ImplementV2Controller(ImplementController legacyController, DSLContext dsl) {
        this.legacyController = legacyController;
        this.dsl = dsl;
    }

    @PostMapping
    ImplementV2Response crear(@Valid @RequestBody CreateImplementV2Request request, Authentication authentication) {
        ImplementResponse response = legacyController.crear(new CreateImplementRequest(
                request.name(), request.description(),
                findIdByUuid("category", request.categoryUuid(), "CATEGORY_NOT_FOUND"),
                findIdByUuid("location", request.locationUuid(), "LOCATION_NOT_FOUND"),
                request.itemType(), request.minStock(), request.barcode(), request.imgUrl(), request.observations()), authentication);
        return toV2Response(response);
    }

    @PutMapping("/{implementUuid}")
    ImplementV2Response editar(@PathVariable UUID implementUuid, @Valid @RequestBody UpdateImplementV2Request request, Authentication authentication) {
        Integer implementId = findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND");
        ImplementResponse response = legacyController.editar(implementId, new UpdateImplementRequest(
                request.name(), request.description(),
                findIdByUuid("category", request.categoryUuid(), "CATEGORY_NOT_FOUND"),
                findIdByUuid("location", request.locationUuid(), "LOCATION_NOT_FOUND"),
                request.itemType(), request.minStock(), request.barcode(), request.imgUrl(), request.observations()), authentication);
        return toV2Response(response);
    }

    @GetMapping("/{implementUuid}")
    ImplementV2Response obtener(@PathVariable UUID implementUuid, Authentication authentication) {
        return toV2Response(legacyController.obtener(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), authentication));
    }

    @PatchMapping("/{implementUuid}/active")
    ImplementV2Response setActive(@PathVariable UUID implementUuid, @RequestParam boolean active, Authentication authentication) {
        return toV2Response(legacyController.setActive(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), active, authentication));
    }

    @GetMapping
    List<ImplementSummaryV2Response> listar(@RequestParam(required = false) String name, @RequestParam(required = false) UUID categoryUuid,
            @RequestParam(required = false) String stockStatus, Authentication authentication) {
        Integer categoryId = categoryUuid == null ? null : findIdByUuid("category", categoryUuid, "CATEGORY_NOT_FOUND");
        var legacyRows = legacyController.listar(name, categoryId, stockStatus, authentication);
        Map<Integer, UUID> implementUuids = findUuidsByIds("implement", legacyRows.stream().map(row -> row.id()).toList());
        Map<Integer, UUID> categoryUuids = findUuidsByIds("category", legacyRows.stream().map(row -> row.category() == null ? null : row.category().id()).filter(id -> id != null).toList());
        Map<Integer, UUID> locationUuids = findUuidsByIds("location", legacyRows.stream().map(row -> row.location() == null ? null : row.location().id()).filter(id -> id != null).toList());
        return legacyRows.stream().map(row -> new ImplementSummaryV2Response(
                implementUuids.get(row.id()), row.name(), row.description(), row.barcode(), row.imgUrl(), row.active(), row.available(),
                row.category() == null ? null : new ImplementCategorySummaryV2Response(categoryUuids.get(row.category().id()), row.category().name(), row.category().active()),
                row.location() == null ? null : new ImplementLocationSummaryV2Response(locationUuids.get(row.location().id()), row.location().name(), row.location().description()),
                row.stock())).toList();
    }

    private ImplementV2Response toV2Response(ImplementResponse response) {
        UUID implementUuid = findUuidById("implement", response.id(), "IMPLEMENT_NOT_FOUND");
        UUID categoryUuid = response.categoryId() == null ? null : findUuidById("category", response.categoryId(), "CATEGORY_NOT_FOUND");
        UUID locationUuid = response.locationId() == null ? null : findUuidById("location", response.locationId(), "LOCATION_NOT_FOUND");
        List<InventoryMovementV2Response> movementRows = response.recentMovements() == null ? null : response.recentMovements().stream().map(movement -> new InventoryMovementV2Response(
                movement.id(), movement.implementUuid(), movement.action(), movement.quantity(), movement.performedBy(), movement.timestamp(), movement.notes())).toList();
        return new ImplementV2Response(implementUuid, response.name(), response.description(), response.itemType(),
                response.category() == null ? null : new ImplementCategorySummaryV2Response(categoryUuid, response.category().name(), response.category().active()),
                response.location() == null ? null : new ImplementLocationSummaryV2Response(locationUuid, response.location().name(), response.location().description()),
                response.displayLocation(), categoryUuid, locationUuid, response.minStock(), response.barcode(), response.imgUrl(), response.observations(),
                response.active(), response.createdAt(), response.updatedAt(), response.stock(), movementRows);
    }

    private Integer findIdByUuid(String tableName, UUID uuid, String code) {
        Integer id = dsl.select(field(name("id"), Integer.class)).from(table(name(tableName))).where(field(name("uuid")).eq(uuid)).fetchOne(0, Integer.class);
        if (id == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado");
        return id;
    }
    private UUID findUuidById(String tableName, Integer id, String code) {
        UUID uuid = dsl.select(field(name("uuid"), UUID.class)).from(table(name(tableName))).where(field(name("id")).eq(id)).fetchOne(0, UUID.class);
        if (uuid == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado");
        return uuid;
    }
    private Map<Integer, UUID> findUuidsByIds(String tableName, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        var idField = field(name("id"), Integer.class);
        var uuidField = field(name("uuid"), UUID.class);
        return dsl.select(idField, uuidField).from(table(name(tableName))).where(idField.in(ids)).fetch().stream().collect(Collectors.toMap(r -> r.get(idField), r -> r.get(uuidField), (a, b) -> a));
    }
}
