package com.panol_project.backendpanol.modules.catalog.category.api;

import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.category.domain.CategoriaActiveSelectorResponse;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/categories")
public class CategoryActiveController {

    private final CategoriaService service;

    public CategoryActiveController(CategoriaService service) {
        this.service = service;
    }

    @GetMapping("/active")
    @PreAuthorize("hasRole('COORDINADOR')")
    public List<CategoriaActiveSelectorResponse> listarActivasParaSelector() {
        return service.listarSelector().stream()
                .map(categoria -> new CategoriaActiveSelectorResponse(categoria.id(), categoria.nombre()))
                .toList();
    }
}

