package com.panol_project.backendpanol.modules.catalog.location.api;

import com.panol_project.backendpanol.modules.catalog.location.api.dto.LocationSelectorResponse;
import com.panol_project.backendpanol.modules.catalog.location.api.dto.CreateLocationRequest;
import com.panol_project.backendpanol.modules.catalog.location.api.dto.LocationResponse;
import com.panol_project.backendpanol.modules.catalog.location.api.dto.UpdateLocationRequest;
import com.panol_project.backendpanol.modules.catalog.location.application.LocationService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/locations")
public class LocationController {

    private final LocationService service;

    public LocationController(LocationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    public List<LocationSelectorResponse> listarSelector() {
        return service.listarSelector().stream()
                .map(location -> new LocationSelectorResponse(location.id(), location.name(), location.description(), location.active()))
                .toList();
    }

    @GetMapping("/gestion")
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<LocationResponse> listarGestion() {
        return service.listarGestion().stream()
                .map(location -> new LocationResponse(location.id(), location.name(), location.description(), location.active()))
                .toList();
    }

    @PostMapping
    @PreAuthorize("hasRole('COORDINADOR')")
    public LocationResponse crear(@Valid @RequestBody CreateLocationRequest request) {
        var created = service.crear(request.name(), request.description());
        return new LocationResponse(created.id(), created.name(), created.description(), created.active());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public LocationResponse editar(@PathVariable Integer id, @Valid @RequestBody UpdateLocationRequest request) {
        var updated = service.editar(id, request.name(), request.description());
        return new LocationResponse(updated.id(), updated.name(), updated.description(), updated.active());
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('COORDINADOR')")
    public LocationResponse setActive(@PathVariable Integer id, @RequestParam boolean active) {
        var updated = service.setActive(id, active);
        return new LocationResponse(updated.id(), updated.name(), updated.description(), updated.active());
    }
}

