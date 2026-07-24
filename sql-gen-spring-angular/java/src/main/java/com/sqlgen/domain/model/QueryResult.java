package com.sqlgen.domain.model;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Resultado de procesar una pregunta. Record inmutable.
 *
 * <p>Tres estados posibles:
 * <ul>
 *   <li>Exito: sql + rows + columns populated, error == null</li>
 *   <li>Error del LLM / validador / BD: sql populated (puede estar), rows == null, error populated</li>
 *   <li>Estado vacio (sin pregunta aun): todos null</li>
 * </ul>
 */
public record QueryResult(
    String sql,
    List<Map<String, Object>> rows,
    List<String> columns,
    String error
) {

    /** Constructor canonico con listas inmutables. */
    public QueryResult {
        rows = rows == null ? null : List.copyOf(rows);
        columns = columns == null ? null : List.copyOf(columns);
    }

    /** Factory para un resultado exitoso. */
    public static QueryResult success(String sql, List<Map<String, Object>> rows, List<String> columns) {
        return new QueryResult(sql, rows, columns, null);
    }

    /** Factory para un resultado de error. sql puede ser null si fallo antes de generar SQL. */
    public static QueryResult failure(String sql, String error) {
        return new QueryResult(sql, null, null, error);
    }

    /** Factory para estado vacio (inicial, antes de que el usuario pregunte nada). */
    public static QueryResult empty() {
        return new QueryResult(null, null, null, null);
    }

    /** True si la consulta se ejecuto y devolvio filas. */
    public boolean isSuccess() {
        return error == null && rows != null;
    }

    /** True si hubo un error en cualquier capa. */
    public boolean isError() {
        return error != null;
    }

    /** Acceso seguro a rows (devuelve lista vacia si es null). */
    public List<Map<String, Object>> safeRows() {
        return rows == null ? Collections.emptyList() : rows;
    }
}
