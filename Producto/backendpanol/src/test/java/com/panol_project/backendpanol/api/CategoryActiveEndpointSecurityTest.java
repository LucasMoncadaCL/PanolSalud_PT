package com.panol_project.backendpanol.api;

import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panol_project.backendpanol.bootstrap.config.SecurityConfig;
import com.panol_project.backendpanol.modules.catalog.category.api.CategoryActiveController;
import com.panol_project.backendpanol.modules.catalog.category.application.CategoriaService;
import com.panol_project.backendpanol.modules.catalog.category.domain.Categoria;
import com.panol_project.backendpanol.shared.error.security.RestAccessDeniedHandler;
import com.panol_project.backendpanol.shared.error.security.RestAuthenticationEntryPoint;
import java.time.OffsetDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.TestPropertySource;

@WebMvcTest(controllers = CategoryActiveController.class)
@Import({
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class
})
@TestPropertySource(properties = "app.security.enabled=true")
class CategoryActiveEndpointSecurityTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private CategoriaService service;

    @MockBean
    private JwtDecoder jwtDecoder;

    @Test
    void getActiveDebeRequerirJwt() throws Exception {
        mvc.perform(get("/api/categories/active"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void getActiveDebeRechazarSinRolCoordinador() throws Exception {
        mvc.perform(get("/api/categories/active").with(jwt()))
                .andExpect(status().isForbidden());
    }

    @Test
    void getActiveDebeRetornarCategoriasActivasConRolCoordinador() throws Exception {
        when(service.listarSelector()).thenReturn(List.of(
                new Categoria(1, "Reactivos", null, true, OffsetDateTime.now()),
                new Categoria(2, "Insumos", null, true, OffsetDateTime.now())
        ));

        mvc.perform(get("/api/categories/active").with(jwt()
                        .authorities(new SimpleGrantedAuthority("ROLE_COORDINADOR"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(1))
                .andExpect(jsonPath("$[0].name").value("Reactivos"))
                .andExpect(jsonPath("$[1].id").value(2))
                .andExpect(jsonPath("$[1].name").value("Insumos"));
    }
}
