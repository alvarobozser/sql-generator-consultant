package com.sqlgen.infrastructure.config;

import com.sqlgen.application.service.ProcessQuestionService;
import com.sqlgen.domain.port.in.ProcessQuestionUseCase;
import com.sqlgen.domain.port.out.DatabasePort;
import com.sqlgen.domain.port.out.LlmPort;
import com.sqlgen.domain.port.out.PromptPort;
import com.sqlgen.domain.port.out.SqlSafetyValidatorPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de beans de la capa de aplicacion.
 *
 * <p>La arquitectura hexagonal estricta no usa @Service en la capa de aplicacion
 * (ProcessQuestionService es Java puro). Aqui es donde se "cablea" con sus
 * dependencias para que Spring lo instancie.
 */
@Configuration
public class ApplicationBeansConfig {

    @Bean
    public ProcessQuestionUseCase processQuestionUseCase(
        LlmPort llmPort,
        PromptPort promptPort,
        DatabasePort databasePort,
        SqlSafetyValidatorPort sqlSafetyValidator
    ) {
        return new ProcessQuestionService(llmPort, promptPort, databasePort, sqlSafetyValidator);
    }
}
