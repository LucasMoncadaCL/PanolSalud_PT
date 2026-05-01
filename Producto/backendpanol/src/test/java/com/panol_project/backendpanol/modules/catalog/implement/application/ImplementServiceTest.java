package com.panol_project.backendpanol.modules.catalog.implement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.category.domain.Categoria;
import com.panol_project.backendpanol.modules.catalog.category.domain.CategoriaRepository;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementItemType;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementRepository;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import com.panol_project.backendpanol.modules.catalog.implement.domain.StockStatusFilter;
import com.panol_project.backendpanol.modules.catalog.location.application.LocationService;
import com.panol_project.backendpanol.modules.catalog.location.domain.LocationRepository;
import com.panol_project.backendpanol.shared.error.BadRequestException;
import com.panol_project.backendpanol.shared.error.NotFoundException;
import java.sql.SQLException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

@ExtendWith(MockitoExtension.class)
class ImplementServiceTest {

    @Mock
    private ImplementRepository repository;

    @Mock
    private CategoriaRepository categoriaRepository;

    @Mock
    private LocationRepository locationRepository;

    private CategoriaService categoriaService;
    private LocationService locationService;

    private ImplementService service;

    @BeforeEach
    void setUp() {
        categoriaService = new CategoriaService(categoriaRepository);
        locationService = new LocationService(locationRepository);
        service = new ImplementService(repository, categoriaService, locationService);
    }

    @Test
    void crearDebeValidarLocationYActualizarStockMinimo() {
        OffsetDateTime now = OffsetDateTime.now();
        Implemento created = new Implemento(1, "Guantes", null, 5, 10, ImplementItemType.REUSABLE, null, null, null, true, now, now);

        when(categoriaRepository.findActiveById(5)).thenReturn(Optional.of(new Categoria(5, "Cat", null, true, now)));
        when(locationRepository.existsById(10)).thenReturn(true);
        when(repository.create("Guantes", null, 5, 10, ImplementItemType.REUSABLE, null, null, null)).thenReturn(created);
        when(repository.updateMinStockByImplementId(1, 3)).thenReturn(1);

        Implemento result = service.crear("Guantes", null, 5, 10, "reusable", 3, " ", null, null);

        assertEquals(1, result.id());
        verify(categoriaRepository).findActiveById(5);
        verify(locationRepository).existsById(10);
        verify(repository).updateMinStockByImplementId(1, 3);
    }

