"""Orquestador: conecta LLM + validador + BD en un solo flujo.

Esta es la pieza central de la app:
1. Recibe una pregunta en lenguaje natural.
2. Llama al LLM para generar SQL.
3. Valida el SQL con el safety validator.
4. Si pasa, ejecuta contra PostgreSQL.
5. Devuelve el resultado (SQL + filas) o un error claro.

Maneja errores de cada capa envolviéndolos en mensajes amigables para
mostrar en la UI. Nunca expone stack traces crudos al usuario final.
"""

from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from src.db.connection import DatabaseError, execute_query
from src.llm.client import LLMClient, LLMError
from src.prompts.templates import get_system_prompt
from src.validators.sql_safety import UnsafeSQLError, validate_sql

# Mensajes de error estandarizados para la UI.
ERR_LLM = "Error generando SQL con el LLM"
ERR_VALIDATION = "El SQL generado no es seguro"
ERR_DB = "Error ejecutando la consulta en la base de datos"


@dataclass(frozen=True)
class QueryResult:
    """Resultado de process_question().

    Attributes:
        sql: El SQL que se generó y ejecutó (o se intentó ejecutar).
        rows: Filas devueltas por la BD (None si hubo error).
        columns: Nombres de las columnas (None si no se ejecutó).
        error: Mensaje de error legible para la UI (None si todo fue bien).
    """

    sql: str
    rows: list[dict[str, Any]] | None
    columns: list[str] | None
    error: str | None

    @property
    def success(self) -> bool:
        """True si no hubo error y se obtuvieron filas."""
        return self.error is None and self.rows is not None


def _extract_columns(rows: list[dict[str, Any]]) -> list[str]:
    """Extrae los nombres de columna de una lista de filas."""
    if not rows:
        return []
    return list(rows[0].keys())


def process_question(
    question: str,
    language: str,
    llm_client: LLMClient,
    database_url: str,
) -> QueryResult:
    """Procesa una pregunta en lenguaje natural y devuelve el resultado.

    Flujo:
        1. get_system_prompt(language) -> prompt
        2. llm_client.generate_sql(question, prompt) -> sql
        3. validate_sql(sql) -> (raises UnsafeSQLError si no es seguro)
        4. execute_query(sql, database_url) -> rows
        5. return QueryResult(sql=sql, rows=rows, columns=..., error=None)

    Si algo falla en cualquier paso, devuelve QueryResult con `error` poblado
    y `rows=None`. El SQL generado se devuelve siempre que se haya generado
    (incluso si la validación o ejecución fallan), para que la UI pueda
    mostrarlo al usuario.

    Args:
        question: Pregunta en lenguaje natural del usuario.
        language: Código de idioma ('es' o 'en').
        llm_client: Cliente LLM ya inicializado.
        database_url: Cadena de conexión a PostgreSQL.

    Returns:
        QueryResult con sql, rows, columns y error.
    """
    # Paso 1: obtener el system prompt del idioma.
    try:
        system_prompt = get_system_prompt(language)
    except ValueError as e:
        return QueryResult(sql="", rows=None, columns=None, error=str(e))

    # Paso 2: generar SQL con el LLM.
    try:
        sql = llm_client.generate_sql(question, system_prompt)
    except (LLMError, ValueError) as e:
        return QueryResult(sql="", rows=None, columns=None, error=f"{ERR_LLM}: {e}")

    # Paso 3: validar SQL (defensa en profundidad: incluso si el LLM promete
    # ser seguro, validamos antes de ejecutar).
    try:
        validate_sql(sql)
    except UnsafeSQLError as e:
        return QueryResult(sql=sql, rows=None, columns=None, error=f"{ERR_VALIDATION}: {e}")
    except ValueError as e:
        # SQL vacío o inválido de entrada.
        return QueryResult(sql=sql, rows=None, columns=None, error=f"{ERR_VALIDATION}: {e}")

    # Paso 4: ejecutar contra la BD.
    try:
        rows = execute_query(sql, database_url)
    except DatabaseError as e:
        return QueryResult(sql=sql, rows=None, columns=None, error=f"{ERR_DB}: {e}")

    # Paso 5: devolver resultado exitoso.
    return QueryResult(
        sql=sql,
        rows=rows,
        columns=_extract_columns(rows),
        error=None,
    )
