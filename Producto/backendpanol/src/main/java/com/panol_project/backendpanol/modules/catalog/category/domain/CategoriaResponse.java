package com.panol_project.backendpanol.modules.catalog.category.domain;

import java.time.OffsetDateTime;

public record CategoriaResponse(
        Integer id,
        String nombre,
        Boolean activa,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
}
