# sqlgen-backend (Spring Boot)

Backend Java del AI SQL Query Generator (Issue #2). Replica 1:1 de la version Python
usando arquitectura hexagonal, Spring Boot 3.3 y HTTP directo a Anthropic Claude.

## Requisitos

- Java 21 LTS
- Maven 3.9+
- PostgreSQL 16 corriendo (ver `../../../docker-compose.yml` en la raiz)
- `ANTHROPIC_API_KEY` configurada en `../../../.env` (raiz del repo)

## Comandos

```bash
# Compilar
mvn compile

# Tests
mvn test

# Arrancar (puerto 8080)
mvn spring-boot:run

# Empaquetar JAR
mvn clean package
java -jar target/sqlgen-backend-0.1.0.jar
```

## Endpoints (REST)

- `GET  /api/v1/health`         - Health check
- `POST /api/v1/queries`        - Hacer pregunta en lenguaje natural
- `GET  /api/v1/schema`         - Info del schema de la BD
- `GET  /v3/api-docs`           - OpenAPI JSON
- `GET  /swagger-ui.html`       - Swagger UI
- `GET  /actuator/health`       - Spring Boot Actuator

## Variables de entorno

Ver `../.env.example` en la raiz. Las vars de Java son:
- `JAVA_DATASOURCE_URL`
- `JAVA_DATASOURCE_USERNAME`
- `JAVA_DATASOURCE_PASSWORD`
- `JAVA_DATASOURCE_READONLY_USERNAME` (Task 6)
- `JAVA_DATASOURCE_READONLY_PASSWORD` (Task 6)
- `SPRING_AI_ANTHROPIC_API_KEY`
- `LLM_MODEL` (opcional, default: claude-haiku-4-5)

## Arquitectura

Ver `.harness/tech/tech-plan.md` (Task 2+) para la estructura hexagonal completa:
- `domain/` - Java puro, sin Spring
- `application/` - Servicios de aplicacion
- `infrastructure/` - Spring, JDBC, Spring AI, REST
