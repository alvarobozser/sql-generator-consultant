package com.sqlgen.infrastructure.web;

import com.sqlgen.domain.model.SchemaInfo;
import com.sqlgen.domain.port.out.SchemaPort;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Controller que expone el schema de la BD al frontend. */
@RestController
@RequestMapping("/api/v1/schema")
@Tag(name = "schema", description = "Schema de la base de datos")
public class SchemaController {

    private final SchemaPort schemaPort;

    public SchemaController(SchemaPort schemaPort) {
        this.schemaPort = schemaPort;
    }

    @GetMapping
    @Operation(summary = "Devuelve el schema (tablas y columnas) de la BD")
    public ResponseEntity<SchemaInfo> getSchema() {
        return ResponseEntity.ok(schemaPort.getSchema());
    }
}
