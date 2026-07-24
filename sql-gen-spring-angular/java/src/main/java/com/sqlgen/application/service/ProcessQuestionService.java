package com.sqlgen.application.service;

import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.exception.SqlExecutionException;
import com.sqlgen.domain.exception.UnsafeSqlException;
import com.sqlgen.domain.model.QueryResult;
import com.sqlgen.domain.model.Question;
import com.sqlgen.domain.port.in.ProcessQuestionUseCase;
import com.sqlgen.domain.port.out.DatabasePort;
import com.sqlgen.domain.port.out.LlmPort;
import com.sqlgen.domain.port.out.PromptPort;
import com.sqlgen.domain.port.out.SqlSafetyValidatorPort;

import java.util.List;
import java.util.Map;

/**
 * Servicio de aplicacion: implementa el use case principal.
 *
 * <p>Orquesta los 4 puertos de salida (LLM, prompts, BD, validador) en el
 * orden correcto. Devuelve QueryResult siempre, nunca lanza excepciones
 * de runtime: los errores se envuelven en el campo `error` del resultado.
 *
 * <p>Regla: el SQL se devuelve en el resultado incluso si falla la validacion
 * o la ejecucion, para que la UI pueda mostrar al usuario que intentaba
 * generar el LLM.
 */
public class ProcessQuestionService implements ProcessQuestionUseCase {

    private final LlmPort llmPort;
    private final PromptPort promptPort;
    private final DatabasePort databasePort;
    private final SqlSafetyValidatorPort sqlSafetyValidator;

    public ProcessQuestionService(
        LlmPort llmPort,
        PromptPort promptPort,
        DatabasePort databasePort,
        SqlSafetyValidatorPort sqlSafetyValidator
    ) {
        this.llmPort = llmPort;
        this.promptPort = promptPort;
        this.databasePort = databasePort;
        this.sqlSafetyValidator = sqlSafetyValidator;
    }

    @Override
    public QueryResult process(Question question) {
        // Paso 1: obtener el system prompt del idioma.
        // Esto puede lanzar EmptyQuestionException si el idioma no es valido.
        String systemPrompt;
        try {
            systemPrompt = promptPort.getSystemPrompt(question.language());
        } catch (RuntimeException e) {
            return QueryResult.failure("", e.getMessage());
        }

        // Paso 2: generar SQL con el LLM.
        String sql;
        try {
            sql = llmPort.generate(systemPrompt, question.text());
        } catch (LlmException e) {
            return QueryResult.failure("", "Error generando SQL: " + e.getMessage());
        }

        // Paso 3: validar el SQL (defense in depth).
        try {
            sqlSafetyValidator.validate(sql);
        } catch (UnsafeSqlException e) {
            return QueryResult.failure(sql, "SQL no seguro: " + e.getMessage());
        }

        // Paso 4: ejecutar contra la BD.
        try {
            List<Map<String, Object>> rows = databasePort.executeQuery(sql);
            return QueryResult.success(sql, rows, extractColumns(rows));
        } catch (SqlExecutionException e) {
            return QueryResult.failure(sql, "Error de base de datos: " + e.getMessage());
        }
    }

    /** Extrae los nombres de columna en orden, a partir de la primera fila. */
    private List<String> extractColumns(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) {
            return List.of();
        }
        return List.copyOf(rows.get(0).keySet());
    }
}
