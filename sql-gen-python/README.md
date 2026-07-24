# sql-gen-python (Issue #1)

> Version Python del AI SQL Query Generator.

App Python con Streamlit que convierte preguntas en lenguaje natural a SQL
ejecutables contra una base de datos PostgreSQL de ejemplo (e-commerce).
Usa Anthropic Claude (Haiku 4.5) como motor de IA.

## Quick start

Desde la **raiz del repo** (no desde aqui):

```bash
# 1. Levantar Postgres
docker compose up -d

# 2. Cargar schema + datos
python -m sql-gen-python.scripts.seed_db

# 3. Instalar dependencias de este proyecto
cd sql-gen-python
python -m venv .venv
.venv\Scripts\activate            # Windows
# source .venv/bin/activate      # macOS/Linux
pip install -r requirements.txt

# 4. Configurar .env en la RAIZ del repo
#    (../.env, con ANTHROPIC_API_KEY y DATABASE_URL)

# 5. Arrancar Streamlit
streamlit run src/app.py
# Abre http://localhost:8501
```

## Estructura

```
sql-gen-python/
├── src/
│   ├── app.py               # Entry point Streamlit
│   ├── config.py            # Carga .env, valida vars
│   ├── orchestrator.py      # Orquesta LLM + validador + BD
│   ├── db/                  # psycopg (Postgres)
│   ├── llm/                 # AnthropicLlmClient (SDK anthropic)
│   ├── prompts/             # System prompts ES/EN con few-shot
│   ├── ui/                  # Componentes Streamlit
│   └── validators/          # sql_safety (Python)
├── tests/                   # 91 tests (pytest)
├── scripts/seed_db.py       # Carga db/schema.sql en la BD
├── requirements.txt
├── pyproject.toml           # Config de ruff
└── .streamlit/config.toml   # Config de Streamlit
```

## Comandos utiles

```bash
# Tests
pytest -q
pytest -q -m integration   # solo tests E2E (requieren API key)

# Linter
ruff check .
ruff format .

# Linter + tests
ruff check . && pytest -q
```

## Variables de entorno

Las variables se leen desde `../.env` (raiz del repo) gracias a `python-dotenv`.
Las que usa este proyecto:

- `ANTHROPIC_API_KEY` (obligatoria) - formato `sk-ant-...`
- `DATABASE_URL` (obligatoria) - formato `postgresql://user:pass@host:5432/db`
- `LLM_MODEL` (opcional, default `claude-haiku-4-5`)
- `APP_LANGUAGE` (opcional, default `es`)

## Features

- Preguntas en espanol o ingles
- Validacion de SQL (solo SELECT/WITH/EXPLAIN)
- Ejecucion real contra PostgreSQL
- Tabla de resultados interactiva
- 91 tests automatizados

## Documentacion adicional

Ver `../README.md` (raiz del repo) para el contexto completo del proyecto,
incluyendo la version Java/Spring Boot equivalente.
