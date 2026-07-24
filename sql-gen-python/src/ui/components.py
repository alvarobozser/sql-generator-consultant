"""Componentes reutilizables de la UI de Streamlit.

Estos widgets encapsulan la presentacion de la app:
- LABELS: textos traducibles (es/en) en un solo lugar.
- mask_database_url: oculta el password de la URL para mostrarla en pantalla.
- check_db_connection: hace un SELECT 1 rapido para validar conectividad.
- render_sidebar: dibuja la sidebar con toggle de idioma y estado de la BD.
- render_sql: muestra el SQL generado en un bloque de codigo con syntax highlight.
- render_results: muestra los resultados en una tabla interactiva.
- render_error: muestra un error con icono.
"""

from __future__ import annotations

import re
from typing import Any

import streamlit as st

from src.db.connection import DatabaseError, execute_query

# Textos de la UI en ambos idiomas. Centralizados para facilitar cambios.
LABELS: dict[str, dict[str, str]] = {
    "es": {
        "title": "AI SQL Query Generator",
        "subtitle": "Convierte preguntas en lenguaje natural a SQL con Claude",
        "language": "Idioma",
        "spanish": "Español",
        "english": "Inglés",
        "db_status": "Estado de la base de datos",
        "db_connected": "Conectado",
        "db_disconnected": "No conectado",
        "db_check_button": "Verificar conexión",
        "db_url": "URL de la BD",
        "your_question": "Tu pregunta en lenguaje natural",
        "question_placeholder_es": "p.ej. ¿Cuántos usuarios hay de España?",
        "question_placeholder_en": "e.g. How many users are from Spain?",
        "submit": "Generar SQL y ejecutar",
        "thinking": "Pensando...",
        "generated_sql": "SQL generado",
        "results": "Resultados",
        "rows_count": "filas",
        "no_results": "La consulta no devolvió filas.",
        "error": "Error",
        "examples_title": "Ejemplos para probar",
        "example_1_es": "¿Cuántos usuarios hay en total?",
        "example_2_es": "Dame los 5 productos más caros de 'electronics'",
        "example_3_es": "¿Cuánto han gastado en total los usuarios de España?",
        "example_1_en": "How many users are there in total?",
        "example_2_en": "Show me the 5 most expensive products in 'electronics'",
        "example_3_en": "How much have users from Spain spent in total?",
        "footer": "Powered by Anthropic Claude + PostgreSQL",
    },
    "en": {
        "title": "AI SQL Query Generator",
        "subtitle": "Convert natural language questions to SQL with Claude",
        "language": "Language",
        "spanish": "Spanish",
        "english": "English",
        "db_status": "Database status",
        "db_connected": "Connected",
        "db_disconnected": "Disconnected",
        "db_check_button": "Check connection",
        "db_url": "Database URL",
        "your_question": "Your question in natural language",
        "question_placeholder_es": "e.g. How many users are from Spain?",
        "question_placeholder_en": "e.g. How many users are from Spain?",
        "submit": "Generate SQL and execute",
        "thinking": "Thinking...",
        "generated_sql": "Generated SQL",
        "results": "Results",
        "rows_count": "rows",
        "no_results": "The query returned no rows.",
        "error": "Error",
        "examples_title": "Examples to try",
        "example_1_es": "How many users are there in total?",
        "example_2_es": "Show me the 5 most expensive products in 'electronics'",
        "example_3_es": "How much have users from Spain spent in total?",
        "example_1_en": "How many users are there in total?",
        "example_2_en": "Show me the 5 most expensive products in 'electronics'",
        "example_3_en": "How much have users from Spain spent in total?",
        "footer": "Powered by Anthropic Claude + PostgreSQL",
    },
}


def t(key: str, language: str) -> str:
    """Helper para obtener un label traducido."""
    return LABELS[language].get(key, key)


def mask_database_url(url: str) -> str:
    """Oculta el password de una URL de BD para mostrarla en pantalla.

    postgresql://user:secret@host:5432/db  ->
    postgresql://user:***@host:5432/db
    """
    return re.sub(r"://([^:]+):([^@]+)@", r"://\1:***@", url)


def check_db_connection(database_url: str) -> tuple[bool, str]:
    """Hace un SELECT 1 contra la BD. Devuelve (ok, mensaje).

    Returns:
        (True, "ok") si conecta; (False, mensaje_error) si falla.
    """
    try:
        rows = execute_query("SELECT 1 AS ok", database_url)
        if rows and rows[0].get("ok") == 1:
            return True, "ok"
        return False, "unexpected response from DB"
    except DatabaseError as e:
        return False, str(e)
    except Exception as e:
        return False, f"{type(e).__name__}: {e}"


def render_sidebar(language: str, database_url: str) -> str:
    """Dibuja la sidebar y devuelve el idioma seleccionado.

    Args:
        language: Idioma actual ('es' o 'en').
        database_url: URL de la BD (se muestra enmascarada).

    Returns:
        El nuevo idioma seleccionado ('es' o 'en').
    """
    with st.sidebar:
        st.header(t("title", language))

        # Toggle de idioma.
        new_lang = st.radio(
            t("language", language),
            options=["es", "en"],
            format_func=lambda code: t(code == "es" and "spanish" or "english", language),
            index=0 if language == "es" else 1,
            horizontal=True,
        )

        st.divider()

        # Estado de la BD.
        st.subheader(t("db_status", language))
        if st.button(t("db_check_button", language), use_container_width=True):
            with st.spinner("..."):
                ok, msg = check_db_connection(database_url)
            if ok:
                st.success(f"[OK] {t('db_connected', language)}")
            else:
                st.error(f"[FAIL] {t('db_disconnected', language)}: {msg}")
        st.caption(f"{t('db_url', language)}: `{mask_database_url(database_url)}`")

        st.divider()

        # Ejemplos para inspirarse.
        st.subheader(t("examples_title", language))
        if language == "es":
            st.markdown(f"- {t('example_1_es', language)}")
            st.markdown(f"- {t('example_2_es', language)}")
            st.markdown(f"- {t('example_3_es', language)}")
        else:
            st.markdown(f"- {t('example_1_en', language)}")
            st.markdown(f"- {t('example_2_en', language)}")
            st.markdown(f"- {t('example_3_en', language)}")

    return new_lang


def render_sql(sql: str, language: str) -> None:
    """Muestra el SQL generado en un bloque con syntax highlight."""
    st.subheader(t("generated_sql", language))
    st.code(sql, language="sql")


def render_results(rows: list[dict[str, Any]], language: str) -> None:
    """Muestra los resultados en una tabla interactiva."""
    st.subheader(f"{t('results', language)} ({len(rows)} {t('rows_count', language)})")
    if rows:
        st.dataframe(rows, use_container_width=True, hide_index=True)
    else:
        st.info(t("no_results", language))


def render_error(error: str, language: str) -> None:
    """Muestra un mensaje de error con icono."""
    st.error(f"**{t('error', language)}**: {error}")
