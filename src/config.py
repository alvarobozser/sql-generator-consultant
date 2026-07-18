"""Carga de configuración desde variables de entorno o archivo .env.

Este módulo centraliza el acceso a las variables de configuración.
Las variables se leen en este orden de prioridad:
  1. Variables de entorno del sistema (útil para CI/CD y producción).
  2. Archivo .env en la raíz del proyecto (útil para desarrollo local).
"""

from __future__ import annotations

import os
from dataclasses import dataclass

from dotenv import load_dotenv

# Carga .env solo si existe. Si no, no falla (útil en CI donde las vars vienen del entorno).
load_dotenv()


# Mensajes de error como constantes para reutilizar y testear.
ERR_MISSING_API_KEY = "MINIMAX_API_KEY no está definida en .env ni en el entorno"
ERR_MISSING_BASE_URL = "MINIMAX_BASE_URL no está definida en .env ni en el entorno"
ERR_MISSING_MODEL = "MINIMAX_MODEL no está definida en .env ni en el entorno"
ERR_MISSING_DATABASE_URL = "DATABASE_URL no está definida en .env ni en el entorno"
ERR_INVALID_LANGUAGE = "APP_LANGUAGE debe ser 'es' o 'en', se recibió: {value}"

# Valores por defecto.
DEFAULT_MINIMAX_MODEL = "opencode-go/MiniMax-M3"
DEFAULT_APP_LANGUAGE = "es"
VALID_LANGUAGES = ("es", "en")


@dataclass(frozen=True)
class AppConfig:
    """Configuración inmutable de la aplicación.

    Attributes:
        minimax_api_key: API key de MiniMax.
        minimax_base_url: URL base de la API de MiniMax.
        minimax_model: Modelo a usar.
        database_url: Cadena de conexión a PostgreSQL.
        app_language: Idioma por defecto ('es' o 'en').
    """

    minimax_api_key: str
    minimax_base_url: str
    minimax_model: str
    database_url: str
    app_language: str


def _get_required(name: str, error_message: str) -> str:
    """Lee una variable de entorno obligatoria o lanza ValueError con mensaje claro."""
    value = os.getenv(name)
    if not value or not value.strip():
        raise ValueError(error_message)
    return value.strip()


def _get_optional(name: str, default: str) -> str:
    """Lee una variable de entorno opcional con valor por defecto."""
    value = os.getenv(name)
    if not value or not value.strip():
        return default
    return value.strip()


def load_config() -> AppConfig:
    """Carga y valida la configuración de la aplicación.

    Returns:
        AppConfig con todos los valores necesarios.

    Raises:
        ValueError: Si falta alguna variable obligatoria o tiene un valor inválido.
    """
    minimax_api_key = _get_required("MINIMAX_API_KEY", ERR_MISSING_API_KEY)
    minimax_base_url = _get_required("MINIMAX_BASE_URL", ERR_MISSING_BASE_URL)
    database_url = _get_required("DATABASE_URL", ERR_MISSING_DATABASE_URL)

    # El modelo es opcional (tiene default).
    minimax_model = _get_optional("MINIMAX_MODEL", DEFAULT_MINIMAX_MODEL)

    # El idioma es opcional pero validado.
    app_language = _get_optional("APP_LANGUAGE", DEFAULT_APP_LANGUAGE)
    if app_language not in VALID_LANGUAGES:
        raise ValueError(ERR_INVALID_LANGUAGE.format(value=app_language))

    return AppConfig(
        minimax_api_key=minimax_api_key,
        minimax_base_url=minimax_base_url,
        minimax_model=minimax_model,
        database_url=database_url,
        app_language=app_language,
    )
