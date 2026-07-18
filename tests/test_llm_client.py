"""Tests unitarios para src.llm.client."""

from __future__ import annotations

from unittest.mock import MagicMock, patch

import pytest

from src.llm.client import DEFAULT_MODEL, LLMClient, LLMError, _clean_sql_response


class TestCleanSqlResponse:
    """Tests para _clean_sql_response()."""

    def test_quita_bloque_markdown_con_sql(self) -> None:
        """Quita ```sql ... ``` y deja el SQL limpio."""
        raw = "```sql\nSELECT * FROM users;\n```"
        assert _clean_sql_response(raw) == "SELECT * FROM users"

    def test_quita_bloque_markdown_sin_lenguaje(self) -> None:
        """Quita ``` ... ``` aunque no diga 'sql'."""
        raw = "```\nSELECT id FROM products\n```"
        assert _clean_sql_response(raw) == "SELECT id FROM products"

    def test_quita_punto_y_coma_final(self) -> None:
        """Quita ';' final si existe."""
        assert _clean_sql_response("SELECT 1;") == "SELECT 1"

    def test_devuelve_string_vacio_si_input_None(self) -> None:
        """None -> string vacío."""
        assert _clean_sql_response(None) == ""

    def test_devuelve_string_vacio_si_input_vacio(self) -> None:
        """'' -> string vacío."""
        assert _clean_sql_response("") == ""

    def test_quita_espacios_extra(self) -> None:
        """Quita espacios y newlines al inicio y final."""
        assert _clean_sql_response("\n\n  SELECT 1  \n\n") == "SELECT 1"


class TestLLMClientInit:
    """Tests para LLMClient.__init__()."""

    def test_constructor_con_args_validos(self) -> None:
        """Con args válidos no lanza excepción."""
        with patch("src.llm.client.Anthropic"):
            client = LLMClient(api_key="sk-ant-test")
            assert client.model == DEFAULT_MODEL

    def test_constructor_con_model_explicito(self) -> None:
        """Se puede pasar un modelo distinto al default."""
        with patch("src.llm.client.Anthropic"):
            client = LLMClient(api_key="sk-ant-test", model="claude-opus-4-1")
            assert client.model == "claude-opus-4-1"

    def test_constructor_falla_si_api_key_vacio(self) -> None:
        """Sin API key lanza ValueError claro."""
        with pytest.raises(ValueError, match="api_key"):
            LLMClient(api_key="")

    def test_constructor_falla_si_model_vacio(self) -> None:
        """Sin model lanza ValueError claro."""
        with pytest.raises(ValueError, match="model"):
            LLMClient(api_key="sk-ant-test", model="")

    def test_constructor_quita_espacios(self) -> None:
        """Quita espacios al inicio/final de los args."""
        with patch("src.llm.client.Anthropic"):
            client = LLMClient(api_key="  sk-ant-test  ", model="  claude-haiku-4-5  ")
            assert client.model == "claude-haiku-4-5"


def _build_mock_response(content_blocks: list) -> MagicMock:
    """Helper: crea un mock de respuesta de Anthropic con los bloques de texto dados."""
    response = MagicMock()
    response.content = []
    for text in content_blocks:
        block = MagicMock()
        block.text = text
        response.content.append(block)
    return response


def _build_mock_response_single(content: str) -> MagicMock:
    """Helper: crea un mock con un único bloque de texto."""
    return _build_mock_response([content])


class TestLLMClientGenerateSql:
    """Tests para LLMClient.generate_sql()."""

    def test_generate_sql_happy_path(self) -> None:
        """Caso feliz: respuesta limpia, devuelve SQL."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_client.messages.create.return_value = _build_mock_response_single(
                "SELECT * FROM users"
            )
            mock_anthropic_cls.return_value = mock_client

            client = LLMClient(api_key="sk-ant-test")
            sql = client.generate_sql("dame los usuarios", "system prompt")

            assert sql == "SELECT * FROM users"
            # Verificar que se llamó con los parámetros correctos.
            call_args = mock_client.messages.create.call_args
            assert call_args.kwargs["model"] == DEFAULT_MODEL
            assert call_args.kwargs["system"] == "system prompt"
            assert call_args.kwargs["messages"] == [
                {"role": "user", "content": "dame los usuarios"}
            ]
            assert call_args.kwargs["temperature"] == 0.0

    def test_generate_sql_limpia_markdown(self) -> None:
        """Si el LLM devuelve markdown, lo limpia."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_client.messages.create.return_value = _build_mock_response_single(
                "```sql\nSELECT * FROM products\n```"
            )
            mock_anthropic_cls.return_value = mock_client

            client = LLMClient(api_key="sk-ant-test")
            sql = client.generate_sql("productos", "sp")
            assert sql == "SELECT * FROM products"

    def test_generate_sql_combina_multiples_bloques(self) -> None:
        """Si la respuesta tiene varios content blocks, los concatena."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_client.messages.create.return_value = _build_mock_response(
                ["SELECT * ", "FROM users"]
            )
            mock_anthropic_cls.return_value = mock_client

            client = LLMClient(api_key="sk-ant-test")
            sql = client.generate_sql("q", "sp")
            assert sql == "SELECT * FROM users"

    def test_generate_sql_falla_si_pregunta_vacia(self) -> None:
        """Pregunta vacía -> ValueError, sin llamar a la API."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_anthropic_cls.return_value = mock_client
            client = LLMClient(api_key="sk-ant-test")
            with pytest.raises(ValueError, match="pregunta"):
                client.generate_sql("", "sp")
            mock_client.messages.create.assert_not_called()

    def test_generate_sql_falla_si_system_prompt_vacio(self) -> None:
        """System prompt vacío -> ValueError."""
        client = LLMClient(api_key="sk-ant-test")
        with pytest.raises(ValueError, match="system_prompt"):
            client.generate_sql("pregunta", "  ")

    def test_generate_sql_envuelve_error_de_api_en_LLMError(self) -> None:
        """Si la API lanza cualquier excepción, se envuelve en LLMError."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_client.messages.create.side_effect = RuntimeError("network down")
            mock_anthropic_cls.return_value = mock_client

            client = LLMClient(api_key="sk-ant-test")
            with pytest.raises(LLMError) as exc_info:
                client.generate_sql("q", "sp")
            assert "Error llamando a la API" in str(exc_info.value)
            assert "network down" in str(exc_info.value)
            assert exc_info.value.__cause__ is not None

    def test_generate_sql_falla_si_respuesta_vacia(self) -> None:
        """Si el LLM devuelve string vacío -> LLMError."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_client.messages.create.return_value = _build_mock_response_single("")
            mock_anthropic_cls.return_value = mock_client

            client = LLMClient(api_key="sk-ant-test")
            with pytest.raises(LLMError, match="vacía"):
                client.generate_sql("q", "sp")

    def test_generate_sql_falla_si_solo_markdown_vacio(self) -> None:
        """Si el LLM devuelve solo ``` ``` con nada dentro -> LLMError."""
        with patch("src.llm.client.Anthropic") as mock_anthropic_cls:
            mock_client = MagicMock()
            mock_client.messages.create.return_value = _build_mock_response_single("```\n\n```")
            mock_anthropic_cls.return_value = mock_client

            client = LLMClient(api_key="sk-ant-test")
            with pytest.raises(LLMError, match="vacía"):
                client.generate_sql("q", "sp")
