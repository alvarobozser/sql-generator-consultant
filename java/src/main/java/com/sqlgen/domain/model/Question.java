package com.sqlgen.domain.model;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.EmptyQuestionException;

/**
 * Pregunta del usuario en lenguaje natural. Record inmutable.
 *
 * <p>Lanza EmptyQuestionException si el texto esta vacio o es solo espacios.
 */
public record Question(String text, Language language) {

    private static final int MAX_LENGTH = 2_000;

    public Question {
        if (text == null || text.isBlank()) {
            throw new EmptyQuestionException("La pregunta no puede estar vacia");
        }
        if (text.length() > MAX_LENGTH) {
            throw new EmptyQuestionException(
                "La pregunta es demasiado larga (max " + MAX_LENGTH + " caracteres)"
            );
        }
        if (language == null) {
            throw new EmptyQuestionException("El idioma no puede ser null");
        }
    }
}
