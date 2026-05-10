package com.panol_project.backendpanol.modules.catalog.stock.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;
import java.util.UUID;

public record StockMovementV2Request(
        @JsonProperty("movement_type")
        String movementType,
        Integer quantity,
        @JsonProperty("individual_uuids")
        List<UUID> individualUuids,
        String condition
) {
}
