package com.panol_project.backendpanol.modules.catalog.stock.infrastructure.mongo;

import com.panol_project.backendpanol.modules.catalog.stock.domain.InventoryMovement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;
import java.util.UUID;

public interface InventoryMovementRepository extends MongoRepository<InventoryMovement, String> {
    List<InventoryMovement> findTop10ByImplementUuidOrderByTimestampDesc(UUID implementUuid);
    List<InventoryMovement> findAllByOrderByTimestampDesc();
}
