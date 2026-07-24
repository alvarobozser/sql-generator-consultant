package com.sqlgen.infrastructure.persistence;

import com.sqlgen.domain.model.ColumnInfo;
import com.sqlgen.domain.model.SchemaInfo;
import com.sqlgen.domain.model.TableInfo;
import com.sqlgen.domain.port.out.SchemaPort;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Adaptador JDBC que extrae el schema de la BD consultando information_schema.
 *
 * <p>Usa el DataSource "adminDataSource" (no necesita readonly porque
 * information_schema es de solo lectura por naturaleza).
 */
@Component
public class JdbcSchemaAdapter implements SchemaPort {

    /** Query para obtener tablas del schema public (case-insensitive para portabilidad H2/Postgres). */
    private static final String TABLES_QUERY = """
        SELECT table_name
        FROM information_schema.tables
        WHERE LOWER(table_schema) = 'public'
          AND table_type = 'BASE TABLE'
        ORDER BY table_name
        """;

    /** Query para obtener columnas de un schema (case-insensitive). */
    private static final String COLUMNS_QUERY = """
        SELECT column_name, data_type, is_nullable
        FROM information_schema.columns
        WHERE LOWER(table_schema) = 'public'
          AND table_name = ?
        ORDER BY ordinal_position
        """;

    private final JdbcTemplate jdbcTemplate;

    public JdbcSchemaAdapter(@Qualifier("adminJdbcTemplate") JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public SchemaInfo getSchema() {
        List<String> tableNames = jdbcTemplate.queryForList(TABLES_QUERY, String.class);
        if (tableNames.isEmpty()) {
            return new SchemaInfo(List.of());
        }

        // Agrupamos columnas por tabla.
        Map<String, List<ColumnInfo>> columnsByTable = new LinkedHashMap<>();
        for (String tableName : tableNames) {
            List<ColumnInfo> columns = jdbcTemplate.query(COLUMNS_QUERY,
                (rs, rowNum) -> new ColumnInfo(
                    rs.getString("column_name"),
                    rs.getString("data_type"),
                    "YES".equalsIgnoreCase(rs.getString("is_nullable"))
                ),
                tableName
            );
            columnsByTable.put(tableName, columns);
        }

        List<TableInfo> tables = new ArrayList<>();
        for (Map.Entry<String, List<ColumnInfo>> entry : columnsByTable.entrySet()) {
            tables.add(new TableInfo(entry.getKey(), entry.getValue()));
        }

        return new SchemaInfo(tables);
    }
}
