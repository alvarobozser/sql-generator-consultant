package com.sqlgen.infrastructure.web.dto;

import java.util.List;
import java.util.Map;

/**
 * DTO de salida del endpoint POST /api/v1/queries.
 * Equivalente al QueryResult de dominio, pero pensado para serializacion JSON.
 *
 * <p>Campos:
 * - sql: SQL generado (puede estar presente aunque haya error)
 * - rows: lista de filas (vacía si no hay resultados o si hubo error)
 * - columns: nombres de las columnas
 * - error: mensaje de error (null si todo OK)
 */
public record QueryResponse(
    String sql,
    List<Map<String, Object>> rows,
    List<String> columns,
    String error
) {

    public static QueryResponse from(
        String sql,
        List<Map<String, Object>> rows,
        List<String> columns,
        String error
    ) {
        return new QueryResponse(
            sql == null ? "" : sql,
            rows == null ? List.of() : rows,
            columns == null ? List.of() : columns,
            error
        );
    }
}
