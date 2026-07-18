"""Test de integración end-to-end: LLM -> validador -> BD.

Este test hace una llamada REAL a la API de Anthropic. Se salta
automáticamente si no hay ANTHROPIC_API_KEY válida en el entorno.

Para ejecutarlo:
    export ANTHROPIC_API_KEY=sk-ant-...    # Linux/macOS
    $env:ANTHROPIC_API_KEY = "sk-ant-..."   # PowerShell
    pytest tests/test_integration_e2e.py -v

Este test consume tokens reales (unos pocos céntimos por ejecución).
NO se ejecuta en CI por defecto.
"""

from __future__ import annotations

import os

import pytest

from src.config import load_config
from src.db.connection import execute_query
from src.llm.client import LLMClient
from src.prompts.templates import get_system_prompt
from src.validators.sql_safety import validate_sql

# Skip si no hay key real configurada.
pytestmark = pytest.mark.skipif(
    not os.getenv("ANTHROPIC_API_KEY") or "your_key_here" in os.getenv("ANTHROPIC_API_KEY", ""),
    reason="Requiere ANTHROPIC_API_KEY real en el entorno",
)


@pytest.fixture
def config():
    """Carga la config real. Falla el test si no está bien."""
    return load_config()


@pytest.fixture
def llm_client(config):
    """Cliente LLM real."""
    return LLMClient(api_key=config.anthropic_api_key, model=config.llm_model)


class TestEndToEnd:
    """Tests E2E con API real."""

    def test_flujo_completo_pregunta_a_resultados_en_espanol(self, llm_client, config) -> None:
        """Del pregunta en español hasta filas de la BD, todo el flujo funciona."""
        pregunta = "¿Cuántos usuarios hay en total?"
        system_prompt = get_system_prompt("es")

        # 1. LLM genera SQL
        sql = llm_client.generate_sql(pregunta, system_prompt)
        assert sql, "El LLM no devolvió SQL"
        print(f"\n[SQL generado]: {sql}")

        # 2. Validador lo acepta
        validate_sql(sql)  # debe pasar sin excepción

        # 3. BD lo ejecuta y devuelve resultados
        rows = execute_query(sql, config.database_url)
        assert len(rows) >= 1
        # El resultado debe tener al menos una columna numérica
        first_row = rows[0]
        assert any(isinstance(v, int) for v in first_row.values()), (
            f"Se esperaba un conteo numérico, se obtuvo: {first_row}"
        )

    def test_flujo_completo_en_ingles(self, llm_client, config) -> None:
        """El flujo también funciona en inglés."""
        pregunta = "How many products are in the books category?"
        system_prompt = get_system_prompt("en")

        sql = llm_client.generate_sql(pregunta, system_prompt)
        print(f"\n[SQL generated]: {sql}")

        validate_sql(sql)
        rows = execute_query(sql, config.database_url)
        assert len(rows) >= 1

    def test_query_con_agregacion_y_filtro(self, llm_client, config) -> None:
        """Una pregunta más compleja: agregación + filtro + agrupación."""
        pregunta = "¿Cuánto ha gastado cada usuario de España en pedidos delivered? Dame el top 3."
        system_prompt = get_system_prompt("es")

        sql = llm_client.generate_sql(pregunta, system_prompt)
        print(f"\n[SQL generado]: {sql}")

        validate_sql(sql)
        rows = execute_query(sql, config.database_url)
        # El resultado puede tener entre 0 y 3 filas (depende de los datos)
        assert len(rows) <= 3
