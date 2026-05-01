package com.panol_project.backendpanol.modules.catalog.implement.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.panol_project.backendpanol.modules.catalog.implement.application.ImplementService;
import com.panol_project.backendpanol.modules.catalog.implement.domain.StockStatusFilter;
import com.panol_project.backendpanol.modules.catalog.stock.application.InventoryMovementService;
import com.panol_project.backendpanol.modules.users.application.UserService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Tests de seguridad para la restricción de rol en el parámetro ?stockStatus= de GET /api/implements.
 *
 * <p>Estos tests validan que la restricción de rol funcione correctamente en producción
 * cuando la seguridad JWT esté activada (app.security.enabled=true / HU-01 implementada).
 * Actualmente el filtro es accesible sin restricciones en dev (security disabled), pero
 * en producción solo el rol COORDINADOR podrá usarlo.</p>
 *
 * <p>Deuda técnica registrada: PSD-25 — observación aprobada 2026-05-01.</p>
 */
@WebMvcTest(ImplementController.class)
class ImplementControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ImplementService implementService;

    @MockBean
    private InventoryMovementService inventoryMovementService;

    @MockBean
    private UserService userService;

    /**
     * Verifica que un usuario con rol DOCENTE recibe 403 al intentar usar ?stockStatus=available.
     *
     * <p>Este test garantiza que la restricción de rol funcione correctamente cuando la seguridad
     * esté activa en producción. El rol DOCENTE no tiene permiso para filtrar por estado de stock.</p>
     */
    @Test
    @WithMockUser(roles = "DOCENTE")
    void listarConStockStatusDebeFallarCon403ParaRolDocente() throws Exception {
        mockMvc.perform(get("/api/implements").param("stockStatus", "available"))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifica que un usuario con rol DIRECTOR recibe 403 al intentar usar ?stockStatus=available.
     *
     * <p>El filtro por estado de stock es exclusivo del Coordinador de Laboratorio.
     * El Director de Carrera tiene acceso al catálogo pero no a este filtro específico.</p>
     */
    @Test
    @WithMockUser(roles = "DIRECTOR")
    void listarConStockStatusDebeFallarCon403ParaRolDirector() throws Exception {
        mockMvc.perform(get("/api/implements").param("stockStatus", "available"))
                .andExpect(status().isForbidden());
    }

    /**
     * Verifica que un usuario con rol COORDINADOR puede usar ?stockStatus=available exitosamente.
     *
     * <p>Complemento del test de 403: garantiza que la restricción no bloquea al rol correcto.</p>
     */
    @Test
    @WithMockUser(roles = "COORDINADOR")
    void listarConStockStatusDebePermitirseParaRolCoordinador() throws Exception {
        when(implementService.listar(isNull(), isNull(), eq(StockStatusFilter.AVAILABLE)))
                .thenReturn(List.of());

        mockMvc.perform(get("/api/implements").param("stockStatus", "available"))
                .andExpect(status().isOk());
    }
}
