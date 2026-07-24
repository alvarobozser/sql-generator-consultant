"""Tests unitarios para src.orchestrator."""

from __future__ import annotations

import dataclasses
from unittest.mock import MagicMock

import pytest

from src.db.connection import DatabaseError
from src.llm.client import LLMError
from src.orchestrator import (
    ERR_DB,
    ERR_LLM,
    ERR_VALIDATION,
    QueryResult,
    process_question,
)


@pytest.fixture
def llm_client_mock() -> MagicMock:
    """Mock de LLMClient con un SQL happy-path predefinido."""
    mock = MagicMock()
    mock.generate_sql.return_value = "SELECT id, name FROM users"
    return mock


class TestProcessQuestionHappyPath:
    """Tests del camino feliz."""

    def test_devuelve_QueryResult_con_filas(self, llm_client_mock: MagicMock) -> None:
        """Caso feliz: LLM genera SQL valido, BD devuelve filas."""
        expected_rows = [{"id": 1, "name": "Ana"}, {"id": 2, "name": "Carlos"}]
        with pytest.MonkeyPatch.context() as mp:
            # Mockeamos execute_query para devolver filas sin tocar la BD.
            from src import orchestrator

            mp.setattr(
                orchestrator, "execute_query", lambda sql, url: expected_rows
            )

            result = process_question(
                question="dame los usuarios",
                language="es",
                llm_client=llm_client_mock,
                database_url="postgresql://fake",
            )

        assert result.success is True
        assert result.error is None
        assert result.sql == "SELECT id, name FROM users"
        assert result.rows == expected_rows
        assert result.columns == ["id", "name"]
        # Verificamos que el LLM recibio el system prompt en espanol.
        call_args = llm_client_mock.generate_sql.call_args
        assert "Pregunta" in call_args.args[1] or "usuarios" in call_args.args[1].lower()

    def test_pasa_idioma_al_LLM(self, llm_client_mock: MagicMock) -> None:
        """El idioma se traduce a un system prompt en el idioma correcto."""
        with pytest.MonkeyPatch.context() as mp:
            from src import orchestrator

            mp.setattr(orchestrator, "execute_query", lambda sql, url: [])
            process_question(
                question="hi",
                language="en",
                llm_client=llm_client_mock,
                database_url="postgresql://fake",
            )
        # El system prompt en ingles contiene palabras como "Question" o "Question:".
        call_args = llm_client_mock.generate_sql.call_args
        system_prompt = call_args.args[1]
        assert "Question" in system_prompt or "SELECT" in system_prompt

    def test_query_sin_resultados_devuelve_lista_vacia(self, llm_client_mock: MagicMock) -> None:
        """Si la query no devuelve filas, rows=[] y columns=[]."""
        with pytest.MonkeyPatch.context() as mp:
            from src import orchestrator

            mp.setattr(orchestrator, "execute_query", lambda sql, url: [])
            result = process_question(
                question="dame los usuarios",
                language="es",
                llm_client=llm_client_mock,
                database_url="postgresql://fake",
            )
        assert result.success is True
        assert result.rows == []
        assert result.columns == []


class TestProcessQuestionLlmError:
    """Tests cuando el LLM falla."""

    def test_LLMError_se_envuelve_en_error_legible(self) -> None:
        """Si el LLM lanza LLMError, devolvemos error con prefijo claro."""
        mock = MagicMock()
        mock.generate_sql.side_effect = LLMError("API down")
        result = process_question("q", "es", mock, "postgresql://fake")
        assert result.error is not None
        assert ERR_LLM in result.error
        assert "API down" in result.error
        assert result.rows is None
        assert result.sql == ""

    def test_ValueError_del_LLM_se_envuelve(self) -> None:
        """Si el LLM lanza ValueError (pregunta vacia), error claro."""
        mock = MagicMock()
        mock.generate_sql.side_effect = ValueError("pregunta vacia")
        result = process_question("q", "es", mock, "postgresql://fake")
        assert result.error is not None
        assert ERR_LLM in result.error


class TestProcessQuestionValidationError:
    """Tests cuando el SQL generado no es seguro."""

    def test_UnsafeSQLError_devuelve_error_de_validacion(self) -> None:
        """Si el validador detecta SQL peligroso, devolvemos error claro."""
        mock = MagicMock()
        mock.generate_sql.return_value = "DROP TABLE users"
        result = process_question("borra todo", "es", mock, "postgresql://fake")
        assert result.error is not None
        assert ERR_VALIDATION in result.error
        # El SQL se devuelve igualmente para que la UI lo muestre.
        assert result.sql == "DROP TABLE users"
        assert result.rows is None
        # El LLM no fue llamado para nada mas (no se intento ejecutar).
        mock.generate_sql.assert_called_once()

    def test_ValueError_del_validador_se_envuelve(self) -> None:
        """Si el validador lanza ValueError (SQL vacio), error claro."""
        mock = MagicMock()
        mock.generate_sql.return_value = ""
        result = process_question("q", "es", mock, "postgresql://fake")
        assert result.error is not None
        assert ERR_VALIDATION in result.error


class TestProcessQuestionDbError:
    """Tests cuando la BD falla."""

    def test_DatabaseError_devuelve_error_de_BD(self) -> None:
        """Si execute_query lanza DatabaseError, devolvemos error claro."""
        mock = MagicMock()
        mock.generate_sql.return_value = "SELECT * FROM tabla_inexistente"
        with pytest.MonkeyPatch.context() as mp:
            from src import orchestrator

            mp.setattr(
                orchestrator,
                "execute_query",
                MagicMock(side_effect=DatabaseError("relation does not exist")),
            )
            result = process_question("q", "es", mock, "postgresql://fake")
        assert result.error is not None
        assert ERR_DB in result.error
        assert "relation does not exist" in result.error
        assert result.sql == "SELECT * FROM tabla_inexistente"
        assert result.rows is None


class TestProcessQuestionInvalidLanguage:
    """Tests con idioma invalido."""

    def test_idioma_invalido_devuelve_error(self, llm_client_mock: MagicMock) -> None:
        """Si el idioma no es soportado, error claro sin tocar el LLM."""
        result = process_question(
            "q", "fr", llm_client_mock, "postgresql://fake"
        )
        assert result.error is not None
        assert "Idioma no soportado" in result.error
        # El LLM no se llamo porque fallamos antes.
        llm_client_mock.generate_sql.assert_not_called()


class TestQueryResultDataclass:
    """Tests del dataclass QueryResult."""

    def test_success_cuando_sin_error_y_con_filas(self) -> None:
        """success=True solo si no hay error y hay filas."""
        ok = QueryResult(sql="SELECT 1", rows=[{"x": 1}], columns=["x"], error=None)
        assert ok.success is True

    def test_success_False_con_error(self) -> None:
        """success=False si hay error."""
        err = QueryResult(sql="DROP x", rows=None, columns=None, error="bad")
        assert err.success is False

    def test_success_False_sin_filas(self) -> None:
        """success=False si rows es None aunque no haya error (caso raro)."""
        empty = QueryResult(sql="SELECT 1", rows=None, columns=None, error=None)
        assert empty.success is False

    def test_inmutable(self) -> None:
        """QueryResult es frozen: no se puede modificar."""
        result = QueryResult(sql="x", rows=[], columns=[], error=None)
        with pytest.raises(dataclasses.FrozenInstanceError):
            result.sql = "modified"  # type: ignore[misc]
