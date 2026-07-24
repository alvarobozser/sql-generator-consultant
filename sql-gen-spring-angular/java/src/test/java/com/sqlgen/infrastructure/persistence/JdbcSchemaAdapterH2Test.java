package com.sqlgen.infrastructure.persistence;

import com.sqlgen.domain.model.SchemaInfo;
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

@SpringBootTest(classes = {
    JdbcSchemaAdapter.class,
    JdbcSchemaAdapterH2Test.TestConfig.class
})
@ActiveProfiles("test")
class JdbcSchemaAdapterH2Test {

    @Configuration
    static class TestConfig {
        @Bean
        @Qualifier("adminDataSource")
        public DataSource adminDataSource() {
            return new EmbeddedDatabaseBuilder()
                .setType(EmbeddedDatabaseType.H2)
                .addScript("classpath:schema-test.sql")
                .build();
        }

        @Bean
        @Qualifier("adminJdbcTemplate")
        public JdbcTemplate adminJdbcTemplate(@Qualifier("adminDataSource") DataSource ds) {
            return new JdbcTemplate(ds);
        }
    }

    private final JdbcSchemaAdapter adapter;

    @Autowired
    JdbcSchemaAdapterH2Test(JdbcSchemaAdapter adapter) {
        this.adapter = adapter;
    }

    @Test
    void devuelve_schema_con_tablas_y_columnas() {
        SchemaInfo schema = adapter.getSchema();

        assertThat(schema.tables()).hasSize(1);
        var usersTable = schema.tables().get(0);
        assertThat(usersTable.name()).isEqualToIgnoringCase("users");
        assertThat(usersTable.columns()).hasSize(2);

        var idCol = usersTable.columns().stream()
            .filter(c -> c.name().equalsIgnoreCase("id"))
            .findFirst().orElseThrow();
        assertThat(idCol.nullable()).isFalse();

        var nameCol = usersTable.columns().stream()
            .filter(c -> c.name().equalsIgnoreCase("name"))
            .findFirst().orElseThrow();
        assertThat(nameCol.nullable()).isFalse();
    }

    @Test
    void findTable_encuentra_tabla_por_nombre() {
        SchemaInfo schema = adapter.getSchema();

        var t = schema.findTable("users");
        assertThat(t).isNotNull();
        assertThat(t.name()).isEqualToIgnoringCase("users");
    }

    @Test
    void findTable_devuelve_null_si_no_existe() {
        SchemaInfo schema = adapter.getSchema();

        assertThat(schema.findTable("nope")).isNull();
    }
}
