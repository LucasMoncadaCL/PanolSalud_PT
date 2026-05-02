package com.panol_project.backendpanol.modules.catalog.stock.infrastructure.mongo;

import com.panol_project.backendpanol.modules.catalog.stock.domain.InventoryMovement;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface InventoryMovementRepository extends MongoRepository<InventoryMovement, String> {
    List<InventoryMovement> findTop10ByImplementIdOrderByTimestampDesc(Integer implementId);
    List<InventoryMovement> findAllByOrderByTimestampDesc();
}
