package com.sqlgen.infrastructure.web.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO de entrada del endpoint POST /api/v1/queries.
 * Duplicado del de aplicacion para evitar acoplamiento de paquetes.
 */
public record QueryRequest(
    @NotBlank(message = "La pregunta no puede estar vacia")
    @Size(max = 2000, message = "La pregunta es demasiado larga (max 2000 caracteres)")
    String question,

    @NotBlank(message = "El idioma no puede estar vacio")
    @Pattern(regexp = "es|en", message = "El idioma debe ser 'es' o 'en'")
    String language
) {}
