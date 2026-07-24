package com.sqlgen.domain.model;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

/** Tests para {@link QueryResult}. */
class QueryResultTest {

    @Test
    void success_factory_crea_resultado_exitoso() {
        var rows = List.<Map<String, Object>>of(Map.of("id", 1, "name", "Ana"));
        var result = QueryResult.success("SELECT 1", rows, List.of("id", "name"));

        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isError()).isFalse();
        assertThat(result.sql()).isEqualTo("SELECT 1");
        assertThat(result.rows()).isEqualTo(rows);
        assertThat(result.columns()).containsExactly("id", "name");
        assertThat(result.error()).isNull();
    }

    @Test
    void failure_factory_crea_resultado_con_error() {
        var result = QueryResult.failure("DROP TABLE x", "SQL no seguro");

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isError()).isTrue();
        assertThat(result.sql()).isEqualTo("DROP TABLE x");
        assertThat(result.rows()).isNull();
        assertThat(result.error()).isEqualTo("SQL no seguro");
    }

    @Test
    void empty_factory_crea_resultado_vacio() {
        var result = QueryResult.empty();

        assertThat(result.isSuccess()).isFalse();
        assertThat(result.isError()).isFalse();
        assertThat(result.sql()).isNull();
        assertThat(result.rows()).isNull();
    }

    @Test
    void safeRows_devuelve_lista_vacia_si_rows_es_null() {
        var result = QueryResult.failure("SELECT 1", "error");

        assertThat(result.safeRows()).isEmpty();
    }

    @Test
    void safeRows_devuelve_lista_original_si_no_es_null() {
        var rows = List.<Map<String, Object>>of(Map.of("x", 1));
        var result = QueryResult.success("SELECT 1", rows, List.of("x"));

        assertThat(result.safeRows()).isSameAs(rows);
    }

    @Test
    void records_son_inmutables() {
        var rows = List.<Map<String, Object>>of(Map.of("id", 1));
        var result = QueryResult.success("SELECT 1", rows, List.of("id"));

        // Intentamos modificar la lista externa: debe lanzar porque es inmutable.
        org.junit.jupiter.api.Assertions.assertThrows(
            UnsupportedOperationException.class,
            () -> result.rows().add(Map.<String, Object>of("id", 999))
        );
    }
}
