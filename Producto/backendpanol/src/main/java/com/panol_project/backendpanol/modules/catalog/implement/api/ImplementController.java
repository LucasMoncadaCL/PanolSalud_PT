package com.panol_project.backendpanol.modules.catalog.implement.api;

import com.panol_project.backendpanol.modules.catalog.implement.api.dto.CreateImplementRequest;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementCategorySummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementLocationSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementStockSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.UpdateImplementRequest;
import com.panol_project.backendpanol.modules.catalog.implement.application.ImplementService;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/implements")
public class ImplementController {

    private final ImplementService service;

    public ImplementController(ImplementService service) {
        this.service = service;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    ImplementResponse crear(@Valid @RequestBody CreateImplementRequest request) {
        Implemento created = service.crear(
                request.name(),
                request.description(),
                request.categoryId(),
                request.locationId(),
                request.itemType(),
                request.minStock(),
                request.observations()
        );
        return toResponse(created, request.minStock(), request.observations());
    }

    @PutMapping("/{id}")
    ImplementResponse editar(@PathVariable Integer id, @Valid @RequestBody UpdateImplementRequest request) {
        Implemento updated = service.editar(id, request.name(), request.description(), request.categoryId(), request.locationId());
        Integer minStock = service.obtenerStockMinimo(updated.id());
        return toResponse(updated, minStock, null);
    }

    @GetMapping("/{id}")
    ImplementResponse obtener(@PathVariable Integer id) {
        Implemento implemento = service.obtener(id);
        Integer minStock = service.obtenerStockMinimo(implemento.id());
        return toResponse(implemento, minStock, null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR','DIRECTOR','DOCENTE')")
    List<ImplementSummaryResponse> listar(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer categoryId,
            Authentication authentication
    ) {
        boolean includeStockBreakdown = canViewStockBreakdown(authentication);

        return service.listar(name, categoryId).stream()
                .map(implemento -> new ImplementSummaryResponse(
                        implemento.id(),
                        implemento.name(),
                        implemento.description(),
                        implemento.active(),
                        implemento.stock() != null && implemento.stock().hasAvailability(),
                        implemento.category() == null
                                ? null
                                : new ImplementCategorySummaryResponse(
                                        implemento.category().id(),
                                        implemento.category().name(),
                                        implemento.category().active()
                                ),
                        new ImplementLocationSummaryResponse(
                                implemento.location().id(),
                                implemento.location().name(),
                                implemento.location().description()
                        ),
                        includeStockBreakdown && implemento.stock() != null
                                ? new ImplementStockSummaryResponse(
                                        implemento.stock().totalStock(),
                                        implemento.stock().minStock(),
                                        implemento.stock().available(),
                                        implemento.stock().reserved(),
                                        implemento.stock().loaned(),
                                        implemento.stock().damaged()
                                )
                                : null
                ))
                .toList();
    }

    private boolean canViewStockBreakdown(Authentication authentication) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return true;
        }

        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role -> "ROLE_COORDINADOR".equals(role) || "ROLE_DIRECTOR".equals(role));
    }

    private ImplementResponse toResponse(Implemento implemento) {
        return toResponse(implemento, null, null);
    }

    private ImplementResponse toResponse(Implemento implemento, Integer minStock, String observations) {
        return new ImplementResponse(
                implemento.id(),
                implemento.nombre(),
                implemento.descripcion(),
                implemento.itemType() == null ? null : implemento.itemType().literal(),
                implemento.categoriaId(),
                implemento.locationId(),
                minStock,
                observations,
                implemento.activo(),
                implemento.createdAt(),
                implemento.updatedAt()
        );
    }
}
