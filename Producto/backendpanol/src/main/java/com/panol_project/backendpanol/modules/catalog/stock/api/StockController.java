package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.modules.catalog.stock.api.dto.IndividualResponse;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.IndividualUpdateRequest;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.StockCountersResponse;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.StockDetailResponse;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.StockEntryRequest;
import com.panol_project.backendpanol.modules.catalog.stock.api.dto.StockMovementRequest;
import com.panol_project.backendpanol.modules.catalog.stock.application.StockService;
import com.panol_project.backendpanol.modules.catalog.stock.domain.StockDetail;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/implements/{implementId}/stock")
public class StockController {

    private final StockService service;

    public StockController(StockService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('COORDINADOR','DIRECTOR','DOCENTE')")
    StockDetailResponse getStock(@PathVariable Integer implementId) {
        return toResponse(service.getStockDetail(implementId));
    }

    @PostMapping("/entries")
    @PreAuthorize("hasRole('COORDINADOR')")
    StockDetailResponse addEntry(@PathVariable Integer implementId, @Valid @RequestBody StockEntryRequest request) {
        return toResponse(service.addEntry(implementId, request.quantity(), request.assetCodes()));
    }

    @PostMapping("/movements")
    @PreAuthorize("hasRole('COORDINADOR')")
    StockDetailResponse applyMovement(@PathVariable Integer implementId, @RequestBody StockMovementRequest request) {
        return toResponse(service.applyMovement(
                implementId,
                request.movementType(),
                request.quantity(),
                request.individualIds(),
                request.condition()
        ));
    }

    @PutMapping("/individuals/{individualId}")
    @PreAuthorize("hasRole('COORDINADOR')")
    StockDetailResponse updateIndividual(
            @PathVariable Integer implementId,
            @PathVariable Integer individualId,
            @RequestBody IndividualUpdateRequest request
    ) {
        return toResponse(service.updateIndividual(
                implementId,
                individualId,
                request.status(),
                request.condition(),
                request.currentLocationId(),
                request.active()
        ));
    }

    private StockDetailResponse toResponse(StockDetail detail) {
        return new StockDetailResponse(
                detail.implementId(),
                detail.itemType() == null ? null : detail.itemType().literal(),
                new StockCountersResponse(
                        detail.stock().totalStock(),
                        detail.stock().minStock(),
                        detail.stock().available(),
                        detail.stock().reserved(),
                        detail.stock().loaned(),
                        detail.stock().damaged()
                ),
                detail.individuals() == null
                        ? List.of()
                        : detail.individuals().stream()
                                .map(item -> new IndividualResponse(
                                        item.id(),
                                        item.assetCode(),
                                        item.status(),
                                        item.condition(),
                                        item.currentLocationId(),
                                        item.active()
                                ))
                                .toList()
        );
    }
}
