package com.panol_project.backendpanol.modules.catalog.category.application;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.panol_project.backendpanol.jooq.tables.records.CategoryRecord;
import com.panol_project.backendpanol.modules.catalog.category.domain.CategoriaResponse;
import com.panol_project.backendpanol.modules.catalog.category.infrastructure.CategoriaRepository;
import com.panol_project.backendpanol.shared.error.BadRequestException;
import com.panol_project.backendpanol.shared.error.ConflictException;
import java.time.OffsetDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CategoriaServiceTest {

    @Mock
    private CategoriaRepository repository;

    private CategoriaService service;

    @BeforeEach
    void setUp() {
        service = new CategoriaService(repository);
    }

    @Test
    void crearDebeFallarConBadRequestSiNombreDuplicado() {
        when(repository.existsByNombre("Reactivos", null)).thenReturn(true);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.crear("Reactivos"));

        assertEquals("CATEGORY_NAME_DUPLICATE", ex.getCode());
        assertEquals("Ya existe una categoria con el nombre 'Reactivos'", ex.getMessage());
    }

    @Test
    void desactivarDebeRetornarConflictSiHayImplementsActivosSinForce() {
        CategoryRecord record = new CategoryRecord();
        record.setId(10);
        record.setName("Reactivos");
        record.setActive(true);
        record.setCreatedAt(OffsetDateTime.now());

        when(repository.findById(10)).thenReturn(Optional.of(record));
        when(repository.countActiveImplementsByCategoryId(10)).thenReturn(3);

        ConflictException ex = assertThrows(ConflictException.class, () -> service.desactivar(10, false));

        assertEquals("CATEGORY_HAS_ACTIVE_IMPLEMENTS", ex.getCode());
        verify(repository, never()).deactivate(any());
    }

    @Test
    void eliminarDebeFallarSiTieneImplementsAsociados() {
        CategoryRecord record = new CategoryRecord();
        record.setId(7);
        record.setName("Insumos");
        record.setActive(true);
        record.setCreatedAt(OffsetDateTime.now());

        when(repository.findById(7)).thenReturn(Optional.of(record));
        when(repository.countImplementsByCategoryId(7)).thenReturn(2);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> service.eliminar(7));

        assertEquals("CATEGORY_HAS_IMPLEMENTS", ex.getCode());
        verify(repository, never()).deleteById(any());
    }

    @Test
    void validarCategoriaActivaParaImplementoDebeFallarSiInactiva() {
        when(repository.findActiveById(2)).thenReturn(Optional.empty());

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.validarCategoriaActivaParaImplemento(2));

        assertEquals("CATEGORY_INACTIVE_OR_NOT_FOUND", ex.getCode());
    }

    @Test
    void desactivarConForceDebeDesactivarYRetornarCategoria() {
        CategoryRecord active = new CategoryRecord();
        active.setId(5);
        active.setName("Activa");
        active.setActive(true);
        active.setCreatedAt(OffsetDateTime.now());

        when(repository.findById(5)).thenReturn(Optional.of(active));
        when(repository.countActiveImplementsByCategoryId(5)).thenReturn(1);
        when(repository.toResponse(active)).thenReturn(new CategoriaResponse(
                5,
                "Activa",
                false,
                active.getCreatedAt(),
                null
        ));

        CategoriaResponse response = service.desactivar(5, true);

        assertEquals(5, response.id());
        verify(repository).deactivate(eq(5));
    }
}
