package com.sqlgen.infrastructure.validation;

import com.sqlgen.domain.exception.UnsafeSqlException;
import com.sqlgen.domain.port.out.SqlSafetyValidatorPort;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.ExplainStatement;
import org.springframework.stereotype.Component;

import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validador SQL con JSqlParser. Port Java directo del validador Python.
 *
 * <p>Reglas (equivalentes a src/validators/sql_safety.py en Python):
 * 1. SQL no vacio
 * 2. Sin comentarios SQL (de linea o multilinea)
 * 3. Exactamente UN statement
 * 4. Tipo del statement: SELECT, WITH o EXPLAIN
 * 5. Sin keywords prohibidas (18 keywords), con word boundary
 *
 * <p>Defense in depth: ademas de esto, la app usa un usuario de BD de solo
 * lectura (sqlgen_readonly) en el Docker Compose.
 */
@Component
public class JSqlParserSqlSafetyValidator implements SqlSafetyValidatorPort {

    /** Tipos de statement que podemos ejecutar (todo lo demas se rechaza). */
    private static final Set<String> ALLOWED_TYPES = Set.of("SELECT", "WITH", "EXPLAIN");

    /**
     * 18 keywords prohibidas. Case-insensitive, con word boundary.
     * Incluye DDL, DML de escritura, y comandos administrativos.
     */
    private static final String[] FORBIDDEN_KEYWORDS = {
        "INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE",
        "GRANT", "REVOKE", "CREATE", "EXEC", "EXECUTE", "CALL",
        "COPY", "VACUUM", "REINDEX", "CLUSTER", "LOCK", "SET", "RESET"
    };

    @Override
    public void validate(String sql) {
        if (sql == null || sql.isBlank()) {
            throw new IllegalArgumentException("El SQL esta vacio");
        }
        String trimmed = sql.trim();

        if (hasSqlComment(trimmed)) {
            throw new UnsafeSqlException(
                "El SQL contiene comentarios, que podrian ocultar keywords peligrosas"
            );
        }

        // Parsear primero para detectar multi-statement antes de buscar keywords.
        // Si el SQL tiene "SELECT 1; DROP TABLE x", queremos reportar el multi-statement
        // (mas accionable para el usuario) y no "DROP esta prohibido" (que seria el mismo
        // fallo pero con mensaje menos claro).
        Statements statements;
        try {
            statements = CCJSqlParserUtil.parseStatements(trimmed);
        } catch (JSQLParserException e) {
            throw new UnsafeSqlException("SQL no parseable: " + e.getMessage());
        }

        if (statements.getStatements() == null || statements.getStatements().isEmpty()) {
            throw new UnsafeSqlException("El SQL no contiene ningun statement");
        }

        if (statements.getStatements().size() > 1) {
            throw new UnsafeSqlException(
                "Solo se permite un statement, se encontraron "
                    + statements.getStatements().size()
                    + ". El uso de ';' para encadenar queries no esta permitido."
            );
        }

        for (String keyword : FORBIDDEN_KEYWORDS) {
            if (containsKeyword(trimmed, keyword)) {
                throw new UnsafeSqlException(
                    "El SQL contiene la keyword prohibida: " + keyword
                );
            }
        }

        Statement stmt = statements.getStatements().get(0);
        String stmtType = getStatementType(stmt);

        if (!ALLOWED_TYPES.contains(stmtType)) {
            throw new UnsafeSqlException(
                "Solo se permiten queries de lectura (SELECT, WITH, EXPLAIN). "
                    + "Se recibio: " + (stmtType.isEmpty() ? "tipo desconocido" : stmtType)
            );
        }
    }

    /** Detecta comentarios SQL de linea o multilinea. */
    private boolean hasSqlComment(String sql) {
        if (sql.contains("--")) {
            return true;
        }
        return sql.contains("/*") && sql.contains("*/");
    }

    /** Comprueba si el SQL contiene la keyword como palabra completa (word boundary). */
    private boolean containsKeyword(String sql, String keyword) {
        // Regex: palabra rodeada de no-word-characters. Case-insensitive.
        String pattern = "(?i)(?<![A-Za-z0-9_])" + keyword + "(?![A-Za-z0-9_])";
        return Pattern.compile(pattern).matcher(sql).find();
    }

    /** Obtiene el tipo principal de un statement: SELECT, WITH, EXPLAIN, INSERT, etc. */
    private String getStatementType(Statement stmt) {
        if (stmt instanceof Select) {
            // Si el primer Select tiene WITH, el statement es WITH.
            // Si es un PlainSelect, es SELECT. Si es un SetOperationList (UNION etc.), tambien SELECT.
            if (stmt.toString().trim().toUpperCase().startsWith("WITH")) {
                return "WITH";
            }
            return "SELECT";
        }
        if (stmt instanceof ExplainStatement) {
            return "EXPLAIN";
        }
        // Para cualquier otro tipo (Insert, Update, Delete, etc.), usamos
        // el nombre de la clase en mayusculas.
        return stmt.getClass().getSimpleName().toUpperCase();
    }
}
