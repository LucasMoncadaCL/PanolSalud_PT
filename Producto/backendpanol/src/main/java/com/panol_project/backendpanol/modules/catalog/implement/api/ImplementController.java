package com.panol_project.backendpanol.modules.catalog.implement.api;

import com.panol_project.backendpanol.modules.catalog.implement.api.dto.CreateImplementRequest;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementDetailStockResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementCategorySummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementLocationSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementStockSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.ImplementSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.implement.api.dto.UpdateImplementRequest;
import com.panol_project.backendpanol.modules.catalog.implement.application.ImplementService;
import com.panol_project.backendpanol.modules.catalog.stock.application.InventoryMovementService;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.InventoryMovementResponse;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import com.panol_project.backendpanol.modules.catalog.implement.domain.StockStatusFilter;
import com.panol_project.backendpanol.shared.error.BadRequestException;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/implements")
public class ImplementController {

    private final ImplementService service;
    private final InventoryMovementService inventoryMovementService;

    public ImplementController(
            ImplementService service, 
            InventoryMovementService inventoryMovementService
    ) {
        this.service = service;
        this.inventoryMovementService = inventoryMovementService;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    ImplementResponse crear(@Valid @RequestBody CreateImplementRequest request, Authentication authentication) {
        Implemento created = service.crear(
                request.name(),
                request.description(),
                request.categoryId(),
                request.locationId(),
                request.itemType(),
                request.minStock(),
                request.barcode(),
                request.imgUrl(),
                request.observations()
        );
      
        var summary = service.obtenerSummary(created.id());
        return toResponse(created, summary, service.obtenerStockMinimo(created.id()), created.observations(), authentication, null);


    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('COORDINADOR')")
    ImplementResponse editar(@PathVariable Integer id, @Valid @RequestBody UpdateImplementRequest request, Authentication authentication) {
        Implemento updated = service.editar(
                id,
                request.name(),
                request.description(),
                request.categoryId(),
                request.locationId(),
                request.itemType(),
                request.minStock(),
                request.barcode(),
                request.imgUrl(),
                request.observations()
        );
        var summary = service.obtenerSummary(updated.id());
        Integer minStock = service.obtenerStockMinimo(updated.id());
        return toResponse(updated, summary, minStock, updated.observations(), authentication, null);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('COORDINADOR','DIRECTOR','DOCENTE')")
    ImplementResponse obtener(@PathVariable Integer id, Authentication authentication) {
        Implemento implemento = service.obtener(id);
        var summary = service.obtenerSummary(id);
        Integer minStock = service.obtenerStockMinimo(implemento.id());
        
        List<InventoryMovementResponse> movements = List.of();
                
        return toResponse(implemento, summary, minStock, implemento.observations(), authentication, movements);
    }

    @PatchMapping("/{id}/active")
    @PreAuthorize("hasRole('COORDINADOR')")
    ImplementResponse setActive(@PathVariable Integer id, @RequestParam boolean active, Authentication authentication) {
        Implemento updated = service.setActive(id, active);
        var summary = service.obtenerSummary(updated.id());
        Integer minStock = service.obtenerStockMinimo(updated.id());
        return toResponse(updated, summary, minStock, updated.observations(), authentication, null);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR','DIRECTOR','DOCENTE')")
    List<ImplementSummaryResponse> listar(
            @RequestParam(required = false) String name,
            @RequestParam(required = false) Integer categoryId,
            @RequestParam(required = false) String stockStatus,
            Authentication authentication
    ) {
        boolean securityActive = authentication != null;
        boolean isCoordinador = !securityActive || hasRole(authentication, "ROLE_COORDINADOR");

        // El filtro por estado de stock es exclusivo del Coordinador (solo se aplica con seguridad activa)
        StockStatusFilter resolvedFilter = null;
        if (stockStatus != null) {
            if (!isCoordinador) {
                throw new org.springframework.security.access.AccessDeniedException(
                    "El filtro por estado de stock es exclusivo del rol Coordinador."
                );
            }
            resolvedFilter = StockStatusFilter.fromValue(stockStatus)
                    .orElseThrow(() -> new BadRequestException(
                            "INVALID_STOCK_STATUS",
                            "Valor inválido para stockStatus: '" + stockStatus + "'. Valores válidos: available, reserved, loaned, damaged, blocked."
                    ));
        }

        boolean includeStockBreakdown = canViewStockBreakdown(authentication);
        final StockStatusFilter finalFilter = resolvedFilter;

        return service.listar(name, categoryId, finalFilter).stream()
                .map(implemento -> new ImplementSummaryResponse(
                        implemento.id(),
                        implemento.name(),
                        implemento.description(),
                        implemento.barcode(),
                        implemento.imgUrl(),
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

    private boolean hasRole(Authentication authentication, String role) {
        if (authentication == null || authentication.getAuthorities() == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .anyMatch(role::equals);
    }

    private ImplementResponse toResponse(Implemento implemento) {
        // Fallback para casos donde no tengamos el summary (idealmente, siempre lo tenemos en GET/POST/PUT).
        return toResponse(implemento, null, null, null, null, null);
    }

    private ImplementResponse toResponse(
            Implemento implemento,
            com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementSummary summary,
            Integer minStock,
            String observations,
            Authentication authentication,
            List<InventoryMovementResponse> recentMovements
    ) {
        String displayLocation = summary == null ? null : service.resolveDisplayLocation(summary);
        String resolvedBarcode = summary != null ? summary.barcode() : implemento.barcode();
        String resolvedImgUrl = summary != null ? summary.imgUrl() : implemento.imgUrl();
        String resolvedObservations = summary != null ? observations : implemento.observations();
        
        ImplementDetailStockResponse stockResponse = null;
        if (summary != null && summary.stock() != null) {
            boolean includeStockBreakdown = canViewStockBreakdown(authentication);
            String availableDisplay = summary.stock().hasAvailability() ? "Disponible" : "No disponible";
            
            if (includeStockBreakdown) {
                stockResponse = new ImplementDetailStockResponse(
                        summary.stock().totalStock(),
                        summary.stock().available(),
                        summary.stock().reserved(),
                        summary.stock().loaned(),
                        summary.stock().damaged(),
                        null
                );
            } else {
                stockResponse = new ImplementDetailStockResponse(
                        null,
                        null,
                        null,
                        null,
                        null,
                        availableDisplay
                );
            }
        }

        return new ImplementResponse(
                implemento.id(),
                implemento.nombre(),
                implemento.descripcion(),
                implemento.itemType() == null ? null : implemento.itemType().literal(),
                summary == null || summary.category() == null
                        ? null
                        : new ImplementCategorySummaryResponse(
                                summary.category().id(),
                                summary.category().name(),
                                summary.category().active()
                        ),
                summary == null || summary.location() == null
                        ? null
                        : new ImplementLocationSummaryResponse(
                                summary.location().id(),
                                summary.location().name(),
                                summary.location().description()
                        ),
                displayLocation,
                implemento.categoriaId(),
                implemento.locationId(),
                minStock,
                resolvedBarcode,
                resolvedImgUrl,
                resolvedObservations,
                implemento.activo(),
                implemento.createdAt(),
                implemento.updatedAt(),
                stockResponse,
                recentMovements
        );
    }
}
