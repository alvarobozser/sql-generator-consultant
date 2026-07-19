package com.sqlgen.infrastructure.ai;

import com.sqlgen.domain.exception.LlmException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.client.ChatClient;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** Tests para {@link AnthropicLlmAdapter} con Mockito. */
@ExtendWith(MockitoExtension.class)
class AnthropicLlmAdapterTest {

    @Mock
    private ChatClient chatClient;

    @Mock
    private ChatClient.ChatClientRequestSpec requestSpec;

    @Mock
    private ChatClient.CallResponseSpec callResponseSpec;

    private AnthropicLlmAdapter adapter;

    @BeforeEach
    void setUp() {
        adapter = new AnthropicLlmAdapter(chatClient);
    }

    /**
     * Helper para encadenar los mocks del ChatClient fluent API.
     * Cadena real: prompt() -> ChatClientRequestSpec -> system() -> user() -> call() -> CallResponseSpec -> content()
     * Nota: system() y user() devuelven el mismo ChatClientRequestSpec (cadena).
     */
    private void mockChatClientChain(String content) {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenReturn(callResponseSpec);
        when(callResponseSpec.content()).thenReturn(content);
    }

    @Test
    void happy_path_devuelve_sql_limpio() {
        mockChatClientChain("SELECT * FROM users");

        String sql = adapter.generate("system prompt", "dame los usuarios");

        assertThat(sql).isEqualTo("SELECT * FROM users");
        verify(chatClient, times(1)).prompt();
        verify(requestSpec).system("system prompt");
        verify(requestSpec).user("dame los usuarios");
    }

    @Test
    void limpia_markdown_sql_del_LLM() {
        mockChatClientChain("```sql\nSELECT * FROM products\n```");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT * FROM products");
    }

    @Test
    void limpia_markdown_sin_lenguaje_sql() {
        mockChatClientChain("```\nSELECT 1\n```");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT 1");
    }

    @Test
    void quita_punto_y_coma_final() {
        mockChatClientChain("SELECT COUNT(*) FROM users;");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT COUNT(*) FROM users");
    }

    @Test
    void quita_espacios_al_inicio_y_final() {
        mockChatClientChain("   \n  SELECT 1  \n  ");

        String sql = adapter.generate("sys", "q");

        assertThat(sql).isEqualTo("SELECT 1");
    }

    @Test
    void api_lanza_excepcion_envuelve_en_LlmException() {
        when(chatClient.prompt()).thenReturn(requestSpec);
        when(requestSpec.system(anyString())).thenReturn(requestSpec);
        when(requestSpec.user(anyString())).thenReturn(requestSpec);
        when(requestSpec.call()).thenThrow(new RuntimeException("network down"));

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("Error llamando a la API")
            .hasMessageContaining("network down");
    }

    @Test
    void respuesta_null_lanza_LlmException() {
        mockChatClientChain(null);

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("null");
    }

    @Test
    void respuesta_vacia_o_solo_espacios_lanza_LlmException() {
        mockChatClientChain("   \n  \t  ");

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("vacia");
    }

    @Test
    void respuesta_solo_markdown_vacio_lanza_LlmException() {
        mockChatClientChain("```\n\n```");

        assertThatThrownBy(() -> adapter.generate("sys", "q"))
            .isInstanceOf(LlmException.class)
            .hasMessageContaining("vacia");
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
}
