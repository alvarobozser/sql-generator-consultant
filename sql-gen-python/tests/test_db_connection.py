"""Tests unitarios para src.db.connection."""

from __future__ import annotations

from contextlib import contextmanager
from unittest.mock import MagicMock, patch

import psycopg
import pytest

from src.db.connection import DatabaseError, execute_query, get_connection


class TestGetConnection:
    """Tests para get_connection()."""

    def test_cierra_conexion_incluso_si_el_bloque_falla(self) -> None:
        """Si el bloque del `with` lanza excepción, la conexión se cierra igual."""
        mock_conn = MagicMock()
        with (
            patch("src.db.connection.psycopg.connect", return_value=mock_conn) as mock_connect,
            get_connection("postgresql://fake"),
            pytest.raises(RuntimeError, match="boom"),
        ):
            raise RuntimeError("boom")

        mock_connect.assert_called_once()
        mock_conn.close.assert_called_once()

    def test_cierra_conexion_en_camino_feliz(self) -> None:
        """En camino feliz, la conexión también se cierra al salir del bloque."""
        mock_conn = MagicMock()
        with (
            patch("src.db.connection.psycopg.connect", return_value=mock_conn),
            get_connection("postgresql://fake") as conn,
        ):
            assert conn is mock_conn

        mock_conn.close.assert_called_once()

    def test_falla_con_DatabaseError_si_psycopg_no_puede_conectar(self) -> None:
        """Si psycopg.connect lanza, lo envolvemos en DatabaseError."""
        original_error = psycopg.OperationalError("connection refused")
        with (
            patch("src.db.connection.psycopg.connect", side_effect=original_error),
            pytest.raises(DatabaseError) as exc_info,
            get_connection("postgresql://fake"),
        ):
            pass

        assert "No se pudo conectar" in str(exc_info.value)
        assert exc_info.value.original is original_error


@contextmanager
def _mock_connection_with_cursor(cursor_mock: MagicMock):
    """Context manager que simula ser un Connection con un cursor preconfigurado.

    Sustituye a get_connection: al hacer `with _mock_connection_with_cursor(cur) as conn`,
    dentro del bloque `conn.cursor()` retorna un context manager que da `cur`.
    """
    cursor_context = MagicMock()
    cursor_context.__enter__ = MagicMock(return_value=cursor_mock)
    cursor_context.__exit__ = MagicMock(return_value=False)

    conn = MagicMock()
    conn.cursor.return_value = cursor_context
    try:
        yield conn
    finally:
        # No hacemos nada especial: el mock se descarta solo.
        pass


class TestExecuteQuery:
    """Tests para execute_query()."""

    def test_devuelve_filas_como_dicts(self) -> None:
        """execute_query devuelve las filas como lista de dicts."""
        expected_rows = [{"id": 1, "name": "Ana"}, {"id": 2, "name": "Carlos"}]
        mock_cursor = MagicMock()
        mock_cursor.fetchall.return_value = expected_rows

        # patch sobre el módulo; el side_effect es el context manager
        # devuelto por _mock_connection_with_cursor(mock_cursor).
        cm = _mock_connection_with_cursor(mock_cursor)
        with patch("src.db.connection.get_connection", side_effect=lambda url: cm):
            rows = execute_query("SELECT * FROM users", "postgresql://fake")

        assert rows == expected_rows
        mock_cursor.execute.assert_called_once_with("SELECT * FROM users")

    def test_envuelve_error_de_psycopg_en_DatabaseError(self) -> None:
        """Si la query falla (p.ej. tabla no existe), se lanza DatabaseError."""
        original_error = psycopg.errors.UndefinedTable("table 'foo' does not exist")
        mock_cursor = MagicMock()
        mock_cursor.execute.side_effect = original_error

        cm = _mock_connection_with_cursor(mock_cursor)
        with (
            patch("src.db.connection.get_connection", side_effect=lambda url: cm),
            pytest.raises(DatabaseError) as exc_info,
        ):
            execute_query("SELECT * FROM foo", "postgresql://fake")

        assert "Error ejecutando query" in str(exc_info.value)
        assert exc_info.value.original is original_error

    def test_no_envuelve_Error_de_conexion_doblemente(self) -> None:
        """Si get_connection lanza DatabaseError, execute_query lo re-lanza tal cual."""
        original_db_error = DatabaseError("connection failed", original=Exception("x"))
        with (
            patch("src.db.connection.get_connection", side_effect=original_db_error),
            pytest.raises(DatabaseError) as exc_info,
        ):
            execute_query("SELECT 1", "postgresql://fake")

        assert exc_info.value is original_db_error
