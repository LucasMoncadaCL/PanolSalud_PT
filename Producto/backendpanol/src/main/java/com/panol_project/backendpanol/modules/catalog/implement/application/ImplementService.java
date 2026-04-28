package com.panol_project.backendpanol.modules.catalog.implement.application;

import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementItemType;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementRepository;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementSummary;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import com.panol_project.backendpanol.modules.catalog.location.application.LocationService;
import com.panol_project.backendpanol.shared.error.BadRequestException;
import com.panol_project.backendpanol.shared.error.NotFoundException;
import java.util.List;
import java.sql.SQLException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ImplementService {

    private final ImplementRepository repository;
    private final CategoriaService categoriaService;
    private final LocationService locationService;

    public ImplementService(
            ImplementRepository repository,
            CategoriaService categoriaService,
            LocationService locationService
    ) {
        this.repository = repository;
        this.categoriaService = categoriaService;
        this.locationService = locationService;
    }

    @Transactional
    public Implemento crear(
            String nombre,
            String descripcion,
            Integer categoriaId,
            Integer locationId,
            String itemType,
            Integer minStock,
            String observations
    ) {
        categoriaService.validarCategoriaActivaParaImplemento(categoriaId);
        String normalizedName = normalizeNombre(nombre);
        String normalizedDescription = normalizeDescripcion(descripcion);
        String normalizedObservations = normalizeObservations(observations);
        ImplementItemType normalizedItemType = parseItemType(itemType);
        locationService.validarLocationExistente(locationId);
        validateUniqueActiveNameForCreate(normalizedName);

        try {
            Implemento created = repository.create(
                    normalizedName,
                    normalizedDescription,
                    categoriaId,
                    locationId,
                    normalizedItemType,
                    normalizedObservations
            );
            repository.updateMinStockByImplementId(created.id(), minStock);
            return created;
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw duplicateNameException(normalizedName);
            }
            throw ex;
        }
    }

    @Transactional
    public Implemento editar(Integer id, String nombre, String descripcion, Integer categoriaId, Integer locationId) {
        requireImplement(id);
        categoriaService.validarCategoriaActivaParaImplemento(categoriaId);
        locationService.validarLocationExistente(locationId);
        String normalizedName = normalizeNombre(nombre);
        String normalizedDescription = normalizeDescripcion(descripcion);
        validateUniqueActiveNameForUpdate(normalizedName, id);

        try {
            return repository.update(id, normalizedName, normalizedDescription, categoriaId, locationId);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw duplicateNameException(normalizedName);
            }
            throw ex;
        }
    }

    @Transactional(readOnly = true)
    public Implemento obtener(Integer id) {
        return requireImplement(id);
    }

    @Transactional(readOnly = true)
    public Integer obtenerStockMinimo(Integer implementId) {
        return repository.findMinStockByImplementId(implementId).orElse(null);
    }

    @Transactional(readOnly = true)
    public List<ImplementSummary> listar() {
        return repository.findAllSummaries();
    }

    private Implemento requireImplement(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("IMPLEMENT_NOT_FOUND", "Implemento no encontrado"));
    }

    private String normalizeNombre(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    private String normalizeDescripcion(String descripcion) {
        if (descripcion == null) {
            return null;
        }
        String normalized = descripcion.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private String normalizeObservations(String observations) {
        if (observations == null) {
            return null;
        }
        String normalized = observations.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    private void validateUniqueActiveNameForCreate(String normalizedName) {
        if (repository.existsActiveByNameIgnoreCase(normalizedName)) {
            throw duplicateNameException(normalizedName);
        }
    }

    private void validateUniqueActiveNameForUpdate(String normalizedName, Integer id) {
        if (repository.existsActiveByNameIgnoreCaseAndIdNot(normalizedName, id)) {
            throw duplicateNameException(normalizedName);
        }
    }

    private BadRequestException duplicateNameException(String normalizedName) {
        return new BadRequestException(
                "IMPLEMENT_NAME_DUPLICATE",
                "Ya existe un producto con el nombre '" + normalizedName + "'"
        );
    }

    private ImplementItemType parseItemType(String itemType) {
        return ImplementItemType.fromLiteral(itemType)
                .orElseThrow(() -> new BadRequestException(
                        "IMPLEMENT_ITEM_TYPE_INVALID",
                        "El tipo de implemento debe ser consumable, reusable o individual"
                ));
    }

    private boolean isUniqueViolation(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SQLException sqlException) {
                return "23505".equals(sqlException.getSQLState());
            }
            current = current.getCause();
        }
        return false;
    }
}
