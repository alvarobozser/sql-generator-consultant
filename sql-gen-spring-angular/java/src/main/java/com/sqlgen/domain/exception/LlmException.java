package com.sqlgen.domain.exception;

/** Excepcion de dominio: error al comunicarse con el LLM. */
public class LlmException extends RuntimeException {

    public LlmException(String message, Throwable cause) {
        super(message, cause);
    }

    public LlmException(String message) {
        super(message);
    }
}
