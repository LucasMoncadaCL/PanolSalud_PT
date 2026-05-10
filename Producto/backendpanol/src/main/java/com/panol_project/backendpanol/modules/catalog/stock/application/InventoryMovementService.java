package com.panol_project.backendpanol.modules.catalog.stock.application;

import com.panol_project.backendpanol.modules.catalog.stock.domain.InventoryMovement;
import com.panol_project.backendpanol.modules.catalog.stock.domain.MovementAction;
import com.panol_project.backendpanol.modules.catalog.stock.infrastructure.mongo.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository repository;

    public InventoryMovementService(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    public InventoryMovement registrarMovimiento(UUID implementUuid, MovementAction action, Integer quantity, UUID performedByUuid, String notes) {
        InventoryMovement movement = new InventoryMovement(
                implementUuid,
                action, 
                quantity, 
                performedByUuid,
                Instant.now(), 
                notes
        );
        return repository.save(movement);
    }

    public List<InventoryMovement> obtenerUltimosMovimientos(UUID implementUuid) {
        return repository.findTop10ByImplementUuidOrderByTimestampDesc(implementUuid);
    }

    public List<InventoryMovement> obtenerTodosMovimientos() {
        return repository.findAllByOrderByTimestampDesc();
    }
}
