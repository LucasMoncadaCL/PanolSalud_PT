package com.panol_project.backendpanol.modules.catalog.category.api;

import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.CategoriaAssociationSummaryResponse;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.CategoriaResponse;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.CreateCategoriaRequest;
import com.panol_project.backendpanol.modules.catalog.category.api.dto.UpdateCategoriaRequest;
import com.panol_project.backendpanol.modules.catalog.category.domain.Categoria;
import com.panol_project.backendpanol.modules.catalog.category.domain.CategoriaActiveSelectorResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/categorias")
public class CategoriaController {

    private final CategoriaService service;

    public CategoriaController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping("/gestion")
    List<CategoriaResponse> listarGestion() {
        return service.listarGestion().stream().map(this::toResponse).toList();
    }

    @GetMapping("/selector")
    List<CategoriaResponse> listarSelector() {
        return service.listarSelector().stream().map(this::toResponse).toList();
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<CategoriaActiveSelectorResponse> listarActivasParaSelectorProducto() {
        return service.listarSelector().stream()
                .map(categoria -> new CategoriaActiveSelectorResponse(categoria.id(), categoria.nombre()))
                .toList();
    }

    @GetMapping
    List<CategoriaResponse> listarCompatibilidad(@RequestParam(defaultValue = "true") boolean incluirInactivas) {
        return incluirInactivas
                ? service.listarGestion().stream().map(this::toResponse).toList()
                : service.listarSelector().stream().map(this::toResponse).toList();
    }

    @GetMapping("/{id}/asociaciones")
    CategoriaAssociationSummaryResponse resumenAsociaciones(@PathVariable Integer id) {
        int implementCount = service.contarImplementsAsociados(id);
        return new CategoriaAssociationSummaryResponse(id, implementCount, implementCount == 0);
    }

    @GetMapping("/{id}/validar-asignacion-implemento")
    ResponseEntity<Void> validarAsignacionImplemento(@PathVariable Integer id) {
        service.validarCategoriaActivaParaImplemento(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    CategoriaResponse crear(@Valid @RequestBody CreateCategoriaRequest request) {
        return toResponse(service.crear(request.nombre(), request.descripcion()));
    }

    @PutMapping("/{id}")
    CategoriaResponse editar(@PathVariable Integer id, @Valid @RequestBody UpdateCategoriaRequest request) {
        return toResponse(service.editar(id, request.nombre(), request.descripcion()));
    }

    @PatchMapping("/{id}/desactivar")
    CategoriaResponse desactivar(
            @PathVariable Integer id,
            @RequestParam(defaultValue = "false") boolean force) {
        return toResponse(service.desactivar(id, force));
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void eliminar(@PathVariable Integer id) {
        service.eliminar(id);
    }

    private CategoriaResponse toResponse(Categoria categoria) {
        return new CategoriaResponse(
                categoria.id(),
                categoria.nombre(),
                categoria.descripcion(),
                categoria.activa(),
                categoria.createdAt());
    }
}
