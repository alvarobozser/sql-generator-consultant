"""App principal de Streamlit: AI SQL Query Generator.

Arranca con: streamlit run src/app.py
"""

from __future__ import annotations

import streamlit as st

from src.config import load_config
from src.llm.client import LLMClient
from src.orchestrator import process_question
from src.ui.components import (
    render_error,
    render_results,
    render_sidebar,
    render_sql,
    t,
)


@st.cache_resource(show_spinner=False)
def get_llm_client(api_key: str, model: str) -> LLMClient:
    """Crea y cachea el cliente LLM (se reutiliza entre reruns)."""
    return LLMClient(api_key=api_key, model=model)


def main() -> None:
    """Punto de entrada de la app."""
    # Carga config (falla rapido si falta algo).
    try:
        config = load_config()
    except ValueError as e:
        st.error(f"Error de configuracion: {e}")
        st.info(
            "Asegurate de tener un archivo .env con ANTHROPIC_API_KEY, "
            "DATABASE_URL y APP_LANGUAGE. Copia .env.example a .env como base."
        )
        st.stop()

    # Configuracion de la pagina.
    st.set_page_config(
        page_title="AI SQL Query Generator",
        page_icon=":bar_chart:",
        layout="wide",
    )

    # Sidebar: idioma + estado BD.
    language = render_sidebar(config.app_language, config.database_url)

    # Encabezado principal.
    st.title(t("title", language))
    st.caption(t("subtitle", language))

    # Input de pregunta.
    question = st.text_area(
        t("your_question", language),
        placeholder=t("question_placeholder_es" if language == "es" else "question_placeholder_en", language),
        height=100,
        key="question_input",
    )

    # Boton de submit.
    submitted = st.button(t("submit", language), type="primary", use_container_width=True)

    # Procesar la pregunta.
    if submitted and question.strip():
        llm_client = get_llm_client(config.anthropic_api_key, config.llm_model)
        with st.spinner(t("thinking", language)):
            result = process_question(
                question=question,
                language=language,
                llm_client=llm_client,
                database_url=config.database_url,
            )

        # Mostrar SQL generado (incluso si hubo error, para que el usuario vea que paso).
        if result.sql:
            render_sql(result.sql, language)

        # Mostrar resultado o error.
        if result.success:
            render_results(result.rows or [], language)
        else:
            render_error(result.error or "Error desconocido", language)

    # Footer.
    st.divider()
    st.caption(t("footer", language))


if __name__ == "__main__":
    main()
