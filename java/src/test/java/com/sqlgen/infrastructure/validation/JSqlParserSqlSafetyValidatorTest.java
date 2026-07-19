package com.sqlgen.infrastructure.validation;

import com.sqlgen.domain.exception.UnsafeSqlException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Tests para {@link JSqlParserSqlSafetyValidator}. */
class JSqlParserSqlSafetyValidatorTest {

    private final JSqlParserSqlSafetyValidator validator = new JSqlParserSqlSafetyValidator();

    @Nested
    @DisplayName("Queries validas (deben pasar)")
    class ValidQueries {

        @ParameterizedTest
        @ValueSource(strings = {
            "SELECT * FROM users",
            "SELECT id, name FROM users WHERE country = 'Spain'",
            "SELECT count(*) FROM orders WHERE status = 'paid'",
            "SELECT * FROM products ORDER BY price DESC LIMIT 10",
            "WITH active_users AS (SELECT * FROM users WHERE country = 'Spain') "
                + "SELECT * FROM active_users",
            "EXPLAIN SELECT * FROM products",
            "select id from users",  // lowercase
            "  SELECT   *   FROM   users  "  // espacios extra
        })
        void acepta_queries_de_lectura_validas(String sql) {
            assertThatCode(() -> validator.validate(sql)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Keywords prohibidas (deben ser bloqueadas)")
    class ForbiddenKeywords {

        @ParameterizedTest
        @ValueSource(strings = {
            "DROP TABLE users",
            "DROP DATABASE sqlgen",
            "DELETE FROM users",
            "DELETE FROM users WHERE id = 1",
            "INSERT INTO users (name) VALUES ('hacker')",
            "UPDATE users SET name = 'hacker'",
            "ALTER TABLE users ADD COLUMN hack TEXT",
            "TRUNCATE TABLE users",
            "CREATE TABLE hack (id INT)",
            "GRANT ALL ON users TO public",
            "REVOKE ALL ON users FROM public"
        })
        void bloquea_keywords_destructivas(String sql) {
            // Todas estas queries DEBEN ser rechazadas con UnsafeSqlException.
            // El mensaje exacto puede variar ("prohibida" si el parser las reconoce,
            // o "no parseable" si el parser no las entiende) pero el rechazo es lo que importa.
            assertThatThrownBy(() -> validator.validate(sql))
                .isInstanceOf(UnsafeSqlException.class);
        }
    }

    @Nested
    @DisplayName("Multi-statement y comentarios")
    class MultipleStatementsAndComments {

        @Test
        void bloquea_multiples_statements_con_punto_y_coma() {
            assertThatThrownBy(() -> validator.validate("SELECT 1; DROP TABLE users"))
                .isInstanceOf(UnsafeSqlException.class)
                .hasMessageContaining("un statement");
        }

        @Test
        void bloquea_comentario_de_linea() {
            assertThatThrownBy(() -> validator.validate("SELECT 1 -- esto es un comentario"))
                .isInstanceOf(UnsafeSqlException.class)
                .hasMessageContaining("comentarios");
        }

        @Test
        void bloquea_comentario_multilinea() {
            assertThatThrownBy(() -> validator.validate("SELECT 1 /* DROP TABLE users */"))
                .isInstanceOf(UnsafeSqlException.class)
                .hasMessageContaining("comentarios");
        }

        @Test
        void bloquea_keyword_prohibida_en_string() {
            // Defense in depth: incluso en string, se bloquea.
            assertThatThrownBy(() -> validator.validate("SELECT 'DROP TABLE users' AS warning"))
                .isInstanceOf(UnsafeSqlException.class);
        }

        @Test
        void bloquea_keyword_en_subquery_delete() {
            assertThatThrownBy(() -> validator.validate(
                "SELECT * FROM (DELETE FROM users RETURNING *) AS t"))
                .isInstanceOf(UnsafeSqlException.class);
        }
    }

    @Nested
    @DisplayName("Casos limite (no deben bloquear nombres de columna)")
    class EdgeCases {

        @Test
        void no_bloquea_columna_con_nombre_parecido_a_update() {
            // updated_at no debe ser bloqueado (es nombre de columna).
            assertThatCode(() -> validator.validate("SELECT updated_at FROM orders"))
                .doesNotThrowAnyException();
        }

        @Test
        void no_bloquea_columna_con_nombre_parecido_a_delete() {
            // created_at, deleted_at, etc. tampoco.
            assertThatCode(() -> validator.validate("SELECT created_at FROM users"))
                .doesNotThrowAnyException();
        }

        @Test
        void no_bloquea_columna_con_nombre_parecido_a_set() {
            // settings, settled, etc.
            assertThatCode(() -> validator.validate("SELECT settings FROM users"))
                .doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("Validacion de input")
    class InputValidation {

        @Test
        void falla_con_null() {
            assertThatThrownBy(() -> validator.validate(null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacio");
        }

        @Test
        void falla_con_string_vacio() {
            assertThatThrownBy(() -> validator.validate(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacio");
        }

        @Test
        void falla_con_solo_espacios() {
            assertThatThrownBy(() -> validator.validate("   \n  \t  "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("vacio");
        }

        @Test
        void falla_con_sql_no_parseable() {
            assertThatThrownBy(() -> validator.validate("SELECT FROM WHERE"))
                .isInstanceOf(UnsafeSqlException.class)
                .hasMessageContaining("parseable");
        }
    }

    @Nested
    @DisplayName("Cobertura del blacklist")
    class BlacklistCoverage {

        @Test
        void las_8_keywords_criticas_estan_en_la_lista() {
            // Sanity check: el blacklist incluye las keywords criticas de SQL.
            String[] critical = {"INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE"};
            String allKeywords = String.join(",", validator.getClass().getDeclaredFields()[0].getName());
            // Verificamos que la lista FORBIDDEN_KEYWORDS incluye todas las criticas.
            // (Acceso via reflection: el campo private static final String[] FORBIDDEN_KEYWORDS)
            try {
                java.lang.reflect.Field field = JSqlParserSqlSafetyValidator.class
                    .getDeclaredField("FORBIDDEN_KEYWORDS");
                field.setAccessible(true);
                String[] values = (String[]) field.get(null);
                for (String kw : critical) {
                    assertThat(values).contains(kw);
                }
            } catch (Exception e) {
                throw new RuntimeException("No se pudo leer FORBIDDEN_KEYWORDS", e);
            }
        }
    }
}
