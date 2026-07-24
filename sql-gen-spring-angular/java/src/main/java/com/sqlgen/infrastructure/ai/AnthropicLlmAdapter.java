package com.sqlgen.infrastructure.ai;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.sqlgen.domain.exception.LlmException;
import com.sqlgen.domain.port.out.LlmPort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Adaptador que llama a la API de Anthropic directamente via HTTP.
 *
 * <p>Por que NO usamos Spring AI: Spring AI 1.x tiene un bug con Anthropic
 * (envia Authorization: Bearer en vez de x-api-key, lo que produce 401).
 *
 * <p>Por que NO usamos el SDK oficial: el SDK de Java no esta en Maven Central,
 * requiere repo custom. HTTP directo es mas simple y mantenible.
 *
 * <p>Endpoint: https://api.anthropic.com/v1/messages
 * Headers: x-api-key, anthropic-version: 2023-06-01, content-type: application/json
 */
@Component
public class AnthropicLlmAdapter implements LlmPort {

    private static final Logger log = LoggerFactory.getLogger(AnthropicLlmAdapter.class);
    private static final String ANTHROPIC_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    /** Regex para extraer el contenido de un bloque markdown. */
    private static final Pattern MARKDOWN_CODE_BLOCK = Pattern.compile(
        "```(?:sql)?\\s*(.*?)\\s*```",
        Pattern.DOTALL | Pattern.CASE_INSENSITIVE
    );

    private final RestClient restClient;
    private final String model;

    /**
     * Constructor principal (usado por Spring): recibe api key y modelo, crea su RestClient.
     */
    @org.springframework.beans.factory.annotation.Autowired
    public AnthropicLlmAdapter(
        @Value("${anthropic.api-key}") String apiKey,
        @Value("${llm.model:claude-haiku-4-5}") String model
    ) {
        this(buildDefaultClient(apiKey), apiKey, model);
    }

    /**
     * Constructor package-private para testing: recibe un RestClient ya configurado.
     */
    AnthropicLlmAdapter(RestClient restClient, String apiKey, String model) {
        if (apiKey == null || apiKey.isBlank() || "your_key_here".equals(apiKey)) {
            throw new IllegalStateException(
                "ANTHROPIC_API_KEY no esta configurada. Configurala en .env o como variable de entorno."
            );
        }
        this.restClient = restClient;
        this.model = model;
    }

    private static RestClient buildDefaultClient(String apiKey) {
        return RestClient.builder()
            .baseUrl(ANTHROPIC_API_URL)
            .defaultHeader("x-api-key", apiKey)
            .defaultHeader("anthropic-version", ANTHROPIC_VERSION)
            .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
            .build();
    }

    @Override
    public String generate(String systemPrompt, String userPrompt) {
        if (systemPrompt == null || systemPrompt.isBlank()) {
            throw new IllegalArgumentException("systemPrompt no puede estar vacio");
        }
        if (userPrompt == null || userPrompt.isBlank()) {
            throw new IllegalArgumentException("userPrompt no puede estar vacio");
        }

        // Construir el body del request.
        AnthropicRequest request = new AnthropicRequest(
            model,
            1024,
            systemPrompt,
            List.of(new AnthropicMessage("user", userPrompt)),
            0.0
        );

        try {
            AnthropicResponse response = restClient.post()
                .body(request)
                .retrieve()
                .body(AnthropicResponse.class);

            if (response == null || response.content() == null || response.content().isEmpty()) {
                throw new LlmException("La API devolvio una respuesta vacia");
            }

            String rawContent = response.content().get(0).text();
            log.debug("LLM raw response: {}", rawContent);

            String cleaned = cleanSqlResponse(rawContent);
            if (cleaned.isEmpty()) {
                throw new LlmException("La API devolvio una respuesta vacia");
            }
            return cleaned;

        } catch (RestClientResponseException e) {
            // 4xx/5xx de la API
            String body = e.getResponseBodyAsString();
            log.error("Error HTTP de Anthropic: {} - {}", e.getStatusCode(), body);
            throw new LlmException(
                "Error HTTP " + e.getStatusCode() + " de Anthropic: " + body, e
            );
        } catch (LlmException e) {
            throw e;
        } catch (Exception e) {
            log.error("Error llamando a la API de Anthropic", e);
            throw new LlmException("Error llamando a la API: " + e.getMessage(), e);
        }
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

    // --- DTOs internos ---

    @JsonInclude(JsonInclude.Include.NON_NULL)
    public record AnthropicRequest(
        String model,
        @JsonProperty("max_tokens") int maxTokens,
        String system,
        List<AnthropicMessage> messages,
        double temperature
    ) {}

    public record AnthropicMessage(String role, String content) {}

    public record AnthropicResponse(
        String id,
        String type,
        String role,
        List<AnthropicContent> content,
        String model,
        @JsonProperty("stop_reason") String stopReason
    ) {}

    public record AnthropicContent(String type, String text) {}
}
