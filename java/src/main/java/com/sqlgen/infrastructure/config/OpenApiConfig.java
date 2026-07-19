package com.sqlgen.infrastructure.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Metadata para la documentacion OpenAPI/Swagger.
 * Accesible en /v3/api-docs (JSON) y /swagger-ui.html (UI).
 */
@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI sqlgenOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("AI SQL Query Generator API")
                .description("Backend Spring Boot del AI SQL Query Generator (Issue #2)")
                .version("0.1.0")
                .contact(new Contact()
                    .name("Alvaro Bozser")
                    .url("https://github.com/alvarobozser"))
                .license(new License()
                    .name("MIT")
                    .url("https://opensource.org/licenses/MIT")));
    }
}
