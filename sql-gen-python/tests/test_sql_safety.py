"""Tests unitarios para src.validators.sql_safety."""

from __future__ import annotations

import pytest

from src.validators.sql_safety import (
    FORBIDDEN_KEYWORDS,
    UnsafeSQLError,
    validate_sql,
)


class TestValidateSqlAcceptsSafeQueries:
    """Queries que deben pasar la validación."""

    @pytest.mark.parametrize(
        "sql",
        [
            "SELECT * FROM users",
            "SELECT id, name FROM users WHERE country = 'Spain'",
            "SELECT count(*) FROM orders WHERE status = 'paid'",
            "WITH active_users AS (SELECT * FROM users WHERE country = 'Spain') "
            "SELECT * FROM active_users",
            "EXPLAIN SELECT * FROM products",
            "EXPLAIN ANALYZE SELECT * FROM orders",
            "select id from users",  # lowercase
            "  SELECT   *   FROM   users  ",  # espacios extra
        ],
    )
    def test_query_segura_pasa(self, sql: str) -> None:
        """Queries de lectura válidas deben pasar sin lanzar excepción."""
        validate_sql(sql)  # no debe lanzar


class TestValidateSqlRejectsUnsafeQueries:
    """Queries que deben ser bloqueadas."""

    @pytest.mark.parametrize(
        "sql",
        [
            "DROP TABLE users",
            "DROP DATABASE sqlgen",
            "DELETE FROM users",
            "DELETE FROM users WHERE id = 1",
            "INSERT INTO users (name) VALUES ('hacker')",
            "UPDATE users SET name = 'hacker'",
            "ALTER TABLE users ADD COLUMN hack TEXT",
            "TRUNCATE TABLE users",
            "CREATE TABLE hack (id INT)",
            "GRANT ALL ON users TO public",
            "REVOKE ALL ON users FROM public",
            "EXEC sp_something",
            "CALL my_procedure()",
            "VACUUM FULL",
            "REINDEX TABLE users",
        ],
    )
    def test_query_peligrosa_se_bloquea(self, sql: str) -> None:
        """Cada keyword prohibida debe ser detectada y lanzar UnsafeSQLError."""
        with pytest.raises(UnsafeSQLError) as exc_info:
            validate_sql(sql)
        # Verificamos que el mensaje menciona la keyword.
        assert "prohibida" in str(exc_info.value).lower() or "permiten" in str(exc_info.value).lower()

    def test_multiples_statements_se_bloquea(self) -> None:
        """Un ; encadenando dos queries debe ser bloqueado."""
        with pytest.raises(UnsafeSQLError, match="Solo se permite un statement"):
            validate_sql("SELECT 1; DROP TABLE users")

    def test_comentario_de_linea_se_bloquea(self) -> None:
        """Comentarios -- deben ser bloqueados (pueden esconder keywords)."""
        with pytest.raises(UnsafeSQLError, match="comentarios"):
            validate_sql("SELECT 1 -- esto es un comentario")

    def test_comentario_multilinea_se_bloquea(self) -> None:
        """Comentarios /* */ deben ser bloqueados."""
        with pytest.raises(UnsafeSQLError, match="comentarios"):
            validate_sql("SELECT 1 /* DROP TABLE users */")

    def test_keyword_prohibida_dentro_de_string_se_detecta(self) -> None:
        """Incluso si la keyword está como string, el validador la bloquea por defensa.

        Esto es deliberadamente conservador: preferimos falsos positivos a
        permitir SQL destructivo.
        """
        with pytest.raises(UnsafeSQLError):
            validate_sql("SELECT 'DROP TABLE users' AS warning")

    def test_columna_con_nombre_parecido_no_se_bloquea(self) -> None:
        """Nombres de columna que contienen keywords (ej. updated_at) NO se bloquean.

        El validador usa word boundary, así que 'updated_at' no dispara el
        check de 'update'.
        """
        # updated_at no debe ser bloqueado (es nombre de columna, no keyword).
        validate_sql("SELECT updated_at FROM orders")
        # created_at tampoco.
        validate_sql("SELECT created_at FROM users")

    def test_keyword_en_subquery_select_se_bloquea(self) -> None:
        """Un SELECT que contiene un DELETE en subquery debe bloquearse."""
        with pytest.raises(UnsafeSQLError):
            validate_sql("SELECT * FROM (DELETE FROM users RETURNING *) AS t")


class TestValidateSqlInputValidation:
    """Validación de inputs (vacíos, None, etc.)."""

    def test_string_vacio_lanza_ValueError(self) -> None:
        """SQL vacío debe lanzar ValueError claro."""
        with pytest.raises(ValueError, match="está vacío"):
            validate_sql("")

    def test_solo_espacios_lanza_ValueError(self) -> None:
        """SQL con solo espacios debe lanzar ValueError claro."""
        with pytest.raises(ValueError, match="está vacío"):
            validate_sql("    \n  \t  ")

    def test_todas_las_keywords_prohibidas_estan_en_la_lista(self) -> None:
        """Sanity check: la blacklist incluye las keywords críticas.

        Si alguien añade una keyword peligrosa nueva, este test ayuda a
        recordar añadirla también al blacklist.
        """
        critical = {"INSERT", "UPDATE", "DELETE", "DROP", "ALTER", "TRUNCATE", "GRANT", "REVOKE"}
        assert critical.issubset(FORBIDDEN_KEYWORDS), (
            f"Faltan keywords críticas en el blacklist: {critical - FORBIDDEN_KEYWORDS}"
        )
