# AI SQL Query Generator

> Generador de consultas SQL a partir de lenguaje natural, usando **Anthropic Claude** y **PostgreSQL**.

App con **dos implementaciones** del mismo producto en este repo:

| Stack | Estado | Carpeta | Issue |
|---|---|---|---|
| **Python** (Streamlit) | ✅ Funcional | `src/`, `tests/` | #1 |
| **Java** (Spring Boot + Angular 19) | ✅ Funcional | `java/`, `frontend/` | #2 |

Ambas comparten la misma base de datos (PostgreSQL vía Docker) y las mismas preguntas en lenguaje natural.

---

## Stack

### Versión Python (Issue #1)

- Python 3.14
- Streamlit
- Anthropic Claude (SDK `anthropic`)
- psycopg
- pytest, ruff

### Versión Java + Angular (Issue #2)

| Componente | Tecnología |
|---|---|
| Backend | Java 21 LTS + Spring Boot 3.3.5 |
| AI | Anthropic Claude (HTTP directo via Spring `RestClient`) |
| BD | PostgreSQL 16 (Driver JDBC nativo) |
| Validación SQL | JSqlParser 4.9 |
| Arquitectura | Hexagonal (domain / application / infrastructure) |
| Tests | JUnit 5 + Mockito + AssertJ + Spring Boot Test |
| Frontend | Angular 19 (standalone, signals) |
| HTTP cliente | Angular HttpClient (vía proxy de dev server) |

### Compartido

- **PostgreSQL 16** vía Docker Compose
- **Schema** (`db/schema.sql`): 4 tablas e-commerce
- **Anthropic API** (misma `ANTHROPIC_API_KEY`)

---

## Quick start

### Requisitos

- Python 3.11+ (para la versión Python)
- Java 21 LTS (para la versión Java)
- Node 20+ y npm 10+ (para Angular)
- Docker + Docker Compose (para PostgreSQL)
- Una **API key de Anthropic** — consíguela en https://console.anthropic.com

### Setup común

```bash
# 1. Clonar el repositorio
git clone https://github.com/alvarobozser/sql-generator-consultant.git
cd sql-generator-consultant

# 2. Configurar variables de entorno
cp .env.example .env
# Edita .env y rellena ANTHROPIC_API_KEY con tu key real (formato sk-ant-...)

# 3. Levantar PostgreSQL
docker compose up -d

# 4. Cargar schema + datos de ejemplo
python -m scripts.seed_db
# Salida:
#   users       : 20
#   products    : 50
#   orders      : 105
#   order_items : 315
```

### Opción A — Python (Streamlit)

```bash
python -m venv .venv
.venv\Scripts\activate            # Windows
source .venv/bin/activate          # macOS/Linux
pip install -r requirements.txt

# Arrancar
streamlit run src/app.py
# Abre http://localhost:8501
```

### Opción B — Java (Spring Boot) + Angular

```bash
# 1. Backend (puerto 8080)
cd java
mvn clean test          # ejecutar tests
mvn spring-boot:run     # arrancar

# En otra terminal:
curl http://localhost:8080/api/v1/health
# {"status":"UP"}

# 2. Frontend (puerto 4200)
cd frontend
npm install
npm start
# Abre http://localhost:4200
```

El frontend tiene un proxy configurado (`proxy.conf.json`) que redirige `/api/*` al backend en `8080`. **No hay problemas de CORS en dev**.

---

## Variables de entorno

En `.env` (basado en `.env.example`):

| Variable | Obligatoria | Usada por |
|---|---|---|
| `ANTHROPIC_API_KEY` | Sí | Python y Java |
| `DATABASE_URL` | Sí | Python (psycopg) |
| `JAVA_DATASOURCE_URL` | Sí | Java (Spring Boot) |
| `JAVA_DATASOURCE_USERNAME` | Sí | Java |
| `JAVA_DATASOURCE_PASSWORD` | Sí | Java |
| `JAVA_DATASOURCE_READONLY_USERNAME` | Sí | Java (usuario SQL seguro) |
| `JAVA_DATASOURCE_READONLY_PASSWORD` | Sí | Java |
| `LLM_MODEL` | No | Ambos (default: `claude-haiku-4-5`) |
| `APP_LANGUAGE` | No | Python (default: `es`) |

**Importante**: `.env` está en `.gitignore`. Nunca lo commitees.

---

## Estructura del proyecto

