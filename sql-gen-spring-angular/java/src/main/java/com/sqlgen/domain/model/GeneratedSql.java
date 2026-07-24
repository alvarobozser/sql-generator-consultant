package com.sqlgen.domain.model;

/** SQL generado por el LLM, antes de validarlo. Record inmutable. */
public record GeneratedSql(String sql, String originalQuestion) {

    public GeneratedSql {
        if (sql == null) {
            throw new IllegalArgumentException("sql no puede ser null");
        }
    }
}
