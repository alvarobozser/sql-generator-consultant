# AI SQL Query Generator

> Generador de consultas SQL a partir de lenguaje natural, usando IA.

Una app Python que convierte preguntas en español/inglés a consultas SQL ejecutables, con fines didácticos.

## Estado

🚧 **En desarrollo** — apenas se está inicializando el proyecto.

## Características (planeadas)

- [ ] Convertir lenguaje natural a SQL
- [ ] Interfaz para el usuario (CLI / web)
- [ ] Base de datos de ejemplo para probar
- [ ] Validación del SQL generado
- [ ] Tests unitarios

## Stack

- **Lenguaje:** Python 3.x
- **Proveedor IA:** por definir (OpenAI / Anthropic / Ollama local)
- **BD de ejemplo:** por definir (SQLite / DuckDB)
- **Interfaz:** por definir (CLI / Streamlit / Gradio)

## Quick start

> Placeholder — se completará tras la fase de planificación.

```bash
# Clonar
git clone https://github.com/alvarobozser/sql-generator-consultant.git
cd sql-generator-consultant

# Crear entorno virtual
python -m venv .venv
.venv\Scripts\activate   # Windows
# source .venv/bin/activate  # macOS/Linux

# Instalar dependencias (cuando existan)
pip install -r requirements.txt

# Ejecutar (cuando exista)
python -m src.main
```

## Estructura del proyecto

> Se definirá tras la fase de research + planning.

```
.
├── AGENTS.md            # Reglas globales del agente
├── CLAUDE.md            # Notas del asistente
├── README.md            # Este archivo
├── .gitignore
├── .harness/            # Config del harness de desarrollo (agentes, skills)
└── ...                  # código fuente, tests, etc.
```

## Licencia

Por definir.
