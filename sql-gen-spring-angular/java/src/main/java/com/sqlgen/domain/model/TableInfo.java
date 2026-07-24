package com.sqlgen.domain.model;

import java.util.List;

/** Info de una tabla: nombre + columnas. Record inmutable. */
public record TableInfo(String name, List<ColumnInfo> columns) {

    public TableInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name no puede estar vacio");
        }
        columns = columns == null ? List.of() : List.copyOf(columns);
    }
}
