# AI SQL Query Generator

> Generador de consultas SQL a partir de lenguaje natural, usando **Anthropic Claude** y **PostgreSQL**.

Este repositorio contiene **dos implementaciones independientes** del mismo producto, que comparten la base de datos y la API key de Claude:

| Implementacion | Stack | Carpeta | Issue |
|---|---|---|---|
| **Python** | Python 3.11+ / Streamlit / psycopg | [`sql-gen-python/`](sql-gen-python/) | #1 |
| **Java + Angular** | Java 21 / Spring Boot 3.3 / Angular 19 | [`sql-gen-spring-angular/`](sql-gen-spring-angular/) | #2 |

Ambas hacen lo mismo: reciben una pregunta en lenguaje natural, la pasan a Claude para que genere SQL, validan que el SQL sea seguro (solo lectura), lo ejecutan contra PostgreSQL y devuelven los resultados.

---

## Estructura del repo

```
nproject/
├── README.md                       # Este archivo
├── AGENTS.md                       # Reglas globales del agente
├── CLAUDE.md
├── .env.example                    # Plantilla de variables de entorno
├── .gitignore
├── docker-compose.yml              # PostgreSQL 16
│
├── db/                             # Recursos compartidos de BD
│   ├── README.md
│   ├── schema.sql                  # Schema + seed data
│   └── init.sql                    # Crea usuario readonly
│
├── sql-gen-python/                 # Version Python (Issue #1)
│   ├── README.md                   # Guia especifica
│   ├── src/                        # Codigo Python
│   ├── tests/                      # 91 tests
│   ├── scripts/seed_db.py
│   ├── requirements.txt
│   └── pyproject.toml
│
├── sql-gen-spring-angular/         # Version Java + Angular (Issue #2)
│   ├── README.md                   # Guia especifica
│   ├── java/                       # Backend Spring Boot
│   │   ├── src/main/java/com/sqlgen/
│   │   │   ├── domain/             # Java puro (hexagonal)
│   │   │   ├── application/
│   │   │   └── infrastructure/
│   │   ├── src/main/resources/
│   │   ├── src/test/               # 95 tests
│   │   └── pom.xml
│   └── frontend/                   # Angular 19 SPA
│       ├── proxy.conf.json         # /api/* -> localhost:8080
│       └── src/app/
│
└── .harness/                       # Config del agente (no tocar)
    ├── agents.md
    ├── research/
    ├── tech/
    └── memory/
```

---

## Stack

### Compartido

- **PostgreSQL 16** via Docker Compose
- **Schema compartido** (`db/schema.sql`): 4 tablas e-commerce
- **Anthropic API** (Claude Haiku 4.5 por defecto)
- **API key unica** (`ANTHROPIC_API_KEY` en `.env`)

### Version Python (sql-gen-python/)

- Python 3.11+ con venv
- Streamlit para UI
- psycopg para Postgres
- SDK `anthropic` para Claude
- sqlparse para validacion SQL
- pytest + ruff para tests/lint
- 91 tests

### Version Java + Angular (sql-gen-spring-angular/)

- **Backend**: Java 21 LTS, Spring Boot 3.3.5, Maven 3.9+
- **AI**: HTTP directo a Anthropic (via `RestClient` de Spring)
- **BD**: JDBC nativo + JSqlParser para validacion
- **Arquitectura**: Hexagonal estricta (domain puro, application, infrastructure)
- **Tests**: JUnit 5 + Mockito + AssertJ (95 tests)
- **Frontend**: Angular 19 (standalone, signals, signals-based reactivity)
- **HTTP**: Angular HttpClient + dev proxy
- 95 tests

---

## Quick start (comun)

```bash
# 1. Clonar
git clone https://github.com/alvarobozser/sql-generator-consultant.git
cd sql-generator-consultant

# 2. Configurar variables de entorno
cp .env.example .env
# Edita .env y rellena ANTHROPIC_API_KEY con tu key real (formato sk-ant-...)

# 3. Levantar PostgreSQL
docker compose up -d

# 4. Cargar schema + datos de ejemplo
python -m sql-gen-python.scripts.seed_db
# Salida:
#   users       : 20
#   products    : 50
#   orders      : 105
#   order_items : 315
```

A partir de aqui, elige una implementacion:

### Opcion A - Python (Streamlit)

```bash
cd sql-gen-python
python -m venv .venv
.venv\Scripts\activate            # Windows
source .venv/bin/activate          # macOS/Linux
pip install -r requirements.txt

streamlit run src/app.py
# Abre http://localhost:8501
```

### Opcion B - Java (Spring Boot) + Angular

```bash
# Terminal 1: Backend (puerto 8080)
cd sql-gen-spring-angular/java
mvn clean test          # ejecutar tests
mvn spring-boot:run     # arrancar
# curl http://localhost:8080/api/v1/health

# Terminal 2: Frontend (puerto 4200)
cd sql-gen-spring-angular/frontend
npm install
npm start
# Abre http://localhost:4200
```

