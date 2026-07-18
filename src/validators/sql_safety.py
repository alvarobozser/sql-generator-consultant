"""Validador de seguridad SQL.

Asegura que solo se ejecutan queries de lectura (SELECT/WITH/EXPLAIN)
y bloquea cualquier intento de escritura o DDL, incluyendo intentos
de esconder keywords en comentarios.

El flujo de validación es:
1. Parsear con sqlparse.
2. Comprobar que hay exactamente un statement.
3. Comprobar que el tipo de statement es SELECT, WITH o EXPLAIN.
4. Comprobar que no hay comentarios SQL (los comentarios pueden ocultar keywords).
5. Comprobar que no hay keywords peligrosas (DROP, DELETE, INSERT, etc.) en ninguna
   parte del SQL, por si el LLM intenta meterlas como string o subquery.

Excepciones:
    UnsafeSQLError: si el SQL no pasa alguna comprobación.
"""

from __future__ import annotations

import re

import sqlparse
from sqlparse.sql import Statement
from sqlparse.tokens import Comment

# Keywords permitidas como tipo de statement principal.
ALLOWED_STATEMENT_TYPES = {"SELECT", "WITH", "EXPLAIN"}

# Keywords bloqueadas en cualquier parte del SQL (case-insensitive).
# Incluimos DDL, DML de escritura, y comandos de administración.
FORBIDDEN_KEYWORDS = {
    "INSERT",
    "UPDATE",
    "DELETE",
    "DROP",
    "ALTER",
    "TRUNCATE",
    "GRANT",
    "REVOKE",
    "CREATE",
    "EXEC",
    "EXECUTE",
    "CALL",
    "COPY",
    "VACUUM",
    "REINDEX",
    "CLUSTER",
    "LOCK",
    "SET",
    "RESET",
}


class UnsafeSQLError(ValueError):
    """El SQL no es seguro para ejecutar (no es solo-lectura)."""


def _has_sql_comments(sql: str) -> bool:
    """Detecta si el SQL contiene comentarios de una línea (--) o multilínea (/* */)."""
    # Comentario de una línea: -- hasta fin de línea.
    if re.search(r"--", sql):
        return True
    # Comentario multilínea: /* ... */.
    return bool(re.search(r"/\*", sql) and re.search(r"\*/", sql))


def _has_forbidden_keyword(sql: str) -> str | None:
    """Devuelve la primera keyword prohibida encontrada en el SQL, o None.

    Busca como palabra completa (con word boundary) para no falsear positivos
    tipo "updated_at" (que contiene "update" como substring).
    """
    sql_upper = sql.upper()
    for kw in FORBIDDEN_KEYWORDS:
        pattern = rf"\b{re.escape(kw)}\b"
        if re.search(pattern, sql_upper):
            return kw
    return None


def validate_sql(sql: str) -> None:
    """Valida que el SQL sea seguro para ejecutar.

    Args:
        sql: La query SQL a validar.

    Raises:
        UnsafeSQLError: Si el SQL no es seguro.
        ValueError: Si el SQL está vacío o es None.

    Examples:
        >>> validate_sql("SELECT * FROM users")  # OK
        >>> validate_sql("DROP TABLE users")  # UnsafeSQLError
    """
    if not sql or not sql.strip():
        raise ValueError("El SQL está vacío")

    sql = sql.strip()

    if _has_sql_comments(sql):
        raise UnsafeSQLError("El SQL contiene comentarios, que podrían ocultar keywords peligrosas")

    # Parsear primero para detectar multi-statement antes de buscar keywords.
    # Si el SQL tiene "SELECT 1; DROP TABLE x", queremos reportar el multi-statement
    # (más accionable para el usuario) y no "DROP está prohibido" (que sería el mismo
    # fallo pero con mensaje menos claro).
    statements = [s for s in sqlparse.parse(sql) if s.tokens and str(s).strip()]

    if len(statements) == 0:
        raise ValueError("El SQL no contiene ningún statement")

    if len(statements) > 1:
        raise UnsafeSQLError(
            f"Solo se permite un statement, se encontraron {len(statements)}. "
            "El uso de ';' para encadenar queries no está permitido."
        )

    forbidden = _has_forbidden_keyword(sql)
    if forbidden:
        raise UnsafeSQLError(f"El SQL contiene la keyword prohibida: {forbidden}")

    stmt = statements[0]
    stmt_type = _get_statement_type(stmt)

    if stmt_type not in ALLOWED_STATEMENT_TYPES:
        raise UnsafeSQLError(
            f"Solo se permiten queries de lectura (SELECT, WITH, EXPLAIN). "
            f"Se recibió: {stmt_type or 'tipo desconocido'}"
        )


def _get_statement_type(stmt: Statement) -> str:
    """Obtiene el tipo principal de un statement (SELECT, WITH, INSERT, etc.).

    Para CTEs (WITH ... AS (...)), sqlparse devuelve 'AS' como primer token
    de keyword, así que hay que mirar el primer token no-Comentario que sea
    un Keyword (o tipo similar) y reconstruir el tipo real.
    """
    # Recorremos los tokens del statement (no flatten, para respetar estructura).
    for token in stmt.tokens:
        if token.ttype is Comment:
            continue
        if token.is_whitespace:
            continue
        value = str(token).upper().strip()
        if not value:
            continue
        # Caso CTE: el primer token es la keyword WITH, el segundo es el
        # identificador, el tercero es la keyword AS, y luego viene el SELECT.
        if value == "WITH":
            return "WITH"
        if value == "EXPLAIN":
            return "EXPLAIN"
        if value == "SELECT":
            return "SELECT"
        # Primer token significativo: es la keyword principal (INSERT, DELETE, etc.).
        return value.split()[0]

    return ""
