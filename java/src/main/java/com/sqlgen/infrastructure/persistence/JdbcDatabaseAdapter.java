package com.sqlgen.infrastructure.persistence;

import com.sqlgen.domain.exception.SqlExecutionException;
import com.sqlgen.domain.port.out.DatabasePort;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador JDBC que ejecuta queries SELECT contra la BD de solo lectura.
 *
 * <p>Usa el DataSource "readonlyDataSource" (usuario sqlgen_readonly) por
 * defense in depth: aunque el validador SQL falle, la BD rechaza writes.
 */
@Component
public class JdbcDatabaseAdapter implements DatabasePort {

    private static final Logger log = LoggerFactory.getLogger(JdbcDatabaseAdapter.class);

    private final JdbcTemplate jdbcTemplate;

    public JdbcDatabaseAdapter(@Qualifier("readonlyJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public List<Map<String, Object>> executeQuery(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("sql no puede estar vacio");
        }
        log.debug("Ejecutando query: {}", sql);
        try {
            // rowMapper anonimo: cada fila es un Map en orden de insercion (LinkedHashMap).
            return jdbcTemplate.query(sql, (rs, rowNum) -> {
                Map<String, Object> row = new LinkedHashMap<>();
                int columnCount = rs.getMetaData().getColumnCount();
                for (int i = 1; i <= columnCount; i++) {
                    row.put(rs.getMetaData().getColumnLabel(i), rs.getObject(i));
                }
                return row;
            });
        } catch (Exception e) {
            // Envolvemos cualquier error de BD en SqlExecutionException.
            log.warn("Error ejecutando query: {}", e.getMessage());
            throw new SqlExecutionException(
                "Error ejecutando query: " + e.getMessage(), e
            );
        }
    }
}