---

## Variables de entorno

En `.env` (en la raiz del repo):

| Variable | Obligatoria | Usada por |
|---|---|---|
| `ANTHROPIC_API_KEY` | Si | Ambas |
| `DATABASE_URL` | Si | Python (psycopg) |
| `JAVA_DATASOURCE_URL` | Si | Java (Spring Boot) |
| `JAVA_DATASOURCE_USERNAME` | Si | Java |
| `JAVA_DATASOURCE_PASSWORD` | Si | Java |
| `JAVA_DATASOURCE_READONLY_USERNAME` | Si | Java (usuario SQL seguro) |
| `JAVA_DATASOURCE_READONLY_PASSWORD` | Si | Java |
| `LLM_MODEL` | No | Ambas (default: `claude-haiku-4-5`) |
| `APP_LANGUAGE` | No | Python (default: `es`) |

**Importante**: `.env` esta en `.gitignore`. Nunca lo commitees.

---

## API REST (Java/Angular)

Con el backend Java corriendo:

| Metodo | Path | Descripcion |
|---|---|---|
| `GET`  | `/api/v1/health` | Health check |
| `POST` | `/api/v1/queries` | Hacer pregunta en lenguaje natural |
| `GET`  | `/api/v1/schema` | Schema de la BD (tablas + columnas) |
| `GET`  | `/v3/api-docs` | OpenAPI JSON |
| `GET`  | `/swagger-ui.html` | Swagger UI |

### Ejemplo

```bash
curl -X POST http://localhost:8080/api/v1/queries \
  -H "Content-Type: application/json" \
  -d '{"question":"Cuantos usuarios hay de Espana?","language":"es"}'
```

Respuesta:

```json
{
  "sql": "SELECT COUNT(*) FROM users WHERE country = 'Spain'",
  "rows": [{"count": 7}],
  "columns": ["count"],
  "error": null
}
```

---

## Tests

```bash
# Python (91 tests)
cd sql-gen-python
pytest -q
ruff check .

# Java (95 tests)
cd sql-gen-spring-angular/java
mvn test

# Angular (build verification)
cd sql-gen-spring-angular/frontend
npm run build
```

**Total: 186 tests automatizados** (91 Python + 95 Java) + verificacion de build para Angular.

---

## Arquitectura hexagonal (version Java)

```
com.sqlgen/
├── domain/                          # Java puro (sin Spring)
│   ├── model/                       # Records: Question, QueryResult, etc.
│   ├── port/in/ProcessQuestionUseCase
│   ├── port/out/                    # Interfaces
│   │   ├── LlmPort, DatabasePort, SchemaPort, PromptPort, SqlSafetyValidatorPort
│   └── exception/
│
├── application/                     # Orquesta use cases
│   ├── service/ProcessQuestionService
│   └── dto/
│
└── infrastructure/                  # Detalles tecnicos
    ├── config/                       # Beans de Spring
    ├── ai/                           # AnthropicLlmAdapter (HTTP directo)
    ├── persistence/                  # JdbcDatabaseAdapter, JdbcSchemaAdapter
    ├── validation/                   # JSqlParserSqlSafetyValidator
    ├── i18n/                         # ResourceBundlePromptAdapter
    └── web/                          # Controllers REST
```

**Reglas**:
- `domain/` no importa nada de Spring, JDBC ni del SDK de Anthropic
- `application/` solo importa de `domain/`
- `infrastructure/` importa de `application/` y `domain/`, pero nunca al reves

---

## Seguridad

- **API key** solo en `.env` (ignorado por git). Rotar periodicamente.
- **SQL inyectado**: defense in depth con parser AST + blacklist de 18 keywords prohibidas.
- **Solo lectura en BD** (version Java): usuario `sqlgen_readonly` con permisos solo-SELECT, aunque el validador falle.
- **No exposicion de secretos en la UI**: la URL de la BD se enmascara antes de mostrarse.
- **Auditoria de seguridad hecha**: cero secretos en repo, cero patrones peligrosos en codigo.

---

## Troubleshooting

### "ANTHROPIC_API_KEY no esta definida"
Crea `.env` copiando `.env.example` y rellena la variable.

### "connection refused" en el frontend Angular
Verifica que el backend Spring Boot este corriendo en `8080`:
```bash
curl http://localhost:8080/api/v1/health
```

### "connection refused" en la version Python
Verifica que PostgreSQL este corriendo:
```bash
docker compose ps
# Debe mostrar el contenedor sqlgen-postgres como "healthy"
```

### Quiero resetear los datos de la BD
```bash
docker compose down -v
docker compose up -d
python -m sql-gen-python.scripts.seed_db
```

### Quiero cambiar el modelo de Claude
En `.env`:
```env
LLM_MODEL=claude-sonnet-4-5
```

Sonnet es mas capaz pero mas caro (~5x Haiku). Opus es el mas potente (~25x).

---

## Licencia

MIT - libre uso, modificacion y distribucion.
