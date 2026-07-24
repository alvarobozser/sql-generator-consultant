package com.sqlgen.domain.port.out;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.EmptyQuestionException;

/**
 * Puerto de salida: acceso a los prompts del sistema por idioma.
 *
 * <p>La implementacion lee de un ResourceBundle, de archivos properties, etc.
 * Lo importante: el dominio solo conoce esta interface.
 */
public interface PromptPort {

    /**
     * Devuelve el system prompt completo para el idioma dado.
     *
     * @param language Idioma (ES o EN).
     * @return System prompt con schema, reglas y few-shot examples.
     * @throws EmptyQuestionException Si el idioma no es soportado.
     */
    String getSystemPrompt(Language language);
}
