package com.panol_project.backendpanol.modules.catalog.implement.domain;

public record ImplementSummary(
        Integer id,
        String name,
        String description,
        Boolean active,
        ImplementCategorySummary category,
        ImplementLocationSummary location,
        ImplementStockSummary stock
) {
}

