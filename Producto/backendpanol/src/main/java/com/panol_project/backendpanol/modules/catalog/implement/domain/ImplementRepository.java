package com.panol_project.backendpanol.modules.catalog.implement.domain;

import java.util.Optional;
import java.util.List;

public interface ImplementRepository {

    Optional<Implemento> findById(Integer id);

    List<ImplementSummary> findAllSummaries();

    boolean existsActiveByNameIgnoreCase(String nombre);

    boolean existsActiveByNameIgnoreCaseAndIdNot(String nombre, Integer excludedId);

    Implemento create(String nombre, String descripcion, Integer categoriaId, Integer locationId);

    Implemento update(Integer id, String nombre, String descripcion, Integer categoriaId, Integer locationId);
}
