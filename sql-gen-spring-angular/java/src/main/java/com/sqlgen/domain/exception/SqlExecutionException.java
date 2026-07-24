package com.sqlgen.domain.exception;

/** Excepcion de dominio: error al ejecutar SQL contra la base de datos. */
public class SqlExecutionException extends RuntimeException {

    public SqlExecutionException(String message, Throwable cause) {
        super(message, cause);
    }

    public SqlExecutionException(String message) {
        super(message);
    }
}
