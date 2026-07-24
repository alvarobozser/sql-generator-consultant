package com.sqlgen.domain.model;

import java.util.List;

/** Info completa del schema: lista de tablas. Record inmutable. */
public record SchemaInfo(List<TableInfo> tables) {

    public SchemaInfo {
        tables = tables == null ? List.of() : List.copyOf(tables);
    }

    /** Busca una tabla por nombre. Devuelve null si no existe. */
    public TableInfo findTable(String name) {
        return tables.stream()
            .filter(t -> t.name().equalsIgnoreCase(name))
            .findFirst()
            .orElse(null);
    }
}
