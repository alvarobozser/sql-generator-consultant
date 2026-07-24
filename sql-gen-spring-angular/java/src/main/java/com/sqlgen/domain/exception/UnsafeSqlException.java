package com.sqlgen.domain.exception;

/**
 * Excepcion de dominio: el SQL generado por el LLM no es seguro (no es solo-lectura).
 */
public class UnsafeSqlException extends RuntimeException {

    public UnsafeSqlException(String message) {
        super(message);
    }
}
