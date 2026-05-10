package com.panol_project.backendpanol.modules.catalog.location.api;

import com.panol_project.backendpanol.modules.catalog.location.api.dto.CreateLocationRequest;
import com.panol_project.backendpanol.modules.catalog.location.api.dto.LocationSelectorV2Response;
import com.panol_project.backendpanol.modules.catalog.location.api.dto.UpdateLocationRequest;
import com.panol_project.backendpanol.modules.catalog.location.application.LocationService;
import com.panol_project.backendpanol.shared.error.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/api/v2/locations")
public class LocationV2Controller {

    private final LocationService locationService;
    private final DSLContext dsl;

    public LocationV2Controller(LocationService locationService, DSLContext dsl) {
        this.locationService = locationService;
        this.dsl = dsl;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LocationSelectorV2Response> listSelector() {
        var locations = locationService.listarSelector();
        Map<Integer, UUID> uuidById = findUuidsByIds("location", locations.stream().map(location -> location.id()).toList());
        return locations.stream().map(location -> new LocationSelectorV2Response(uuidById.get(location.id()), location.name(), location.description(), location.active())).toList();
    }

    @GetMapping("/management")
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<LocationSelectorV2Response> listManagement() {
        var locations = locationService.listarGestion();
        Map<Integer, UUID> uuidById = findUuidsByIds("location", locations.stream().map(location -> location.id()).toList());
        return locations.stream().map(location -> new LocationSelectorV2Response(uuidById.get(location.id()), location.name(), location.description(), location.active())).toList();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    public LocationSelectorV2Response create(@Valid @RequestBody CreateLocationRequest request) {
        var created = locationService.crear(request.name(), request.description());
        UUID uuid = findUuidsByIds("location", List.of(created.id())).get(created.id());
        return new LocationSelectorV2Response(uuid, created.name(), created.description(), created.active());
    }

    @PutMapping("/{locationUuid}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public LocationSelectorV2Response update(@PathVariable UUID locationUuid, @Valid @RequestBody UpdateLocationRequest request) {
        Integer locationId = findIdByUuid("location", locationUuid, "LOCATION_NOT_FOUND");
        var updated = locationService.editar(locationId, request.name(), request.description());
        return new LocationSelectorV2Response(locationUuid, updated.name(), updated.description(), updated.active());
    }

    @PatchMapping("/{locationUuid}/active")
    @PreAuthorize("hasRole('COORDINADOR')")
    public LocationSelectorV2Response setActive(@PathVariable UUID locationUuid, @RequestParam boolean active) {
        Integer locationId = findIdByUuid("location", locationUuid, "LOCATION_NOT_FOUND");
        var updated = locationService.setActive(locationId, active);
        return new LocationSelectorV2Response(locationUuid, updated.name(), updated.description(), updated.active());
    }

    private Integer findIdByUuid(String tableName, UUID uuid, String code) {
        Integer id = dsl.select(field(name("id"), Integer.class)).from(table(name(tableName))).where(field(name("uuid")).eq(uuid)).fetchOne(0, Integer.class);
        if (id == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado");
        return id;
    }

    private Map<Integer, UUID> findUuidsByIds(String tableName, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        var idField = field(name("id"), Integer.class);
        var uuidField = field(name("uuid"), UUID.class);
        return dsl.select(idField, uuidField).from(table(name(tableName))).where(idField.in(ids)).fetch().stream().collect(Collectors.toMap(r -> r.get(idField), r -> r.get(uuidField), (a, b) -> a));
    }
}
