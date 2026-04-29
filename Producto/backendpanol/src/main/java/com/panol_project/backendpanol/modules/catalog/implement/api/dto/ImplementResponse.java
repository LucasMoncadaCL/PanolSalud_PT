package com.panol_project.backendpanol.modules.catalog.implement.api.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.OffsetDateTime;

public record ImplementResponse(
        Integer id,
        String name,
        String description,
        @JsonProperty("item_type")
        String itemType,
        ImplementCategorySummaryResponse category,
        ImplementLocationSummaryResponse location,
        @JsonProperty("display_location")
        String displayLocation,
        Integer categoryId,
        Integer locationId,
        @JsonProperty("min_stock")
        Integer minStock,
        String observations,
        Boolean active,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
