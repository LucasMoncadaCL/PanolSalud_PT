package com.panol_project.backendpanol.modules.catalog.stock.application;

import com.panol_project.backendpanol.modules.catalog.stock.domain.InventoryMovement;
import com.panol_project.backendpanol.modules.catalog.stock.domain.MovementAction;
import com.panol_project.backendpanol.modules.catalog.stock.infrastructure.mongo.InventoryMovementRepository;
import org.springframework.stereotype.Service;
import java.time.Instant;
import java.util.List;

@Service
public class InventoryMovementService {

    private final InventoryMovementRepository repository;

    public InventoryMovementService(InventoryMovementRepository repository) {
        this.repository = repository;
    }

    public InventoryMovement registrarMovimiento(Integer implementId, MovementAction action, Integer quantity, Integer performedBy, String notes) {
        InventoryMovement movement = new InventoryMovement(
                implementId, 
                action, 
                quantity, 
                performedBy, 
                Instant.now(), 
                notes
        );
        return repository.save(movement);
    }

    public List<InventoryMovement> obtenerUltimosMovimientos(Integer implementId) {
        return repository.findTop10ByImplementIdOrderByTimestampDesc(implementId);
    }
}
