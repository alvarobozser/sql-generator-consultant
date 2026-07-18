"""Cliente LLM: wrapper sobre el SDK de Anthropic.

El cliente:
- Usa el SDK oficial `anthropic` con la API Messages.
- Limpia la respuesta (quita bloques de markdown ```sql ... ```).
- Maneja errores de la API envolviéndolos en `LLMError` (no expone stack traces crudos).
- Lanza `LLMError` si la respuesta viene vacía.

Para usar otro proveedor (OpenAI, MiniMax, Ollama, etc.) solo habría que
cambiar esta clase: el resto del código (orquestador, UI) usa la misma
interfaz `LLMClient.generate_sql(question, system_prompt) -> str`.
"""

from __future__ import annotations

import re

from anthropic import Anthropic

# Mensajes de error estandarizados.
ERR_EMPTY_RESPONSE = "El LLM devolvió una respuesta vacía"
ERR_API_CALL_FAILED = "Error llamando a la API de Anthropic"

# Modelo por defecto. claude-haiku-4-5 es rápido, barato y muy bueno en SQL.
DEFAULT_MODEL = "claude-haiku-4-5"


class LLMError(Exception):
    """Error al comunicarse con el LLM."""


def _clean_sql_response(raw: str | None) -> str:
    """Limpia la respuesta del LLM para quedarse solo con el SQL.

    El LLM a veces envuelve el SQL en bloques markdown:
        ```sql
        SELECT * FROM users;
        ```
    Otras veces añade prosa antes o después. Esta función extrae el SQL
    puro, sin markdown, sin punto y coma final.

    Args:
        raw: Texto crudo devuelto por el LLM (puede ser None o vacío).

    Returns:
        SQL limpio, sin markdown, sin espacios extra, sin ';' final.
    """
    if not raw:
        return ""

    text = raw.strip()

    # Quitar bloques de markdown: ```sql ... ``` o ``` ... ```.
    match = re.search(r"```(?:sql)?\s*(.*?)\s*```", text, flags=re.DOTALL | re.IGNORECASE)
    if match:
        text = match.group(1).strip()

    # Quitar ';' final si lo tiene.
    if text.endswith(";"):
        text = text[:-1].strip()

    return text


class LLMClient:
    """Cliente para Anthropic Claude.

    Attributes:
        model: Identificador del modelo (ej. 'claude-haiku-4-5').
    """

    def __init__(self, api_key: str, model: str = DEFAULT_MODEL) -> None:
        """Inicializa el cliente.

        Args:
            api_key: API key de Anthropic (formato 'sk-ant-...').
            model: Nombre del modelo a usar. Por defecto claude-haiku-4-5.

        Raises:
            ValueError: Si api_key o model están vacíos.
        """
        if not api_key or not api_key.strip():
            raise ValueError("api_key no puede estar vacío")
        if not model or not model.strip():
            raise ValueError("model no puede estar vacío")

        self._client = Anthropic(api_key=api_key.strip())
        self.model = model.strip()

    def generate_sql(self, question: str, system_prompt: str, max_tokens: int = 1024) -> str:
        """Genera SQL a partir de una pregunta en lenguaje natural.

        Args:
            question: Pregunta del usuario en lenguaje natural.
            system_prompt: System prompt completo (incluye schema y reglas).
            max_tokens: Tope de tokens para la respuesta. Por defecto 1024 (sobra para SQL).

        Returns:
            SQL limpio, listo para validar y ejecutar.

        Raises:
            LLMError: Si la API falla o devuelve respuesta vacía.
            ValueError: Si `question` o `system_prompt` están vacíos.
        """
        if not question or not question.strip():
            raise ValueError("La pregunta no puede estar vacía")
        if not system_prompt or not system_prompt.strip():
            raise ValueError("El system_prompt no puede estar vacío")

        try:
            response = self._client.messages.create(
                model=self.model,
                max_tokens=max_tokens,
                system=system_prompt,
                messages=[{"role": "user", "content": question.strip()}],
                temperature=0.0,  # Determinismo: misma pregunta -> mismo SQL.
            )
        except Exception as e:
            # Envolver cualquier error de la API (red, auth, rate limit, etc.)
            # en LLMError con mensaje claro, sin exponer el stack trace crudo.
            raise LLMError(f"{ERR_API_CALL_FAILED}: {e}") from e

        # Extraer contenido de la respuesta.
        # La estructura de Anthropic es response.content -> list[ContentBlock].
        # Cada bloque tiene .text si es de tipo 'text'.
        try:
            blocks = response.content
            raw_content = "".join(
                block.text for block in blocks if hasattr(block, "text")
            )
        except (AttributeError, TypeError) as e:
            raise LLMError(
                f"{ERR_API_CALL_FAILED}: respuesta con formato inesperado"
            ) from e

        cleaned = _clean_sql_response(raw_content)
        if not cleaned:
            raise LLMError(ERR_EMPTY_RESPONSE)

        return cleaned
