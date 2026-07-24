package com.sqlgen.infrastructure.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlgen.application.dto.QuestionRequest;
import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.port.in.ProcessQuestionUseCase;
import com.sqlgen.domain.model.QueryResult;
import com.sqlgen.domain.model.Question;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Tests del QueryController con @WebMvcTest.
 * Mockeamos ProcessQuestionUseCase para aislar el controller.
 */
@WebMvcTest(controllers = QueryController.class)
@Import(GlobalExceptionHandler.class)
class QueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProcessQuestionUseCase processQuestionUseCase;

    @Test
    void post_query_exitosa_devuelve_200_con_sql_y_rows() throws Exception {
        // Arrange: el use case devuelve un resultado exitoso
        when(processQuestionUseCase.process(any(Question.class))).thenReturn(
            QueryResult.success(
                "SELECT COUNT(*) FROM users",
                List.<Map<String, Object>>of(Map.of("count", 20L)),
                List.of("count")
            )
        );

        String body = objectMapper.writeValueAsString(Map.of(
            "question", "Cuantos usuarios hay?",
            "language", "es"
        ));

        mockMvc.perform(post("/api/v1/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.sql").value("SELECT COUNT(*) FROM users"))
            .andExpect(jsonPath("$.rows[0].count").value(20))
            .andExpect(jsonPath("$.columns[0]").value("count"))
            .andExpect(jsonPath("$.error").doesNotExist());
    }

    @Test
    void post_query_con_error_del_LLM_devuelve_502() throws Exception {
        when(processQuestionUseCase.process(any(Question.class)))
            .thenThrow(new LlmException("API key invalida"));

        String body = objectMapper.writeValueAsString(Map.of(
            "question", "test",
            "language", "es"
        ));

        mockMvc.perform(post("/api/v1/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadGateway())
            .andExpect(jsonPath("$.error").value(org.hamcrest.Matchers.containsString("API key invalida")));
    }

    @Test
    void post_con_question_vacia_devuelve_400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "question", "",
            "language", "es"
        ));

        mockMvc.perform(post("/api/v1/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void post_con_language_invalido_devuelve_400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
            "question", "test",
            "language", "fr"
        ));

        mockMvc.perform(post("/api/v1/queries")
                .contentType(MediaType.APPLICATION_JSON)
                .content(body))
            .andExpect(status().isBadRequest());
    }

    @Test
    void post_sin_body_devuelve_400() throws Exception {
        mockMvc.perform(post("/api/v1/queries")
                .contentType(MediaType.APPLICATION_JSON))
            .andExpect(status().isBadRequest());
    }
}
