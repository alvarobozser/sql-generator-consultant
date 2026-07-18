"""Capa de conexión a PostgreSQL.

Provee:
- get_connection(): context manager que abre/cierra conexiones de forma segura.
- execute_query(sql): ejecuta un SELECT y devuelve filas como lista de dicts.
- DatabaseError: excepción custom para errores de la BD.

La capa de validación SQL (solo lectura) está separada en `src.validators.sql_safety`.
Aquí NO se valida nada: se asume que la query ya pasó por el validador.
"""

from __future__ import annotations

from collections.abc import Generator
from contextlib import contextmanager
from typing import Any

import psycopg
from psycopg.rows import dict_row


class DatabaseError(Exception):
    """Error de base de datos. Envuelve la excepción original."""

    def __init__(self, message: str, original: Exception | None = None) -> None:
        super().__init__(message)
        self.original = original


@contextmanager
def get_connection(database_url: str) -> Generator[psycopg.Connection, None, None]:
    """Abre una conexión a PostgreSQL y la cierra al salir del bloque.

    Args:
        database_url: Cadena de conexión (formato postgresql://...).

    Yields:
        La conexión abierta.

    Raises:
        DatabaseError: Si no se puede establecer la conexión.
    """
    try:
        conn = psycopg.connect(database_url, row_factory=dict_row)
    except psycopg.Error as e:
        raise DatabaseError(f"No se pudo conectar a PostgreSQL: {e}", original=e) from e

    try:
        yield conn
    finally:
        conn.close()


def execute_query(sql: str, database_url: str) -> list[dict[str, Any]]:
    """Ejecuta un SELECT y devuelve las filas como lista de dicts.

    IMPORTANTE: esta función NO valida que el SQL sea seguro.
    El caller debe haber pasado la query por `validators.sql_safety.validate_sql`
    antes de llamarla. Aquí solo se ejecuta y se devuelven resultados.

    Args:
        sql: La query SQL a ejecutar.
        database_url: Cadena de conexión.

    Returns:
        Lista de filas, cada una como un dict {columna: valor}.

    Raises:
        DatabaseError: Si la query falla por motivos de BD (tabla inexistente,
            sintaxis incorrecta, etc.).
    """
    try:
        with get_connection(database_url) as conn, conn.cursor() as cur:
            cur.execute(sql)
            return list(cur.fetchall())
    except DatabaseError:
        # Re-raise errores de conexión sin envolver dos veces.
        raise
    except psycopg.Error as e:
        raise DatabaseError(f"Error ejecutando query: {e}", original=e) from e
