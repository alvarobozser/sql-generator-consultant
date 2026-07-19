package com.sqlgen.infrastructure.ai;

import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.port.out.LlmPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador que envuelve Spring AI ChatClient para implementar LlmPort.
 *
 * <p>Spring AI es la abstraccion oficial de Spring para LLMs. Soporta Anthropic,
 * OpenAI, Azure, Ollama, etc. con la misma API. Cambiar de proveedor es solo
 * cambiar la dependencia y la config en application.yml.
 *
 * <p>El adaptador limpia la respuesta del LLM:
 * - Quita bloques markdown (```sql ... ```)
 * - Quita ';' final
 * - Quita espacios extra
 *
 * <p>Si la respuesta viene vacia, lanza LlmException.
 */
@Component
public class AnthropicLlmAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmAdapter.class);

    /** Regex para extraer el contenido de un bloque markdown. */
    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile(
        "```(?:sql)?\\s*(.*?)\\s*```",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    private final ChatClient chatClient;

    public AnthropicLlmAdapter(ChatClient chatClient) {
        this.chatClient = chatClient;
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt no puede estar vacio");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt no puede estar vacio");
        }

        String rawContent;
        try {
            rawContent = chatClient.prompt()
                .system(systemPrompt)
                .user(userPrompt)
                .call()
                .content();
        } catch (Exception e) {
            // Envolvemos cualquier error de la API (red, auth, rate limit, etc.)
            // en LlmException con mensaje claro, sin exponer el stack trace crudo.
            log.error("Error llamando a la API de Anthropic", e);
            throw new LlmException("Error llamando a la API: " + e.getMessage(), e);
        }

        if (rawContent == null) {
            throw new LlmException("La API devolvio una respuesta null");
        }

        String cleaned = cleanSqlResponse(rawContent);
        if (cleaned.isEmpty()) {
            throw new LlmException("La API devolvio una respuesta vacia");
        }

        return cleaned;
    }

    /**
     * Limpia la respuesta del LLM para quedarse solo con el SQL.
     * - Quita bloques markdown ```sql ... ```
     * - Quita ';' final
     * - Quita espacios al inicio y final
     */
    static String cleanSqlResponse(String raw) {
        if (raw == null || raw.isBlank()) {
            return "";
        }
        String text = raw.strip();

        Matcher matcher = MARKDOWN_CODE_BLOCK.matcher(text);
        if (matcher.find()) {
            text = matcher.group(1).strip();
        }

        if (text.endsWith(";")) {
            text = text.substring(0, text.length() - 1).strip();
        }

        return text;
    }
}
