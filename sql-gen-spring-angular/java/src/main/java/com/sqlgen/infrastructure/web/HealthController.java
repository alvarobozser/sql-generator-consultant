package com.sqlgen.infrastructure.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Health check del backend. */
@RestController
@RequestMapping("/api/v1/health")
@Tag(name = "health", description = "Health check del servicio")
public class HealthController {

    @GetMapping
    @Operation(summary = "Devuelve el estado del servicio")
    public ResponseEntity<Map<String, String>> health() {
        return ResponseEntity.ok(Map.of("status", "UP"));
    }
}
