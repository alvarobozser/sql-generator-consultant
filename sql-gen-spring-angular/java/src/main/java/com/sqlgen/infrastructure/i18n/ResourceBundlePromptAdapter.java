package com.sqlgen.infrastructure.i18n;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.EmptyQuestionException;
import com.sqlgen.domain.port.out.PromptPort;
import org.springframework.stereotype.Component;

import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Adaptador de prompts que carga los textos desde archivos properties.
 *
 * <p>Usa el ResourceBundle de Java (mecanismo estandar de i18n). Los archivos
 * properties estan en src/main/resources/:
 * - prompts_es.properties (espanol)
 * - prompts_en.properties (ingles)
 *
 * <p>Si el idioma no esta soportado, lanza EmptyQuestionException.
 */
@Component
public class ResourceBundlePromptAdapter implements PromptPort {

    /** Base name de los archivos properties (sin locale ni extension). */
    private static final String BUNDLE_BASE_NAME = "prompts";

    @Override
    public String getSystemPrompt(Language language) {
        if (language == null) {
            throw new EmptyQuestionException("El idioma no puede ser null");
        }
        Locale locale = toLocale(language);
        try {
            return ResourceBundle.getBundle(BUNDLE_BASE_NAME, locale).getString("system");
        } catch (Exception e) {
            throw new EmptyQuestionException(
                "Idioma no soportado: '" + language.code() + "'. Error: " + e.getMessage()
            );
        }
    }

    /** Convierte nuestro enum Language a un java.util.Locale. */
    private Locale toLocale(Language language) {
        return new Locale(language.code());
    }
}
