package com.panol_project.backendpanol.modules.catalog.category.domain;

public record CategoriaAssociationSummaryResponse(
        Integer categoryId,
        int implementCount,
        boolean canDelete
) {
}
