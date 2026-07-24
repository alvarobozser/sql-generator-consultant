package com.sqlgen.infrastructure.persistence;

import com.sqlgen.domain.exception.SqlExecutionException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseBuilder;
import org.springframework.jdbc.datasource.embedded.EmbeddedDatabaseType;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Tests del adaptador JDBC con H2 in-memory (modo PostgreSQL).
 * Rapido y no requiere Docker. Para tests de integracion reales,
 * ver JdbcDatabaseAdapterIntegrationTest (Testcontainers).
 */
@SpringBootTest(classes = {
    JdbcDatabaseAdapter.class,
    JdbcDatabaseAdapterH2Test.TestConfig.class
})
@ActiveProfiles("test")
class JdbcDatabaseAdapterH2Test {

    @Configuration
    static class TestConfig {
        @Bean
        @Qualifier("readonlyDataSource")
        public DataSource readonlyDataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema-test.sql")
                .build();
        }

        @Bean
        @Qualifier("readonlyJdbcTemplate")
        public JdbcTemplate readonlyJdbcTemplate(@Qualifier("readonlyDataSource") DataSource ds) {
            return new JdbcTemplate(ds);
        }
    }

    private final JdbcDatabaseAdapter adapter;

    @Autowired
    JdbcDatabaseAdapterH2Test(JdbcDatabaseAdapter adapter) {
        this.adapter = adapter;
    }

    @BeforeEach
    void cleanState() {
        // Cada test empieza con la misma BD, definida en schema-test.sql.
    }

    @Test
    void ejecuta_select_basico() {
        var rows = adapter.executeQuery("SELECT id, name FROM users ORDER BY id");

        assertThat(rows).hasSize(3);
        // H2 devuelve nombres en MAYUSCULAS por defecto.
        assertThat(rows.get(0)).containsEntry("ID", 1).containsEntry("NAME", "Ana");
        assertThat(rows.get(1)).containsEntry("ID", 2).containsEntry("NAME", "Carlos");
        assertThat(rows.get(2)).containsEntry("ID", 3).containsEntry("NAME", "Sofia");
    }

    @Test
    void ejecuta_select_con_count() {
        var rows = adapter.executeQuery("SELECT COUNT(*) AS total FROM users");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("TOTAL")).isEqualTo(3L);
    }

    @Test
    void ejecuta_select_con_where() {
        var rows = adapter.executeQuery("SELECT name FROM users WHERE id = 2");

        assertThat(rows).hasSize(1);
        assertThat(rows.get(0).get("NAME")).isEqualTo("Carlos");
    }

    @Test
    void select_sin_resultados_devuelve_lista_vacia() {
        var rows = adapter.executeQuery("SELECT * FROM users WHERE id = 999");

        assertThat(rows).isEmpty();
    }

    @Test
    void query_a_tabla_inexistente_lanza_SqlExecutionException() {
        assertThatThrownBy(() -> adapter.executeQuery("SELECT * FROM tabla_inexistente"))
            .isInstanceOf(SqlExecutionException.class)
            .hasMessageContaining("tabla_inexistente");
    }

    @Test
    void query_con_sintaxis_invalida_lanza_SqlExecutionException() {
        assertThatThrownBy(() -> adapter.executeQuery("SELEKT * FROM users"))
            .isInstanceOf(SqlExecutionException.class);
    }

    @Test
    void query_vacia_lanza_IllegalArgumentException() {
        assertThatThrownBy(() -> adapter.executeQuery(""))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void query_null_lanza_IllegalArgumentException() {
        assertThatThrownBy(() -> adapter.executeQuery(null))
            .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void preserva_orden_de_columnas() {
        // H2 (como la mayoria de las BDs) deduplica nombres de columna repetidos.
        // Verificamos que el orden de columnas distintas se preserva via LinkedHashMap.
        var rows = adapter.executeQuery("SELECT id, name, id FROM users WHERE id = 1");

        // LinkedHashMap preserva el orden de insercion (id aparece primero que name).
        var keys = rows.get(0).keySet();
        assertThat(keys.iterator().next()).isEqualTo("ID");
    }
}
