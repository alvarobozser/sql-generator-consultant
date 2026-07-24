package com.sqlgen.domain.port.out;

import com.sqlgen.domain.exception.SqlExecutionException;

import java.util.List;
import java.util.Map;

/**
 * Puerto de salida: ejecucion de queries SQL contra la base de datos.
 *
 * <p>La implementacion debe usar un usuario de solo lectura (defense in depth)
 * y debe lanzar SqlExecutionException si la query falla por motivos de BD.
 */
public interface DatabasePort {

    /**
     * Ejecuta un SELECT (validado previamente) y devuelve las filas.
     *
     * @param sql Query SELECT, WITH o EXPLAIN (ya validada como segura).
     * @return Lista de filas como Maps {columna: valor}.
     * @throws SqlExecutionException Si la query falla.
     */
    List<Map<String, Object>> executeQuery(String sql);
}
