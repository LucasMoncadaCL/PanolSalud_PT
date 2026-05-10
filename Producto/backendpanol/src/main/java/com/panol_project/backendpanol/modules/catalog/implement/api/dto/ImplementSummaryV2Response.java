package com.panol_project.backendpanol.modules.catalog.implement.api.dto;

import java.util.UUID;

public record ImplementSummaryV2Response(
        UUID uuid,
        String name,
        String description,
        String barcode,
        String imgUrl,
        Boolean active,
        Boolean available,
        ImplementCategorySummaryV2Response category,
        ImplementLocationSummaryV2Response location,
        ImplementStockSummaryResponse stock
) {
}
