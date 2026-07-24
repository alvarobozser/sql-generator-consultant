# sql-gen-spring-angular (Issue #2)

> Version Java + Angular del AI SQL Query Generator (migracion de la version Python).

Backend Java 21 con Spring Boot 3.3 (arquitectura hexagonal) y frontend Angular 19
(standalone components + signals). Usa Anthropic Claude (Haiku 4.5) como motor de IA.

## Quick start

Desde la **raiz del repo** (no desde aqui):

```bash
# 1. Levantar Postgres (con usuario readonly)
docker compose up -d

# 2. Cargar schema + datos
python -m sql-gen-python.scripts.seed_db

# 3. Backend (puerto 8080)
cd sql-gen-spring-angular/java
mvn clean test         # ejecutar tests
mvn spring-boot:run    # arrancar

# 4. Frontend (puerto 4200, en otra terminal)
cd sql-gen-spring-angular/frontend
npm install
npm start

# Abre http://localhost:4200
```

El frontend tiene un proxy (`proxy.conf.json`) que redirige `/api/*` al backend
en `8080`. **No hay problemas de CORS en dev**.

## Estructura

```
sql-gen-spring-angular/
├── java/                          # Backend Spring Boot
│   ├── pom.xml
│   ├── README.md                  # Detalles del backend
│   └── src/
│       ├── main/
│       │   ├── java/com/sqlgen/
│       │   │   ├── SqlgenApplication.java
│       │   │   ├── domain/        # Java puro (sin Spring)
│       │   │   ├── application/   # Servicios de aplicacion
│       │   │   └── infrastructure/ # Spring, JDBC, REST
│       │   └── resources/
│       │       ├── application.yml
│       │       ├── prompts_es.properties
│       │       └── prompts_en.properties
│       └── test/                  # 95 tests (JUnit 5 + Mockito)
│
└── frontend/                      # Angular 19 SPA
    ├── package.json
    ├── angular.json
    ├── proxy.conf.json            # /api/* -> localhost:8080
    └── src/
        └── app/
            ├── core/              # Servicios singleton (HTTP, i18n)
            └── features/query/    # Componente principal
```

## Comandos utiles

```bash
# Backend
cd java
mvn test                            # 95 tests
mvn spring-boot:run                 # arrancar
mvn clean package                   # generar JAR

# Frontend
cd frontend
npm install                         # solo la primera vez
npm start                           # dev server (puerto 4200)
npm run build                       # build produccion
npm test                            # tests con Karma + Jasmine
```

## API REST

| Metodo | Path | Descripcion |
|---|---|---|
| `GET`  | `/api/v1/health` | Health check |
| `POST` | `/api/v1/queries` | Hacer pregunta en lenguaje natural |
| `GET`  | `/api/v1/schema` | Schema de la BD (tablas + columnas) |
| `GET`  | `/v3/api-docs` | OpenAPI JSON |
| `GET`  | `/swagger-ui.html` | Swagger UI |
| `GET`  | `/actuator/health` | Spring Boot Actuator |

Con el backend corriendo:
- http://localhost:8080/swagger-ui.html - UI interactiva
- http://localhost:8080/v3/api-docs - JSON

### Ejemplo

```bash
curl -X POST http://localhost:8080/api/v1/queries \
  -H "Content-Type: application/json" \
  -d '{"question":"Cuantos usuarios hay de Espana?","language":"es"}'
```

## Variables de entorno

Las variables se leen desde `../.env` (raiz del repo) gracias al loader
custom en `SqlgenApplication.main()`.

- `ANTHROPIC_API_KEY` (obligatoria)
- `JAVA_DATASOURCE_URL`, `JAVA_DATASOURCE_USERNAME`, `JAVA_DATASOURCE_PASSWORD`
- `JAVA_DATASOURCE_READONLY_USERNAME`, `JAVA_DATASOURCE_READONLY_PASSWORD`
- `LLM_MODEL` (opcional, default `claude-haiku-4-5`)

## Features

- Preguntas en espanol o ingles
- Validacion de SQL con JSqlParser (defense in depth)
- Ejecucion real contra PostgreSQL via JDBC
- Usuario de BD de solo lectura (defense in depth)
- Arquitectura hexagonal estricta (domain Java puro)
- 95 tests automatizados
- API REST documentada con OpenAPI/Swagger
- Frontend con signals (Angular 19)

## Documentacion adicional

Ver `../README.md` (raiz del repo) para el contexto completo del proyecto,
incluyendo la version Python equivalente.
