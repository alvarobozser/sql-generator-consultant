package com.sqlgen.domain;

import com.sqlgen.domain.exception.EmptyQuestionException;

import java.util.Locale;
import java.util.Map;

/**
 * Idiomas soportados por la aplicacion. Se mapea directamente a los codigos
 * de dos letras usados en la API REST y en los archivos de prompts.
 */
public enum Language {
    ES("es"),
    EN("en");

    private static final Map<String, Language> BY_CODE = Map.of(
        "es", ES,
        "en", EN
    );

    private final String code;

    Language(String code) {
        this.code = code;
    }

    public String code() {
        return code;
    }

    /**
     * Convierte un codigo de dos letras a un enum Language.
     * Lanza EmptyQuestionException si el codigo no es valido (se reutiliza
     * porque semanticamente la entrada es "vacia" en terminos del sistema).
     */
    public static Language fromCode(String code) {
        if (code == null) {
            throw new EmptyQuestionException("El codigo de idioma no puede ser null");
        }
        Language lang = BY_CODE.get(code.toLowerCase(Locale.ROOT));
        if (lang == null) {
            throw new EmptyQuestionException(
                "Idioma no soportado: '" + code + "'. Idiomas validos: " + BY_CODE.keySet()
            );
        }
        return lang;
    }
}
