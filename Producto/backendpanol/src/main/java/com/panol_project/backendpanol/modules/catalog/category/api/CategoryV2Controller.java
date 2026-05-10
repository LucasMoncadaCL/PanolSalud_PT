package com.panol_project.backendpanol.modules.catalog.category.api;

import com.panol_project.backendpanol.modules.catalog.category.api.dto.CategoryAssociationV2Response;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.CategoryActiveV2Response;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.CategoryManagementV2Response;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.CreateCategoriaRequest;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.UpdateCategoriaRequest;
import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.category.domain.Categoria;
import com.panol_project.backendpanol.shared.error.ApiException;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/api/v2/categories")
public class CategoryV2Controller {

    private final CategoriaService categoriaService;
    private final DSLContext dsl;

    public CategoryV2Controller(CategoriaService categoriaService, DSLContext dsl) {
        this.categoriaService = categoriaService;
        this.dsl = dsl;
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<CategoryActiveV2Response> listActive() {
        var categories = categoriaService.listarSelector();
        Map<Integer, UUID> uuidById = findUuidsByIds("category", categories.stream().map(c -> c.id()).toList());
        return categories.stream()
                .map(category -> new CategoryActiveV2Response(uuidById.get(category.id()), category.nombre()))
                .toList();
    }

    @GetMapping("/gestion")
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<CategoryManagementV2Response> listManagement() {
        var categories = categoriaService.listarGestion();
        Map<Integer, UUID> uuidById = findUuidsByIds("category", categories.stream().map(Categoria::id).toList());
        return categories.stream().map(category -> toResponse(category, uuidById.get(category.id()))).toList();
    }

    @GetMapping("/{categoryUuid}/associations")
    @PreAuthorize("hasRole('COORDINADOR')")
    public CategoryAssociationV2Response associations(@PathVariable UUID categoryUuid) {
        Integer categoryId = findIdByUuid("category", categoryUuid, "CATEGORY_NOT_FOUND");
        int implementCount = categoriaService.contarImplementsAsociados(categoryId);
        return new CategoryAssociationV2Response(categoryUuid, implementCount, implementCount == 0);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('COORDINADOR')")
    public CategoryManagementV2Response create(@Valid @RequestBody CreateCategoriaRequest request) {
        var created = categoriaService.crear(request.nombre(), request.descripcion());
        UUID uuid = findUuidsByIds("category", List.of(created.id())).get(created.id());
        return toResponse(created, uuid);
    }

    @PutMapping("/{categoryUuid}")
    @PreAuthorize("hasRole('COORDINADOR')")
    public CategoryManagementV2Response update(@PathVariable UUID categoryUuid, @Valid @RequestBody UpdateCategoriaRequest request) {
        Integer categoryId = findIdByUuid("category", categoryUuid, "CATEGORY_NOT_FOUND");
        var updated = categoriaService.editar(categoryId, request.nombre(), request.descripcion());
        return toResponse(updated, categoryUuid);
    }

    @PatchMapping("/{categoryUuid}/deactivate")
    @PreAuthorize("hasRole('COORDINADOR')")
    public CategoryManagementV2Response deactivate(@PathVariable UUID categoryUuid, @RequestParam(defaultValue = "false") boolean force) {
        Integer categoryId = findIdByUuid("category", categoryUuid, "CATEGORY_NOT_FOUND");
        var updated = categoriaService.desactivar(categoryId, force);
        return toResponse(updated, categoryUuid);
    }

    @DeleteMapping("/{categoryUuid}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @PreAuthorize("hasRole('COORDINADOR')")
    public void delete(@PathVariable UUID categoryUuid) {
        Integer categoryId = findIdByUuid("category", categoryUuid, "CATEGORY_NOT_FOUND");
        categoriaService.eliminar(categoryId);
    }

    private CategoryManagementV2Response toResponse(Categoria category, UUID uuid) {
        return new CategoryManagementV2Response(uuid, category.nombre(), category.descripcion(), category.activa(), category.createdAt());
    }

    private Integer findIdByUuid(String tableName, UUID uuid, String code) {
        Integer id = dsl.select(field(name("id"), Integer.class)).from(table(name(tableName))).where(field(name("uuid")).eq(uuid)).fetchOne(0, Integer.class);
        if (id == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado");
        return id;
    }

    private Map<Integer, UUID> findUuidsByIds(String tableName, List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return Map.of();
        var idField = field(name("id"), Integer.class);
        var uuidField = field(name("uuid"), UUID.class);
        return dsl.select(idField, uuidField).from(table(name(tableName))).where(idField.in(ids)).fetch().stream().collect(Collectors.toMap(r -> r.get(idField), r -> r.get(uuidField), (a, b) -> a));
    }
}
