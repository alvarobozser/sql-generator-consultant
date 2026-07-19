package com.sqlgen.application.service;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.exception.SqlExecutionException;
import com.sqlgen.domain.exception.UnsafeSqlException;
import com.sqlgen.domain.model.Question;
import com.sqlgen.domain.model.QueryResult;
import com.sqlgen.domain.port.out.DatabasePort;
import com.sqlgen.domain.port.out.LlmPort;
import com.sqlgen.domain.port.out.PromptPort;
import com.sqlgen.domain.port.out.SqlSafetyValidatorPort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests para {@link ProcessQuestionService} con Mockito. */
@ExtendWith(MockitoExtension.class)
class ProcessQuestionServiceTest {

    @Mock
    private LlmPort llmPort;

    @Mock
    private PromptPort promptPort;

    @Mock
    private DatabasePort databasePort;

    @Mock
    private SqlSafetyValidatorPort sqlSafetyValidator;

    @InjectMocks
    private ProcessQuestionService service;

    private Question question;

    @BeforeEach
    void setUp() {
        question = new Question("Cuantos usuarios hay?", Language.ES);
    }

    @Test
    void happy_path_devuelve_queryresult_exitoso() {
        when(promptPort.getSystemPrompt(Language.ES)).thenReturn("system prompt ES");
        when(llmPort.generate(anyString(), anyString())).thenReturn("SELECT COUNT(*) FROM users");
        doNothing().when(sqlSafetyValidator).validate(anyString());
        when(databasePort.executeQuery(anyString())).thenReturn(
            List.<Map<String, Object>>of(Map.of("count", 20L))
        );

        QueryResult result = service.process(question);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.sql()).isEqualTo("SELECT COUNT(*) FROM users");
        assertThat(result.rows()).hasSize(1);
        assertThat(result.columns()).containsExactly("count");
        assertThat(result.error()).isNull();
    }

    @Test
    void promptPort_lanza_excepcion_devuelve_failure_con_error() {
        when(promptPort.getSystemPrompt(Language.ES))
            .thenThrow(new IllegalArgumentException("Idioma no soportado: fr"));

        QueryResult result = service.process(question);

        assertThat(result.isError()).isTrue();
        assertThat(result.error()).contains("Idioma no soportado");
        assertThat(result.sql()).isEmpty();
        verify(llmPort, never()).generate(anyString(), anyString());
    }

    @Test
    void llm_lanza_excepcion_devuelve_failure_sin_sql() {
        when(promptPort.getSystemPrompt(Language.ES)).thenReturn("system");
        when(llmPort.generate(anyString(), anyString()))
            .thenThrow(new LlmException("API key invalida"));

        QueryResult result = service.process(question);

        assertThat(result.isError()).isTrue();
        assertThat(result.error()).contains("Error generando SQL");
        assertThat(result.error()).contains("API key invalida");
        assertThat(result.sql()).isEmpty();
        verify(databasePort, never()).executeQuery(anyString());
    }

    @Test
    void validador_rechaza_sql_devuelve_failure_con_sql_visible() {
        when(promptPort.getSystemPrompt(Language.ES)).thenReturn("system");
        when(llmPort.generate(anyString(), anyString())).thenReturn("DROP TABLE users");
        doThrow(new UnsafeSqlException("El SQL contiene la keyword prohibida: DROP"))
            .when(sqlSafetyValidator).validate("DROP TABLE users");

        QueryResult result = service.process(question);

        assertThat(result.isError()).isTrue();
        assertThat(result.error()).contains("SQL no seguro");
        // Importante: el SQL se devuelve aunque falle, para que la UI lo muestre.
        assertThat(result.sql()).isEqualTo("DROP TABLE users");
        verify(databasePort, never()).executeQuery(anyString());
    }

    @Test
    void database_lanza_excepcion_devuelve_failure_con_sql_visible() {
        when(promptPort.getSystemPrompt(Language.ES)).thenReturn("system");
        when(llmPort.generate(anyString(), anyString())).thenReturn("SELECT * FROM x");
        doNothing().when(sqlSafetyValidator).validate(anyString());
        when(databasePort.executeQuery("SELECT * FROM x"))
            .thenThrow(new SqlExecutionException("table 'x' does not exist"));

        QueryResult result = service.process(question);

        assertThat(result.isError()).isTrue();
        assertThat(result.error()).contains("Error de base de datos");
        assertThat(result.error()).contains("table 'x' does not exist");
        assertThat(result.sql()).isEqualTo("SELECT * FROM x");
    }

    @Test
    void query_sin_resultados_devuelve_success_con_listas_vacias() {
        when(promptPort.getSystemPrompt(Language.ES)).thenReturn("system");
        when(llmPort.generate(anyString(), anyString())).thenReturn("SELECT 1");
        doNothing().when(sqlSafetyValidator).validate(anyString());
        when(databasePort.executeQuery(anyString())).thenReturn(List.of());

        QueryResult result = service.process(question);

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.rows()).isEmpty();
        assertThat(result.columns()).isEmpty();
    }

    @Test
    void language_en_se_pasa_al_prompt_port() {
        Question enQ = new Question("How many users?", Language.EN);
        when(promptPort.getSystemPrompt(Language.EN)).thenReturn("system EN");
        when(llmPort.generate(anyString(), anyString())).thenReturn("SELECT 1");
        doNothing().when(sqlSafetyValidator).validate(anyString());
        when(databasePort.executeQuery(anyString())).thenReturn(List.<Map<String, Object>>of(Map.of("x", 1)));

        service.process(enQ);

        verify(promptPort, times(1)).getSystemPrompt(Language.EN);
        verify(promptPort, never()).getSystemPrompt(Language.ES);
    }
}
