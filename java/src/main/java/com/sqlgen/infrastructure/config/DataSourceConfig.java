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

    @Bean
    @Primary
    @ConfigurationProperties("spring.datasource")
    public DataSourceProperties adminDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "adminDataSource")
    @Primary
    public DataSource adminDataSource() {
        DataSourceProperties props = adminDataSourceProperties();
        return DataSourceBuilder.create()
            .url(props.getUrl())
            .username(props.getUsername())
            .password(props.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    }

    @Bean(name = "readonlyDataSource")
    @ConfigurationProperties("spring.datasource.readonly")
    public DataSourceProperties readonlyDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean(name = "readonlyDataSource")
    public DataSource readonlyDataSource() {
        DataSourceProperties props = readonlyDataSourceProperties();
        return DataSourceBuilder.create()
            .url(props.getUrl())
            .username(props.getUsername())
            .password(props.getPassword())
            .driverClassName("org.postgresql.Driver")
            .build();
    }

    @Bean(name = "adminJdbcTemplate")
    public JdbcTemplate adminJdbcTemplate(@Qualifier("adminDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    @Bean(name = "readonlyJdbcTemplate")
    public JdbcTemplate readonlyJdbcTemplate(@Qualifier("readonlyDataSource") DataSource dataSource) {
        return new JdbcTemplate(dataSource);
    }

    /** Wrapper para tener las props con su tipo concreto. */
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
