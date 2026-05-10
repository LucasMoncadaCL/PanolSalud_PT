package com.panol_project.backendpanol.modules.catalog.stock.api;

import com.panol_project.backendpanol.modules.catalog.stock.application.BarcodeLabelService;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/internal/legacy/api/implements/{implementId}/labels")
public class BarcodeLabelController {

    private final BarcodeLabelService service;

    public BarcodeLabelController(BarcodeLabelService service) {
        this.service = service;
    }

    @GetMapping(value = "/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasRole('COORDINADOR')")
    public ResponseEntity<byte[]> generatePdf(
            @PathVariable Integer implementId,
            @RequestParam(name = "scope", required = false, defaultValue = "GENERAL") String scope,
            @RequestParam(name = "quantity", required = false, defaultValue = "1") Integer quantity,
            @RequestParam(name = "individual_id", required = false) Integer individualId
    ) {
        byte[] payload = service.generateLabelsPdf(implementId, scope, quantity, individualId);
        String filename = "etiquetas-implemento-" + implementId + "-" + scope.toLowerCase() + ".pdf";
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline().filename(filename).build().toString())
                .contentType(MediaType.APPLICATION_PDF)
                .body(payload);
    }
}
