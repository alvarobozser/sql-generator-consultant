"""Tests unitarios para src.prompts.templates."""

from __future__ import annotations

import pytest

from src.prompts.templates import (
    SCHEMA_DESCRIPTION,
    SUPPORTED_LANGUAGES,
    get_schema_description,
    get_system_prompt,
)


class TestGetSystemPrompt:
    """Tests para get_system_prompt()."""

    def test_prompt_en_espanol_contiene_las_4_tablas(self) -> None:
        """El prompt en ES menciona las 4 tablas del schema."""
        prompt = get_system_prompt("es")
        for table in ("users", "products", "orders", "order_items"):
            assert table in prompt, f"Falta la tabla '{table}' en el prompt ES"

    def test_prompt_en_ingles_contiene_las_4_tablas(self) -> None:
        """El prompt en EN menciona las 4 tablas del schema."""
        prompt = get_system_prompt("en")
        for table in ("users", "products", "orders", "order_items"):
            assert table in prompt, f"Falta la tabla '{table}' en el prompt EN"

    def test_ambos_prompts_mencionan_solo_lectura(self) -> None:
        """Ambos prompts explicitan que solo se permiten queries de lectura."""
        es = get_system_prompt("es")
        en = get_system_prompt("en")
        # ES: "solo lectura" o equivalente; EN: "read-only".
        assert "SOLO LECTURA" in es.upper() or "READ-ONLY" in es.upper()
        assert "READ-ONLY" in en.upper() or "SOLO LECTURA" in en.upper()

    def test_ambos_prompts_mencionan_keywords_prohibidas(self) -> None:
        """Ambos prompts listan al menos INSERT/UPDATE/DELETE/DROP como prohibidas."""
        for lang in ("es", "en"):
            prompt = get_system_prompt(lang).upper()
            for kw in ("INSERT", "UPDATE", "DELETE", "DROP"):
                assert kw in prompt, f"Falta keyword '{kw}' en prompt '{lang}'"

    def test_prompt_en_tiene_ejemplos_few_shot(self) -> None:
        """Cada idioma tiene al menos 2 ejemplos few-shot (Pregunta/Question + SQL)."""
        es = get_system_prompt("es")
        en = get_system_prompt("en")
        # Cada ejemplo tiene al menos 1 "SQL:" y 1 "Pregunta:" (ES) o "Question:" (EN).
        assert es.count("SQL:") >= 2
        assert es.count("Pregunta:") >= 2
        assert en.count("SQL:") >= 2
        assert en.count("Question:") >= 2

    def test_prompt_en_espanol_tiene_reglas_en_espanol(self) -> None:
        """El prompt ES está mayormente en español (heurística simple)."""
        prompt = get_system_prompt("es").lower()
        # Palabras comunes en español que deberían aparecer.
        assert "pregunta" in prompt
        assert "reglas" in prompt

    def test_idioma_no_soportado_lanza_ValueError(self) -> None:
        """Si el idioma no es 'es' ni 'en', lanza ValueError claro."""
        with pytest.raises(ValueError) as exc_info:
            get_system_prompt("fr")
        assert "Idioma no soportado" in str(exc_info.value)
        assert "fr" in str(exc_info.value)

    def test_idioma_vacio_lanza_ValueError(self) -> None:
        """Idioma vacío también falla."""
        with pytest.raises(ValueError):
            get_system_prompt("")


class TestGetSchemaDescription:
    """Tests para get_schema_description()."""

    def test_devuelve_string_no_vacio(self) -> None:
        """La descripción del schema es un string no vacío."""
        desc = get_schema_description()
        assert isinstance(desc, str)
        assert len(desc) > 100  # tiene que ser sustanciosa

    def test_contiene_las_4_tablas(self) -> None:
        """La descripción menciona las 4 tablas."""
        desc = get_schema_description()
        for table in ("users", "products", "orders", "order_items"):
            assert table in desc

    def test_coincide_con_la_constante(self) -> None:
        """get_schema_description() devuelve SCHEMA_DESCRIPTION sin espacios extra."""
        assert get_schema_description() == SCHEMA_DESCRIPTION.strip()


class TestSupportedLanguages:
    """Tests para la constante SUPPORTED_LANGUAGES."""

    def test_solo_es_y_en(self) -> None:
        """Por ahora solo soportamos español e inglés."""
        assert SUPPORTED_LANGUAGES == ("es", "en")
