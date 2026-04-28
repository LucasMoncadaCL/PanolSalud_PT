package com.panol_project.backendpanol.modules.catalog.implement.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.implement.domain.ImplementRepository;
import com.panol_project.backendpanol.modules.catalog.implement.domain.Implemento;
import com.panol_project.backendpanol.modules.catalog.location.application.LocationService;
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
    private CategoriaService categoriaService;

    @Mock
    private LocationService locationService;

    private ImplementService service;

    @BeforeEach
    void setUp() {
        service = new ImplementService(repository, categoriaService, locationService);
    }

    @Test
    void crearDebePermitirCategoriaNull() {
        Implemento created = new Implemento(1, "Guantes", null, null, 10, true, OffsetDateTime.now(), OffsetDateTime.now());

        when(repository.create("Guantes", null, null, 10)).thenReturn(created);

        Implemento result = service.crear("Guantes", null, null, 10);

        assertEquals(1, result.id());
        verify(categoriaService).validarCategoriaActivaParaImplemento(null);
        verify(locationService).validarLocationExistente(10);
    }

    @Test
    void crearDebeFallarSiCategoriaInactivaONoExiste() {
        doThrow(new BadRequestException(
                "CATEGORY_INACTIVE_OR_NOT_FOUND",
                "No se puede asignar una categoria inactiva al implemento"
        )).when(categoriaService).validarCategoriaActivaParaImplemento(99);

        assertThrows(BadRequestException.class, () -> service.crear("Guantes", null, 99, 10));
        verify(repository, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void crearDebeFallarConBadRequestSiNombreActivoYaExiste() {
        when(repository.existsActiveByNameIgnoreCase("Guantes")).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.crear("Guantes", null, null, 10));

        assertEquals("IMPLEMENT_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe un producto con el nombre 'Guantes'", ex.getMessage());
        verify(repository, never()).create(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }

    @Test
    void crearDebeRetornarBadRequestSiNombreDuplicadoPorConstraintUnico() {
        when(repository.create("Guantes", null, null, 10)).thenThrow(new DataIntegrityViolationException(
                "unique violation",
                new SQLException("duplicate key", "23505")
        ));

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.crear("Guantes", null, null, 10));

        assertEquals("IMPLEMENT_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe un producto con el nombre 'Guantes'", ex.getMessage());
    }

    @Test
    void editarDebeFallarSiImplementoNoExiste() {
        when(repository.findById(10)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.editar(10, "X", null, null, 10));
    }

    @Test
    void editarDebeValidarCategoriaSiExisteImplemento() {
        Implemento existing = new Implemento(10, "Existente", null, null, 10, true, OffsetDateTime.now(), OffsetDateTime.now());
        Implemento updated = new Implemento(10, "Nuevo", null, 2, 10, true, OffsetDateTime.now(), OffsetDateTime.now());

        when(repository.findById(10)).thenReturn(Optional.of(existing));
        when(repository.update(10, "Nuevo", null, 2, 10)).thenReturn(updated);

        Implemento result = service.editar(10, "Nuevo", null, 2, 10);

        assertEquals(2, result.categoriaId());
        verify(categoriaService).validarCategoriaActivaParaImplemento(2);
        verify(locationService).validarLocationExistente(10);
    }

    @Test
    void editarDebeFallarConBadRequestSiNombreActivoExisteEnOtroImplemento() {
        Implemento existing = new Implemento(10, "Existente", null, null, 10, true, OffsetDateTime.now(), OffsetDateTime.now());
        when(repository.findById(10)).thenReturn(Optional.of(existing));
        when(repository.existsActiveByNameIgnoreCaseAndIdNot("Guantes", 10)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.editar(10, "Guantes", null, null, 10));

        assertEquals("IMPLEMENT_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe un producto con el nombre 'Guantes'", ex.getMessage());
        verify(repository, never()).update(
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.any()
        );
    }
}
