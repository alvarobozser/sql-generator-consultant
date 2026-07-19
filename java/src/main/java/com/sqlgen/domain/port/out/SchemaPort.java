package com.sqlgen.domain.port.out;

import com.sqlgen.domain.model.SchemaInfo;

/** Puerto de salida: lectura del schema de la base de datos. */
public interface SchemaPort {

    /** Devuelve la estructura del schema (tablas + columnas). */
    SchemaInfo getSchema();
}
