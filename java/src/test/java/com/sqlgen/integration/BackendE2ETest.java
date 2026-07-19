package com.sqlgen.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sqlgen.SqlgenApplication;
import com.sqlgen.infrastructure.web.dto.QueryResponse;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.TestPropertySource;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Test de integracion E2E del backend contra el Postgres real de docker-compose.
 *
 * <p>Este test arranca el contexto Spring completo y hace llamadas HTTP
 * reales a los endpoints.
 *
 * <p>Para test E2E con LLM real (que consume API), se hace en una clase
 * separada (BackendE2EWithLlmTest) que se skip si no hay API key real.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@TestPropertySource(properties = {
    "spring.datasource.url=jdbc:postgresql://localhost:5432/sqlgen",
    "spring.datasource.username=sqlgen",
    "spring.datasource.password=sqlgen_dev_password",
    "spring.datasource.readonly.url=jdbc:postgresql://localhost:5432/sqlgen",
    "spring.datasource.readonly.username=sqlgen_readonly",
    "spring.datasource.readonly.password=readonly_password"
})
class BackendE2ETest {

    @BeforeAll
    static void loadDotenv() {
        // Cargar .env para que ANTHROPIC_API_KEY este disponible en los tests.
        String[] paths = { ".env", "../.env", "../../.env" };
        for (String path : paths) {
            File file = new File(path);
            if (file.exists() && file.isFile()) {
                try {
                    for (String line : Files.readAllLines(file.toPath())) {
                        String trimmed = line.trim();
                        if (trimmed.isEmpty() || trimmed.startsWith("#")) continue;
                        int eqIdx = trimmed.indexOf('=');
                        if (eqIdx > 0) {
                            String key = trimmed.substring(0, eqIdx).trim();
                            String value = trimmed.substring(eqIdx + 1).trim();
                            if (value.startsWith("\"") && value.endsWith("\"")) {
                                value = value.substring(1, value.length() - 1);
                            }
                            if (System.getenv(key) == null && System.getProperty(key) == null) {
                                System.setProperty(key, value);
                            }
                        }
                    }
                } catch (IOException ignored) {}
                return;
            }
        }
    }

    @LocalServerPort
    private int port;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void health_check_devuelve_200() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/health", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("UP");
    }

    @Test
    void query_endpoint_con_pregunta_vacia_devuelve_400() {
        // RestTemplate que NO lanza excepcion en 4xx (asi podemos verificar el status).
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setErrorHandler(new org.springframework.web.client.DefaultResponseErrorHandler() {
            @Override
            public boolean hasError(org.springframework.http.client.ClientHttpResponse response) {
                return false;  // nunca tratar como error
            }
        });

        String body = "{\"question\":\"\",\"language\":\"es\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/queries",
            HttpMethod.POST,
            entity,
            String.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(400);
    }

    @Test
    void query_endpoint_con_api_key_real_genera_sql_y_ejecuta() {
        org.junit.jupiter.api.Assumptions.assumeTrue(
            isApiKeyReal(),
            "Requiere ANTHROPIC_API_KEY real (no placeholder) para este test"
        );

        RestTemplate restTemplate = new RestTemplate();
        String body = "{\"question\":\"Cuantos usuarios hay?\",\"language\":\"es\"}";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<String> entity = new HttpEntity<>(body, headers);

        ResponseEntity<QueryResponse> response = restTemplate.exchange(
            "http://localhost:" + port + "/api/v1/queries",
            HttpMethod.POST,
            entity,
            QueryResponse.class
        );

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        QueryResponse resp = response.getBody();
        assertThat(resp).isNotNull();
        assertThat(resp.error()).isNull();
        assertThat(resp.sql()).isNotEmpty();
        assertThat(resp.sql().toUpperCase()).contains("SELECT");
        assertThat(resp.rows()).isNotEmpty();
    }

    @Test
    void schema_endpoint_devuelve_tabla_users() {
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.getForEntity(
            "http://localhost:" + port + "/api/v1/schema", String.class);

        assertThat(response.getStatusCode().value()).isEqualTo(200);
        assertThat(response.getBody()).contains("users");
        assertThat(response.getBody()).contains("products");
        assertThat(response.getBody()).contains("orders");
        assertThat(response.getBody()).contains("order_items");
    }

    private boolean isApiKeyReal() {
        String key = System.getenv("ANTHROPIC_API_KEY");
        if (key == null) {
            // Tambien buscar en system properties (del .env loader)
            key = System.getProperty("ANTHROPIC_API_KEY");
        }
        return key != null && !key.isBlank() && !key.equals("your_key_here")
            && key.startsWith("sk-ant-");
    }
}
