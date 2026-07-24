"""Submódulo de prompts (templates para el LLM)."""

from .templates import SUPPORTED_LANGUAGES, get_schema_description, get_system_prompt

__all__ = ["SUPPORTED_LANGUAGES", "get_schema_description", "get_system_prompt"]
