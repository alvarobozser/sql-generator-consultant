package com.sqlgen.infrastructure.i18n;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.EmptyQuestionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests para {@link ResourceBundlePromptAdapter}. */
class ResourceBundlePromptAdapterTest {

    private final ResourceBundlePromptAdapter adapter = new ResourceBundlePromptAdapter();

    @Test
    void devuelve_prompt_en_espanol() {
        String prompt = adapter.getSystemPrompt(Language.ES);

        assertThat(prompt).isNotBlank();
        // Palabras clave del prompt en espanol.
        assertThat(prompt).contains("Eres un asistente");
        assertThat(prompt).contains("SELECT");
        assertThat(prompt).contains("REGLAS ESTRICTAS");
    }

    @Test
    void devuelve_prompt_en_ingles() {
        String prompt = adapter.getSystemPrompt(Language.EN);

        assertThat(prompt).isNotBlank();
        assertThat(prompt).contains("expert SQL assistant");
        assertThat(prompt).contains("SELECT");
        assertThat(prompt).contains("STRICT RULES");
    }

    @Test
    void prompt_en_espanol_contiene_las_4_tablas() {
        String prompt = adapter.getSystemPrompt(Language.ES);

        assertThat(prompt).contains("users");
        assertThat(prompt).contains("products");
        assertThat(prompt).contains("orders");
        assertThat(prompt).contains("order_items");
    }

    @Test
    void prompt_en_ingles_contiene_las_4_tablas() {
        String prompt = adapter.getSystemPrompt(Language.EN);

        assertThat(prompt).contains("users");
        assertThat(prompt).contains("products");
        assertThat(prompt).contains("orders");
        assertThat(prompt).contains("order_items");
    }

    @Test
    void prompt_tiene_ejemplos_few_shot() {
        String es = adapter.getSystemPrompt(Language.ES);
        String en = adapter.getSystemPrompt(Language.EN);

        // Cada idioma tiene al menos 2 ejemplos (Pregunta/Question + SQL).
        assertThat(es.split("Pregunta:").length - 1).isGreaterThanOrEqualTo(2);
        assertThat(en.split("Question:").length - 1).isGreaterThanOrEqualTo(2);
    }

    @Test
    void idioma_null_lanza_EmptyQuestionException() {
        assertThatThrownBy(() -> adapter.getSystemPrompt(null))
            .isInstanceOf(EmptyQuestionException.class);
    }

    @Test
    void mismo_idioma_devuelve_prompts_equivalentes_en_tamano() {
        // Sanity check: ambos prompts son sustanciosos (>= 500 chars).
        String es = adapter.getSystemPrompt(Language.ES);
        String en = adapter.getSystemPrompt(Language.EN);

        assertThat(es.length()).isGreaterThan(500);
        assertThat(en.length()).isGreaterThan(500);
    }
}
