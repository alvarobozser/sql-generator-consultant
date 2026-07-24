package com.sqlgen.domain;

import com.sqlgen.domain.exception.EmptyQuestionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests para {@link Language}. */
class LanguageTest {

    @Test
    void fromCode_es() {
        assertThat(Language.fromCode("es")).isEqualTo(Language.ES);
    }

    @Test
    void fromCode_en() {
        assertThat(Language.fromCode("en")).isEqualTo(Language.EN);
    }

    @Test
    void fromCode_case_insensitive() {
        assertThat(Language.fromCode("ES")).isEqualTo(Language.ES);
        assertThat(Language.fromCode("En")).isEqualTo(Language.EN);
    }

    @Test
    void fromCode_rechaza_null() {
        assertThrows(EmptyQuestionException.class, () -> Language.fromCode(null));
    }

    @Test
    void fromCode_rechaza_idioma_no_soportado() {
        var ex = assertThrows(EmptyQuestionException.class, () -> Language.fromCode("fr"));
        assertThat(ex.getMessage()).contains("fr");
    }
}