    @Test
    void crearDebeFallarSiCategoriaInactivaONoExiste() {
        when(categoriaRepository.findActiveById(99)).thenReturn(Optional.empty());

        assertThrows(BadRequestException.class, () ->
                service.crear("Guantes", null, 99, 10, "consumable", 5, null, null, null));
        verify(repository, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void crearDebeFallarSiItemTypeNoEsValido() {
        OffsetDateTime now = OffsetDateTime.now();
        when(categoriaRepository.findActiveById(5)).thenReturn(Optional.of(new Categoria(5, "Cat", null, true, now)));
        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.crear("Guantes", null, 5, 10, "otro", 5, null, null, null));

        assertEquals("IMPLEMENT_ITEM_TYPE_INVALID", ex.getCode());
        verify(repository, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void crearDebeFallarConBadRequestSiNombreActivoYaExiste() {
        OffsetDateTime now = OffsetDateTime.now();
        when(categoriaRepository.findActiveById(5)).thenReturn(Optional.of(new Categoria(5, "Cat", null, true, now)));
        when(locationRepository.existsById(10)).thenReturn(true);
        when(repository.existsActiveByNameIgnoreCase("Guantes")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.crear("Guantes", null, 5, 10, "consumable", 4, null, null, null));

        assertEquals("IMPLEMENT_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe un producto con el nombre 'Guantes'", ex.getMessage());
        verify(repository, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void crearDebeRetornarBadRequestSiNombreDuplicadoPorConstraintUnico() {
        OffsetDateTime now = OffsetDateTime.now();
        when(categoriaRepository.findActiveById(5)).thenReturn(Optional.of(new Categoria(5, "Cat", null, true, now)));
        when(locationRepository.existsById(10)).thenReturn(true);
        when(repository.create("Guantes", null, 5, 10, ImplementItemType.CONSUMABLE, null, null, null))
                .thenThrow(new DataIntegrityViolationException(
                        "unique violation",
                        new SQLException("duplicate key", "23505")
                ));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.crear("Guantes", null, 5, 10, "consumable", 3, null, null, null));

        assertEquals("IMPLEMENT_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe un producto con el nombre 'Guantes'", ex.getMessage());
    }

    @Test
    void editarDebeFallarSiImplementoNoExiste() {
        when(repository.findById(10)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.editar(10, "X", null, null, 10, "reusable", 1, null, null, null));
    }

    @Test
    void editarDebeValidarCategoriaSiExisteImplemento() {
        OffsetDateTime now = OffsetDateTime.now();
        Implemento existing = new Implemento(10, "Existente", null, 2, 10, ImplementItemType.REUSABLE, null, null, null, true, now, now);
        Implemento updated = new Implemento(10, "Nuevo", null, 2, 10, ImplementItemType.REUSABLE, null, null, "Obs", true, now, now);

        when(categoriaRepository.findActiveById(2)).thenReturn(Optional.of(new Categoria(2, "Cat", null, true, now)));
        when(locationRepository.existsById(10)).thenReturn(true);
        when(repository.findById(10)).thenReturn(Optional.of(existing));
        when(repository.update(10, "Nuevo", null, 2, 10, ImplementItemType.REUSABLE, null, null, "Obs")).thenReturn(updated);
        when(repository.updateMinStockByImplementId(10, 1)).thenReturn(1);

        Implemento result = service.editar(10, "Nuevo", null, 2, 10, "reusable", 1, "Obs", null, null);

        assertEquals(2, result.categoriaId());
        verify(categoriaRepository).findActiveById(2);
        verify(locationRepository).existsById(10);
        verify(repository).updateMinStockByImplementId(10, 1);
    }

    @Test
    void editarDebeFallarSiImplementoEstaInactivo() {
        OffsetDateTime now = OffsetDateTime.now();
        Implemento existing = new Implemento(10, "Existente", null, 2, 10, ImplementItemType.REUSABLE, null, null, null, false, now, now);
        when(repository.findById(10)).thenReturn(Optional.of(existing));

        BadRequestException ex = assertThrows(BadRequestException.class, () ->
                service.editar(10, "Nuevo", null, 2, 10, "reusable", 1, null, null, null));

        assertEquals("IMPLEMENT_INACTIVE", ex.getCode());
        assertEquals("No se puede editar un producto inactivo", ex.getMessage());
        verify(repository, never()).update(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
        verify(repository, never()).updateMinStockByImplementId(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void editarDebeFallarConBadRequestSiNombreActivoExisteEnOtroImplemento() {
        OffsetDateTime now = OffsetDateTime.now();
        Implemento existing = new Implemento(10, "Existente", null, 2, 10, ImplementItemType.REUSABLE, null, null, null, true, now, now);
        when(categoriaRepository.findActiveById(2)).thenReturn(Optional.of(new Categoria(2, "Cat", null, true, now)));
        when(locationRepository.existsById(10)).thenReturn(true);
        when(repository.findById(10)).thenReturn(Optional.of(existing));
        when(repository.existsActiveByNameIgnoreCaseAndIdNot("Guantes", 10)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.editar(10, "Guantes", null, 2, 10, "reusable", 1, null, null, null));

        assertEquals("IMPLEMENT_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe un producto con el nombre 'Guantes'", ex.getMessage());
        verify(repository, never()).update(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void listarDebeAplicarFiltrosCombinados() {
        when(repository.findAllSummaries("Guante", 3, null)).thenReturn(java.util.List.of());

        service.listar("  Guante ", 3, null);

        verify(repository).findAllSummaries("Guante", 3, null);
    }

    /**
     * Verifica que el filtro BLOCKED se propaga correctamente al repositorio.
     * Cubre la deuda técnica registrada: el campo 'blocked' usa DSL.field() dinámico
     * en lugar del field tipado del codegen. La lógica de propagación debe ser la misma
     * que para los demás estados (AVAILABLE, RESERVED, LOANED, DAMAGED).
     *
     * <p>Cuando el codegen incluya STOCK.BLOCKED, este test seguirá siendo válido
     * sin modificaciones.</p>
     */
    @Test
    void listarConFiltroBlockedDebePropagarsAlRepositorio() {
        when(repository.findAllSummaries(null, null, StockStatusFilter.BLOCKED))
                .thenReturn(java.util.List.of());

        service.listar(null, null, StockStatusFilter.BLOCKED);

        verify(repository).findAllSummaries(null, null, StockStatusFilter.BLOCKED);
    }
}
