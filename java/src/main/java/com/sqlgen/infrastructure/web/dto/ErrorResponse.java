package com.sqlgen.infrastructure.web.dto;

import java.util.Map;

/**
 * DTO estandar para errores HTTP.
 */
public record ErrorResponse(String error, Map<String, Object> details) {

    public static ErrorResponse of(String error) {
        return new ErrorResponse(error, Map.of());
    }
}
