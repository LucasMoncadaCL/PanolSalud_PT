package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.shared.error.ApiException;
import java.util.UUID;
import org.jooq.DSLContext;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import static org.jooq.impl.DSL.field;
import static org.jooq.impl.DSL.name;
import static org.jooq.impl.DSL.table;

@RestController
@RequestMapping("/api/v2/implements/{implementUuid}/labels")
public class BarcodeLabelV2Controller {

    private final BarcodeLabelController legacyController;
    private final DSLContext dsl;

    public BarcodeLabelV2Controller(BarcodeLabelController legacyController, DSLContext dsl) {
        this.legacyController = legacyController;
        this.dsl = dsl;
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<byte[]> generatePdf(@PathVariable UUID implementUuid, @RequestParam(name = "scope", required = false, defaultValue = "GENERAL") String scope,
            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity,
            @RequestParam(name = "individual_uuid", required = false) UUID individualUuid) {
        Integer individualId = individualUuid == null ? null : findIdByUuid("individual", individualUuid, "INDIVIDUAL_NOT_FOUND");
        return legacyController.generatePdf(findIdByUuid("implement", implementUuid, "IMPLEMENT_NOT_FOUND"), scope, quantity, individualId);
    }

    private Integer findIdByUuid(String tableName, UUID uuid, String code) { Integer id = dsl.select(field(name("id"), Integer.class)).from(table(name(tableName))).where(field(name("uuid")).eq(uuid)).fetchOne(0, Integer.class); if (id == null) throw new ApiException(HttpStatus.NOT_FOUND, code, "Recurso no encontrado"); return id; }
}
