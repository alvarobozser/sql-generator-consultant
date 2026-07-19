# AI SQL Query Generator

> Generador de consultas SQL a partir de lenguaje natural, usando **Anthropic Claude** y **PostgreSQL**.

App Python con interfaz **Streamlit** que convierte preguntas en español o inglés a consultas SQL ejecutables contra una base de datos PostgreSQL de ejemplo (e-commerce). El SQL generado pasa por un validador de seguridad antes de ejecutarse, y los resultados se muestran en una tabla interactiva.

Diseñada con fines didácticos: código claro, modular y bien testeado.

---

## Características

- **Conversión natural → SQL** con Claude (`claude-haiku-4-5` por defecto, configurable)
- **Bilingüe**: español e inglés, con toggle en la sidebar
- **Validación de seguridad**: solo se ejecutan queries de lectura (`SELECT`, `WITH`, `EXPLAIN`)
- **Ejecución real** contra PostgreSQL y resultados en tabla interactiva
- **Manejo de errores** claro en cada capa (LLM, validación, base de datos)
- **91 tests** + lint con `ruff`

---

## Stack

| Componente | Tecnología |
|---|---|
| Lenguaje | Python 3.11+ |
| UI | [Streamlit](https://streamlit.io) 1.40+ |
| IA | [Anthropic Claude](https://anthropic.com) vía SDK `anthropic` |
| BD | PostgreSQL 16 (vía Docker Compose) |
| Driver BD | `psycopg[binary]` 3.2+ |
| Validación SQL | `sqlparse` 0.5+ |
| Config | `python-dotenv` 1.0+ |
| Tests | `pytest` 8+ |
| Linter | `ruff` 0.6+ |

---

## Quick start

### Requisitos previos

- **Python 3.11 o superior**
- **Docker** y **Docker Compose** (para PostgreSQL)
- Una **API key de Anthropic** — consíguela en https://console.anthropic.com

### Instalación paso a paso

```bash
# 1. Clonar el repositorio
git clone https://github.com/alvarobozser/sql-generator-consultant.git
cd sql-generator-consultant

# 2. Crear y activar el entorno virtual
python -m venv .venv
# Windows (PowerShell):
.venv\Scripts\Activate.ps1
# macOS / Linux:
source .venv/bin/activate

# 3. Instalar dependencias
pip install -r requirements.txt

# 4. Configurar variables de entorno
cp .env.example .env        # macOS / Linux
copy .env.example .env      # Windows
# Edita .env y rellena ANTHROPIC_API_KEY con tu key real (formato sk-ant-...)

# 5. Levantar PostgreSQL con Docker
docker compose up -d

# 6. Crear el schema y cargar datos de ejemplo
python -m scripts.seed_db
# Salida esperada:
#   users       : 20
#   products    : 50
#   orders      : 105
#   order_items : 315

# 7. Arrancar la app
streamlit run src/app.py
```

Abre http://localhost:8501 en tu navegador.

### Primer uso

1. En la **sidebar**, selecciona el idioma (Español / Inglés).
2. Opcional: pulsa **"Verificar conexión"** para confirmar que la BD responde.
3. En el área principal, escribe una pregunta en lenguaje natural, por ejemplo:
   - *"¿Cuántos usuarios hay en total?"*
   - *"Dame los 5 productos más caros de electronics"*
   - *"¿Cuánto han gastado los usuarios de España en pedidos delivered?"*
4. Pulsa **"Generar SQL y ejecutar"**.
5. Verás el SQL generado en un bloque de código y los resultados en una tabla.

---

## Estructura del proyecto

```
.
├── AGENTS.md                  # Reglas globales del agente
├── CLAUDE.md                  # Notas para el asistente
├── README.md                  # Este archivo
├── .env.example               # Plantilla de variables de entorno
├── .gitignore
├── docker-compose.yml         # PostgreSQL 16
├── pyproject.toml             # Config de ruff, black, pytest
├── requirements.txt           # Dependencias pinned
├── schema.sql                 # DDL + seed data (e-commerce)
├── scripts/
│   ├── __init__.py
│   └── seed_db.py             # Carga schema.sql en la BD
├── src/
│   ├── __init__.py
│   ├── app.py                 # Entrypoint de Streamlit
│   ├── config.py              # Carga de configuración (.env)
│   ├── orchestrator.py        # Orquesta LLM + validador + BD
│   ├── db/
│   │   ├── __init__.py
│   │   └── connection.py      # get_connection(), execute_query()
│   ├── llm/
│   │   ├── __init__.py
│   │   └── client.py          # LLMClient (Anthropic Claude)
│   ├── prompts/
│   │   ├── __init__.py
│   │   └── templates.py       # System prompts ES/EN con few-shot
│   ├── validators/
│   │   ├── __init__.py
│   │   └── sql_safety.py      # validate_sql()
│   └── ui/
│       ├── __init__.py
│       └── components.py      # Widgets de Streamlit reutilizables
└── tests/
    ├── __init__.py
    ├── test_config.py
    ├── test_db_connection.py
    ├── test_integration_e2e.py
    ├── test_llm_client.py
    ├── test_orchestrator.py
    ├── test_prompts.py
    └── test_sql_safety.py
```

---

## Variables de entorno

Definidas en `.env` (basado en `.env.example`):

| Variable | Obligatoria | Default | Descripción |
|---|---|---|---|
| `ANTHROPIC_API_KEY` | Sí | — | API key de Anthropic (formato `sk-ant-...`) |
| `LLM_MODEL` | No | `claude-haiku-4-5` | Modelo a usar. Otras opciones: `claude-sonnet-4-5`, `claude-opus-4-1` |
| `DATABASE_URL` | Sí | — | Cadena de conexión a PostgreSQL |
| `APP_LANGUAGE` | No | `es` | Idioma por defecto (`es` o `en`) |

**Importante**: `.env` está en `.gitignore`. Nunca lo commitees.

---

## Tests

```bash
# Activar venv primero
pytest -v                  # 91 tests
ruff check src/ tests/     # Linter
```

Los tests de integración (`tests/test_integration_e2e.py`) hacen llamadas **reales** a la API de Anthropic y se saltan automáticamente si no hay `ANTHROPIC_API_KEY` configurada.

Para ejecutarlos:

```bash
# PowerShell
$env:ANTHROPIC_API_KEY = "sk-ant-..."
pytest tests/test_integration_e2e.py -v -s
```

> **Nota**: estos tests consumen tokens reales (pocos céntimos por ejecución).

---

## Troubleshooting

### La app no arranca: "ANTHROPIC_API_KEY no está definida"

Crea el archivo `.env` copiando `.env.example` y rellena la variable.

### "Error de base de datos: connection refused"

PostgreSQL no está corriendo. Ejecuta:

```bash
docker compose up -d
docker compose ps   # debe mostrar 'healthy'
```

### "La API devolvió 401" o error de autenticación de Anthropic

Tu `ANTHROPIC_API_KEY` es inválida o está revocada. Genera una nueva en https://console.anthropic.com.

### Quiero cambiar el modelo (de Haiku a Sonnet o Opus)

Edita `.env`:

```env
LLM_MODEL=claude-sonnet-4-5
```

Sonnet es más capaz pero más caro (~5x Haiku). Opus es el más potente (~25x Haiku).

### Quiero resetear los datos de ejemplo

```bash
python -m scripts.seed_db
```

El script es idempotente: borra y recrea las tablas cada vez.

### Quiero cambiar las credenciales de PostgreSQL

Edita **dos sitios** (deben coincidir):

1. `docker-compose.yml` — `POSTGRES_USER`, `POSTGRES_PASSWORD`, `POSTGRES_DB`
2. `.env` — `DATABASE_URL`

Después:

```bash
docker compose down -v    # ⚠️ borra los datos
docker compose up -d
python -m scripts.seed_db
```

---

## Esquema de la base de datos

4 tablas relacionadas (e-commerce simplificado):

```
users
├── id (PK)
├── name
├── email (UNIQUE)
├── country
└── created_at

products
├── id (PK)
├── name
├── category      -- electronics | home | sports | books
├── price
├── stock
└── created_at

orders
├── id (PK)
├── user_id (FK → users)
├── status        -- pending | paid | shipped | delivered | cancelled
├── total
└── created_at

order_items
├── id (PK)
├── order_id (FK → orders)
├── product_id (FK → products)
├── quantity
└── unit_price
```

---

## Seguridad

- **API key**: solo en `.env` (ignorado por git). Rótalo periódicamente.
- **SQL inyectado**: defense in depth con parser AST + regex de blacklist.
- **Solo lectura**: la app **jamás** ejecuta `INSERT`, `UPDATE`, `DELETE`, `DROP`, etc. El validador bloquea 18 keywords peligrosas.
- **No exposición de secretos en la UI**: la URL de la BD se enmascara antes de mostrarse.

---

## Licencia

MIT — libre uso, modificación y distribución.
