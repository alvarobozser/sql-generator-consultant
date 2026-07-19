package com.sqlgen.domain.model;

import com.sqlgen.domain.Language;
import com.sqlgen.domain.exception.EmptyQuestionException;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** Tests para {@link Question}. */
class QuestionTest {

    @Test
    void crea_question_valida() {
        var q = new Question("Dame los usuarios", Language.ES);

        assertThat(q.text()).isEqualTo("Dame los usuarios");
        assertThat(q.language()).isEqualTo(Language.ES);
    }

    @Test
    void falla_con_texto_vacio() {
        assertThrows(EmptyQuestionException.class, () -> new Question("", Language.ES));
    }

    @Test
    void falla_con_texto_null() {
        assertThrows(EmptyQuestionException.class, () -> new Question(null, Language.ES));
    }

    @Test
    void falla_con_solo_espacios() {
        assertThrows(EmptyQuestionException.class, () -> new Question("   \n\t  ", Language.ES));
    }

    @Test
    void falla_con_idioma_null() {
        assertThrows(EmptyQuestionException.class, () -> new Question("Test", null));
    }
}
