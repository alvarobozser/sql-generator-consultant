package com.sqlgen.domain.model;

/** Info de una columna: nombre + tipo + nullable. Record inmutable. */
public record ColumnInfo(String name, String type, boolean nullable) {

    public ColumnInfo {
        if (name == null || name.isBlank()) {
            throw new IllegalArgumentException("name no puede estar vacio");
        }
        if (type == null || type.isBlank()) {
            throw new IllegalArgumentException("type no puede estar vacio");
        }
    }
}
