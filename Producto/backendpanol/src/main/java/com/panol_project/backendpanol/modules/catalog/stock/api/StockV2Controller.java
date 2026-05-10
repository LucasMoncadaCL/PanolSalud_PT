package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.modules.catalog.stock.api.dto.*;
import com.panol_project.backendpanol.shared.error.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/api/v2/implements/{implementUuid}/stock")
public class StockV2Controller {

    private final StockController legacyController;
    private final DSLContext dsl;

    public StockV2Controller(StockController legacyController, DSLContext dsl) {
        this.legacyController = legacyController;
        this.dsl = dsl;
    }

    @GetMapping
    StockDetailV2Response getStock(@PathVariable UUID implementUuid) { return toV2Response(legacyController.getStock(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"))); }

    @PostMapping("/entries")
    StockDetailV2Response addEntry(@PathVariable UUID implementUuid, @Valid @RequestBody StockEntryRequest request) { return toV2Response(legacyController.addEntry(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), request)); }

    @PostMapping("/movements")
    StockDetailV2Response applyMovement(@PathVariable UUID implementUuid, @RequestBody StockMovementV2Request request) {
        List<Integer> individualIds = request.individualUuids() == null ? null : request.individualUuids().stream().map(uuid -> findIdByUuid("individual", uuid, "INDIVIDUAL_NOT_FOUND")).toList();
        return toV2Response(legacyController.applyMovement(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), new StockMovementRequest(request.movementType(), request.quantity(), individualIds, request.condition())));
    }

    @PutMapping("/individuals/{individualUuid}")
    StockDetailV2Response updateIndividual(@PathVariable UUID implementUuid, @PathVariable UUID individualUuid, @RequestBody IndividualUpdateV2Request request) {
        Integer locationId = request.currentLocationUuid() == null ? null : findIdByUuid("location", request.currentLocationUuid(), "LOCATION_NOT_FOUND");
        return toV2Response(legacyController.updateIndividual(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), findIdByUuid("individual", individualUuid, "INDIVIDUAL_NOT_FOUND"), new IndividualUpdateRequest(request.status(), request.condition(), locationId, request.active())));
    }

    private StockDetailV2Response toV2Response(StockDetailResponse response) {
        UUID implementUuid = findUuidById("implement", response.implementId(), "IMPLEMENT_NOT_FOUND");
        Map<Integer, UUID> individualUuids = findUuidsByIds("individual", response.individuals().stream().map(item -> item.id()).toList());
        Map<Integer, UUID> locationUuids = findUuidsByIds("location", response.individuals().stream().map(item -> item.currentLocationId()).filter(locationId -> locationId != null).toList());
        return new StockDetailV2Response(implementUuid, response.itemType(), response.stock(), response.individuals().stream().map(item -> new IndividualV2Response(individualUuids.get(item.id()), item.assetCode(), item.status(), item.condition(), item.currentLocationId() == null ? null : locationUuids.get(item.currentLocationId()), item.active())).toList());
    }

    private Integer findIdByUuid(String tableName, UUID uuid, String code) { Integer id = dsl.select(field(name("id"), Integer.class)).from(table(name(tableName))).where(field(name("uuid")).eq(uuid)).fetchOne(0, Integer.class); if (id == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado"); return id; }
    private UUID findUuidById(String tableName, Integer id, String code) { UUID uuid = dsl.select(field(name("uuid"), UUID.class)).from(table(name(tableName))).where(field(name("id")).eq(id)).fetchOne(0, UUID.class); if (uuid == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado"); return uuid; }
    private Map<Integer, UUID> findUuidsByIds(String tableName, List<Integer> ids) { if (ids == null || ids.isEmpty()) return Map.of(); var idField = field(name("id"), Integer.class); var uuidField = field(name("uuid"), UUID.class); return dsl.select(idField, uuidField).from(table(name(tableName))).where(idField.in(ids)).fetch().stream().collect(Collectors.toMap(r -> r.get(idField), r -> r.get(uuidField), (a, b) -> a)); }
}
