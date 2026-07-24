package com.sqlgen.application.dto;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.EmptyQuestionException;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada del use case ProcessQuestion.
 * Validado por Bean Validation antes de construir la Question de dominio.
 */
public record QuestionRequest(
    @NotBlank(message = "La pregunta no puede estar vacia")
    @Size(max = 2000, message = "La pregunta es demasiado larga (max 2000 caracteres)")
    String question,

    @NotBlank(message = "El idioma no puede estar vacio")
    @Pattern(regexp = "es|en", message = "El idioma debe ser 'es' o 'en'")
    String language
) {

    /** Convierte este DTO en un objeto de dominio. */
    public com.sqlgen.domain.model.Question toDomain() {
        if (question == null || language == null) {
            throw new EmptyQuestionException("Question o language son null");
        }
        return new com.sqlgen.domain.model.Question(
            question.trim(),
            Language.fromCode(language.trim())
        );
    }
}