```
.
├── .env.example
├── .gitignore
├── AGENTS.md                       # Reglas globales del agente
├── CLAUDE.md
├── README.md                       # Este archivo
├── docker-compose.yml              # PostgreSQL 16
│
├── db/                              # Recursos compartidos de BD
│   ├── README.md
│   ├── init.sql                     # Crea usuario sqlgen_readonly
│   └── schema.sql                   # Schema + seed data
│
├── scripts/
│   └── seed_db.py                   # Carga db/schema.sql
│
├── src/                             # Versión Python (Issue #1)
│   ├── app.py                       # Entry point Streamlit
│   ├── config.py
│   ├── orchestrator.py
│   ├── db/
│   ├── llm/
│   ├── prompts/
│   ├── ui/
│   └── validators/
│
├── tests/                           # Tests Python (91)
│
├── java/                            # Versión Java (Issue #2)
│   ├── pom.xml
│   ├── README.md
│   ├── .gitignore
│   └── src/
│       ├── main/
│       │   ├── java/com/sqlgen/
│       │   │   ├── SqlgenApplication.java
│       │   │   ├── domain/        # Java puro (sin Spring)
│       │   │   ├── application/   # Servicios de aplicación
│       │   │   └── infrastructure/ # Spring, JDBC, REST
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── prompts_es.properties
│       │       └── prompts_en.properties
│       └── test/
│           └── java/com/sqlgen/    # 95 tests
│
├── frontend/                        # Versión Angular (Issue #2)
│   ├── package.json
│   ├── angular.json
│   ├── proxy.conf.json             # /api/* -> localhost:8080
│   └── src/
│       └── app/
│           ├── core/                # Servicios singleton
│           └── features/query/      # Componente principal
│
└── .harness/                        # Config del harness (agente SDD)
    ├── agents.md
    ├── agents/
    ├── research/                    # research-plan.md
    ├── tech/                        # tech-plan.md
    └── memory/                      # Estado de sesión
```

---

## API REST (Java/Angular)

| Método | Path | Descripción |
|---|---|---|
| `GET`  | `/api/v1/health` | Health check |
| `POST` | `/api/v1/queries` | Hacer pregunta en lenguaje natural |
| `GET`  | `/api/v1/schema` | Schema de la BD (tablas + columnas) |
| `GET`  | `/v3/api-docs` | OpenAPI JSON |
| `GET`  | `/swagger-ui.html` | Swagger UI |
| `GET`  | `/actuator/health` | Spring Boot Actuator |

### Ejemplo de uso

```bash
curl -X POST http://localhost:8080/api/v1/queries \
  -H "Content-Type: application/json" \
  -d '{"question":"¿Cuántos usuarios hay de España?","language":"es"}'

# Respuesta:
# {
#   "sql": "SELECT COUNT(*) FROM users WHERE country = 'Spain'",
#   "rows": [{"count": 7}],
#   "columns": ["count"],
#   "error": null
# }
```

---

## Arquitectura hexagonal (versión Java)

```
com.sqlgen/
├── domain/                          # Java puro (sin Spring)
│   ├── model/                       # Records: Question, QueryResult, etc.
│   ├── port/in/ProcessQuestionUseCase  # Use case principal
│   ├── port/out/                    # Interfaces (lo que el dominio necesita)
│   │   ├── LlmPort                   # - LlmPort (genera SQL)
│   │   ├── DatabasePort              # - DatabasePort (ejecuta queries)
│   │   ├── SchemaPort                # - SchemaPort (lee schema)
│   │   ├── PromptPort                # - PromptPort (carga prompts)
│   │   └── SqlSafetyValidatorPort    # - SqlSafetyValidatorPort (valida SQL)
│   └── exception/                    # Excepciones de dominio
│
├── application/                     # Orquesta use cases
│   ├── service/ProcessQuestionService  # Implementa el use case
│   └── dto/                          # DTOs de entrada
│
└── infrastructure/                  # Detalles técnicos
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
- `infrastructure/` importa de `application/` y `domain/`, pero nunca al revés

---

## Seguridad

- **API key** solo en `.env` (ignorado por git). Rótalo periódicamente.
- **SQL inyectado**: defense in depth con parser AST (JSqlParser) + blacklist de 18 keywords prohibidas.
- **Solo lectura en BD**: la app usa un usuario de BD (`sqlgen_readonly`) con permisos solo-SELECT, aunque el validador falle.
- **No exposición de secretos en la UI**: la URL de la BD se enmascara antes de mostrarse.
- **Auditoría de seguridad hecha**: cero secretos en repo, cero patrones peligrosos en código.

---

## Tests

### Python
```bash
pytest -q        # 91 tests pasan
ruff check .     # linter limpio
```

### Java
```bash
cd java
mvn test                                 # 95 tests
mvn test -Dtest=BackendE2ETest          # E2E con Claude API real (si ANTHROPIC_API_KEY valida)
```

Los tests de integración Java hacen llamadas **reales** a la API de Anthropic y se saltan automáticamente si no hay API key válida.

### Angular
```bash
cd frontend
npm run build   # TypeScript compila sin errores
npm test        # Jasmine + Karma
```

---

## Troubleshooting

### "ANTHROPIC_API_KEY no está definida"
Crea `.env` copiando `.env.example` y rellena la variable.

### "connection refused" en el frontend Angular
Verifica que el backend Spring Boot esté corriendo en `8080`:
```bash
curl http://localhost:8080/api/v1/health
```

### "connection refused" en la versión Python
Verifica que PostgreSQL esté corriendo:
```bash
docker compose ps
# Debe mostrar el contenedor sqlgen-postgres como "healthy"
```

### Quiero resetear los datos de la BD
```bash
docker compose down -v
docker compose up -d
python -m scripts.seed_db
```

### Quiero cambiar el modelo de Claude
En `.env`:
```env
LLM_MODEL=claude-sonnet-4-5
```

Sonnet es más capaz pero más caro (~5x Haiku). Opus es el más potente (~25x).

### Quiero ver la documentación de la API
Con el backend corriendo, abre:
- http://localhost:8080/swagger-ui.html (UI interactiva)
- http://localhost:8080/v3/api-docs (JSON)

---

## Licencia

MIT — libre uso, modificación y distribución.
