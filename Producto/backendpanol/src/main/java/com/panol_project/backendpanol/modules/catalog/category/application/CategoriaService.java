package com.panol_project.backendpanol.modules.catalog.category.application;

import com.panol_project.backendpanol.jooq.tables.records.CategoryRecord;
import com.panol_project.backendpanol.modules.catalog.category.domain.CategoriaResponse;
import com.panol_project.backendpanol.modules.catalog.category.infrastructure.CategoriaRepository;
import com.panol_project.backendpanol.shared.error.BadRequestException;
import com.panol_project.backendpanol.shared.error.ConflictException;
import com.panol_project.backendpanol.shared.error.NotFoundException;
import java.sql.SQLException;
import java.util.List;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CategoriaService {

    private final CategoriaRepository repository;

    public CategoriaService(CategoriaRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarGestion() {
        return repository.findAll(true);
    }

    @Transactional(readOnly = true)
    public List<CategoriaResponse> listarSelector() {
        return repository.findOnlyActive();
    }

    @Transactional
    public CategoriaResponse crear(String nombre) {
        String normalizedName = normalizeNombre(nombre);
        validateNombreUnico(normalizedName, null);

        try {
            return repository.create(normalizedName);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw duplicateNameError(normalizedName);
            }
            throw ex;
        }
    }

    @Transactional
    public CategoriaResponse editar(Integer id, String nombre) {
        CategoryRecord categoria = requireCategoria(id);
        String normalizedName = normalizeNombre(nombre);

        validateNombreUnico(normalizedName, categoria.getId());

        try {
            return repository.updateNombre(id, normalizedName);
        } catch (DataIntegrityViolationException ex) {
            if (isUniqueViolation(ex)) {
                throw duplicateNameError(normalizedName);
            }
            throw ex;
        }
    }

    @Transactional
    public CategoriaResponse desactivar(Integer id, boolean force) {
        CategoryRecord categoria = requireCategoria(id);
        if (!Boolean.TRUE.equals(categoria.getActive())) {
            return repository.toResponse(categoria);
        }

        int activeImplements = repository.countActiveImplementsByCategoryId(id);
        if (activeImplements > 0 && !force) {
            throw new ConflictException(
                    "CATEGORY_HAS_ACTIVE_IMPLEMENTS",
                    "La categoria tiene " + activeImplements + " implementos activos asociados"
            );
        }

        repository.deactivate(id);
        categoria.setActive(false);
        return repository.toResponse(categoria);
    }

    @Transactional
    public void eliminar(Integer id) {
        requireCategoria(id);

        int totalImplements = repository.countImplementsByCategoryId(id);
        if (totalImplements > 0) {
            throw new BadRequestException(
                    "CATEGORY_HAS_IMPLEMENTS",
                    "No se puede eliminar la categoria porque tiene " + totalImplements + " implementos asociados"
            );
        }

        repository.deleteById(id);
    }

    @Transactional(readOnly = true)
    public void validarCategoriaActivaParaImplemento(Integer categoryId) {
        if (categoryId == null) {
            return;
        }

        boolean existsActive = repository.findActiveById(categoryId).isPresent();
        if (!existsActive) {
            throw new BadRequestException(
                    "CATEGORY_INACTIVE_OR_NOT_FOUND",
                    "No se puede asignar una categoria inactiva al implemento"
            );
        }
    }

    @Transactional(readOnly = true)
    public int contarImplementsAsociados(Integer id) {
        requireCategoria(id);
        return repository.countImplementsByCategoryId(id);
    }

    private void validateNombreUnico(String nombre, Integer excludingId) {
        if (repository.existsByNombre(nombre, excludingId)) {
            throw duplicateNameError(nombre);
        }
    }

    private CategoryRecord requireCategoria(Integer id) {
        return repository.findById(id)
                .orElseThrow(() -> new NotFoundException("CATEGORY_NOT_FOUND", "Categoria no encontrada"));
    }

    private String normalizeNombre(String nombre) {
        return nombre == null ? "" : nombre.trim();
    }

    private BadRequestException duplicateNameError(String nombre) {
        return new BadRequestException(
                "CATEGORY_NAME_DUPLICATE",
                "Ya existe una categoria con el nombre '" + nombre + "'"
        );
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
