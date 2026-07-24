package com.sqlgen.domain.port.in;

import com.sqlgen.domain.model.QueryResult;
import com.sqlgen.domain.model.Question;

/**
 * Puerto de entrada: use case principal del sistema.
 *
 * <p>La idea de la arquitectura hexagonal: el dominio define QUE se hace
 * (esta interface); la capa de infraestructura (controllers REST, etc.) llama
 * a la implementacion sin saber los detalles tecnicos.
 */
public interface ProcessQuestionUseCase {

    /**
     * Procesa una pregunta del usuario y devuelve el resultado.
     *
     * @param question Pregunta + idioma (validado por el record).
     * @return QueryResult con sql, rows, columns, error.
     */
    QueryResult process(Question question);
}
