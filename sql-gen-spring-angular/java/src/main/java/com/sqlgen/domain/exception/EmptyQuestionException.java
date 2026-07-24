package com.sqlgen.domain.exception;

/** Excepcion de dominio: la pregunta esta vacia o es invalida. */
public class EmptyQuestionException extends RuntimeException {

    public EmptyQuestionException(String message) {
        super(message);
    }
}
