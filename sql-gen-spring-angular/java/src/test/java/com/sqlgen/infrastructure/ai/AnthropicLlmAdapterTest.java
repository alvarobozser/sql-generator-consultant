package com.sqlgen.infrastructure.ai;

import com.sqlgen.domain.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Tests para {@link AnthropicLlmAdapter} con RestClient mockeado. */
@ExtendWith(MockitoExtension.class)
class AnthropicLlmAdapterTest {

    @Mock
    private RestClient restClient;

    private AnthropicLlmAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicLlmAdapter(restClient, "sk-ant-test-key", "claude-haiku-4-5");
    }

    /** Configura la cadena de mocks del RestClient fluent API para devolver un body. */
    private void mockResponse(String jsonBody) {
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(AnthropicLlmAdapter.AnthropicRequest.class)))
            .thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AnthropicLlmAdapter.AnthropicResponse.class))
            .thenReturn(parseResponse(jsonBody));
    }

    /** Parsea el JSON de respuesta a un AnthropicResponse. */
    private AnthropicLlmAdapter.AnthropicResponse parseResponse(String json) {
        try {
            com.fasterxml.jackson.databind.ObjectMapper mapper =
                new com.fasterxml.jackson.databind.ObjectMapper();
            return mapper.readValue(json, AnthropicLlmAdapter.AnthropicResponse.class);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    void happy_path_devuelve_sql_limpio() {
        mockResponse("{\"content\":[{\"type\":\"text\",\"text\":\"SELECT * FROM users\"}]}");

        String sql = adapter.generate("system prompt", "dame los usuarios");

        assertThat(sql).isEqualTo("SELECT * FROM users");
    }

    @Test
    void limpia_markdown_sql_del_LLM() {
        mockResponse("{\"content\":[{\"type\":\"text\",\"text\":\"```sql\\nSELECT * FROM products\\n```\"}]}");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT * FROM products");
    }

    @Test
    void quita_punto_y_coma_final() {
        mockResponse("{\"content\":[{\"type\":\"text\",\"text\":\"SELECT COUNT(*) FROM users;\"}]}");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT COUNT(*) FROM users");
    }

    @Test
    void quita_espacios_al_inicio_y_final() {
        mockResponse("{\"content\":[{\"type\":\"text\",\"text\":\"   \\n  SELECT 1  \\n  \"}]}");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT 1");
    }

    @Test
    void respuesta_vacia_lanza_LlmException() {
        mockResponse("{\"content\":[{\"type\":\"text\",\"text\":\"\"}]}");

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("vacia");
    }

    @Test
    void respuesta_solo_markdown_vacio_lanza_LlmException() {
        mockResponse("{\"content\":[{\"type\":\"text\",\"text\":\"```\\n\\n```\"}]}");

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("vacia");
    }

    @Test
    void api_lanza_excepcion_envuelve_en_LlmException() {
        RestClient.RequestBodySpec requestBodySpec = mock(RestClient.RequestBodySpec.class);
        RestClient.RequestBodyUriSpec requestBodyUriSpec = mock(RestClient.RequestBodyUriSpec.class);
        RestClient.ResponseSpec responseSpec = mock(RestClient.ResponseSpec.class);

        when(restClient.post()).thenReturn(requestBodyUriSpec);
        when(requestBodyUriSpec.body(any(AnthropicLlmAdapter.AnthropicRequest.class)))
            .thenReturn(requestBodySpec);
        when(requestBodySpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.body(AnthropicLlmAdapter.AnthropicResponse.class))
            .thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("Error llamando a la API")
            .hasMessageContaining("network down");
    }

    @Test
    void systemPrompt_vacio_lanza_IllegalArgumentException() {
        assertThatThrownBy(() -> adapter.generate("", "q"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void userPrompt_vacio_lanza_IllegalArgumentException() {
        assertThatThrownBy(() -> adapter.generate("sys", ""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void systemPrompt_null_lanza_IllegalArgumentException() {
        assertThatThrownBy(() -> adapter.generate(null, "q"))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void constructor_con_api_key_placeholder_lanza_IllegalStateException() {
        assertThatThrownBy(() -> new AnthropicLlmAdapter(restClient, "your_key_here", "claude"))
            .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void constructor_con_api_key_vacia_lanza_IllegalStateException() {
        assertThatThrownBy(() -> new AnthropicLlmAdapter(restClient, "", "claude"))
            .isInstanceOf(IllegalStateException.class);
    }
}
