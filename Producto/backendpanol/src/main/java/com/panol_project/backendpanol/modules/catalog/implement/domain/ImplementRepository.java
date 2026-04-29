package com.panol_project.backendpanol.modules.catalog.implement.domain;

import java.util.Optional;
import java.util.List;

public interface ImplementRepository {

    Optional<Implemento> findById(Integer id);

    Optional<ImplementSummary> findSummaryById(Integer id);

    List<ImplementSummary> findAllSummaries(String name, Integer categoryId);

    boolean existsActiveByNameIgnoreCase(String nombre);

    boolean existsActiveByNameIgnoreCaseAndIdNot(String nombre, Integer excludedId);

    Implemento create(
            String nombre,
            String descripcion,
            Integer categoriaId,
            Integer locationId,
            ImplementItemType itemType,
            String observations
    );

    Implemento update(Integer id, String nombre, String descripcion, Integer categoriaId, Integer locationId);

    int updateMinStockByImplementId(Integer implementId, Integer minStock);

    Optional<Integer> findMinStockByImplementId(Integer implementId);
}
