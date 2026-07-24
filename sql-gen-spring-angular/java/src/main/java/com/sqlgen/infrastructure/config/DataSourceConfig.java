package com.sqlgen.infrastructure.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuracion de dos DataSources:
 * - adminDataSource: usuario sqlgen (read-write). Usado para inicializar el schema
 *   y para el SchemaPort (que lee information_schema).
 * - readonlyDataSource: usuario sqlgen_readonly. Usado por DatabasePort para
 *   ejecutar las queries del usuario (defense in depth: aunque el validador
 *   SQL falle, la BD rechaza writes).
 *
 * Las URLs y credenciales vienen de variables de entorno documentadas en .env.example.
 */
@Configuration
public class DataSourceConfig {

    /** Propiedades del DataSource admin (read-write). */
    @Bean(name = "adminDataSourceProperties")
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties adminDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** Propiedades del DataSource readonly. */
    @Bean(name = "readonlyDataSourceProperties")
    @ConfigurationProperties("spring.datasource.readonly")
    public DataSourceProperties readonlyDataSourceProperties() {
        return new DataSourceProperties();
    }

    /** DataSource admin (read-write). */
    @Bean(name = "adminDataSource")
    @Primary
    public DataSource adminDataSource(
        @Qualifier("adminDataSourceProperties") DataSourceProperties props
    ) {
        return buildDataSource(props);
    }

    /** DataSource readonly (solo SELECT). */
    @Bean(name = "readonlyDataSource")
    public DataSource readonlyDataSource(
        @Qualifier("readonlyDataSourceProperties") DataSourceProperties props
    ) {
        return buildDataSource(props);
    }

    /** JdbcTemplate para el admin. */
    @Bean(name = "adminJdbcTemplate")
    public JdbcTemplate adminJdbcTemplate(@Qualifier("adminDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    /** JdbcTemplate readonly (usado por el DatabasePort). */
    @Bean(name = "readonlyJdbcTemplate")
    public JdbcTemplate readonlyJdbcTemplate(@Qualifier("readonlyDataSource") DataSource ds) {
        return new JdbcTemplate(ds);
    }

    private DataSource buildDataSource(DataSourceProperties props) {
        HikariDataSource ds = new HikariDataSource();
        ds.setJdbcUrl(props.getUrl());
        ds.setUsername(props.getUsername());
        ds.setPassword(props.getPassword());
        ds.setDriverClassName("org.postgresql.Driver");
        return ds;
    }

    /** Properties de un DataSource (URL, user, pass). */
    public static class DataSourceProperties {
        private String url;
        private String username;
        private String password;

        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getPassword() { return password; }
        public void setPassword(String password) { this.password = password; }
    }
}
