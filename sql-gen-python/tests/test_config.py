"""Tests unitarios para src.config."""

from __future__ import annotations

import pytest

from src.config import (
    DEFAULT_LLM_MODEL,
    ERR_INVALID_LANGUAGE,
    ERR_MISSING_API_KEY,
    ERR_MISSING_DATABASE_URL,
    load_config,
)


class TestLoadConfig:
    """Tests para load_config()."""

    def test_carga_todas_las_vars_cuando_estan_definidas(
        self, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Si todas las vars están, devuelve AppConfig con esos valores."""
        monkeypatch.setenv("ANTHROPIC_API_KEY", "sk-ant-test-123")
        monkeypatch.setenv("LLM_MODEL", "claude-opus-4-1")
        monkeypatch.setenv("DATABASE_URL", "postgresql://u:p@h:5432/d")
        monkeypatch.setenv("APP_LANGUAGE", "en")

        config = load_config()

        assert config.anthropic_api_key == "sk-ant-test-123"
        assert config.llm_model == "claude-opus-4-1"
        assert config.database_url == "postgresql://u:p@h:5432/d"
        assert config.app_language == "en"

    def test_usa_defaults_para_vars_opcionales(
        self, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Si LLM_MODEL y APP_LANGUAGE no están, usa los defaults."""
        monkeypatch.setenv("ANTHROPIC_API_KEY", "sk-ant-test-123")
        monkeypatch.setenv("DATABASE_URL", "postgresql://u:p@h:5432/d")
        monkeypatch.delenv("LLM_MODEL", raising=False)
        monkeypatch.delenv("APP_LANGUAGE", raising=False)

        config = load_config()

        assert config.llm_model == DEFAULT_LLM_MODEL
        assert config.app_language == "es"

    def test_falla_si_falta_api_key(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Sin ANTHROPIC_API_KEY, lanza ValueError con mensaje claro."""
        monkeypatch.delenv("ANTHROPIC_API_KEY", raising=False)
        monkeypatch.setenv("DATABASE_URL", "postgresql://u:p@h:5432/d")

        with pytest.raises(ValueError, match=ERR_MISSING_API_KEY):
            load_config()

    def test_falla_si_falta_database_url(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Sin DATABASE_URL, lanza ValueError con mensaje claro."""
        monkeypatch.setenv("ANTHROPIC_API_KEY", "sk-ant-test-123")
        monkeypatch.delenv("DATABASE_URL", raising=False)

        with pytest.raises(ValueError, match=ERR_MISSING_DATABASE_URL):
            load_config()

    def test_falla_si_var_obligatoria_es_string_vacio(
        self, monkeypatch: pytest.MonkeyPatch
    ) -> None:
        """Si una var obligatoria es '' o solo espacios, lanza ValueError."""
        monkeypatch.setenv("ANTHROPIC_API_KEY", "   ")
        monkeypatch.setenv("DATABASE_URL", "postgresql://u:p@h:5432/d")

        with pytest.raises(ValueError, match=ERR_MISSING_API_KEY):
            load_config()

    def test_falla_si_idioma_invalido(self, monkeypatch: pytest.MonkeyPatch) -> None:
        """Si APP_LANGUAGE no es 'es' ni 'en', lanza ValueError."""
        monkeypatch.setenv("ANTHROPIC_API_KEY", "sk-ant-test-123")
        monkeypatch.setenv("DATABASE_URL", "postgresql://u:p@h:5432/d")
        monkeypatch.setenv("APP_LANGUAGE", "fr")

        with pytest.raises(ValueError) as exc_info:
            load_config()
        assert ERR_INVALID_LANGUAGE.format(value="fr") in str(exc_info.value)
