package com.sqlgen.domain.port.out;

import com.sqlgen.domain.exception.UnsafeSqlException;

/**
 * Puerto de salida: validacion de seguridad del SQL.
 *
 * <p>La implementacion concreta (JSqlParser en Task 4) vive en infrastructure.
 * El dominio solo conoce esta interface.
 */
public interface SqlSafetyValidatorPort {

    /**
     * Valida que el SQL sea seguro para ejecutar.
     *
     * @param sql Query a validar.
     * @throws UnsafeSqlException Si el SQL no es seguro.
     */
    void validate(String sql);
}
