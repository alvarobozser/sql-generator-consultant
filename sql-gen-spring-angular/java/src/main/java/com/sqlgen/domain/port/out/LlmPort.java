package com.sqlgen.domain.port.out;

import com.sqlgen.domain.exception.LlmException;

/**
 * Puerto de salida: comunicacion con un LLM.
 * La implementacion concreta (Spring AI, OpenAI SDK, etc.) vive en infrastructure.
 */
public interface LlmPort {

    /**
     * Envia un system prompt + user prompt al LLM y devuelve la respuesta cruda.
     *
     * @param systemPrompt Instrucciones del sistema (con schema, reglas, etc.).
     * @param userPrompt   Pregunta del usuario.
     * @return Texto devuelto por el LLM (puede venir con markdown ```sql, espacios extra).
     * @throws LlmException Si la llamada falla o la respuesta viene vacia.
     */
    String generate(String systemPrompt, String userPrompt);
}
