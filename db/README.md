# Recursos compartidos de la base de datos.

Esta carpeta contiene los archivos SQL que son comunes a la version Python
y a la version Java/Spring Boot del AI SQL Query Generator.

## Archivos

- **`schema.sql`** — DDL de las 4 tablas e-commerce + seed data (20 users, 50 products, 105 orders, 315 order_items).
- **`init.sql`** — Script que se ejecuta automaticamente al crear el contenedor de Postgres. Crea el usuario `sqlgen_readonly` con permisos solo-SELECT, usado por la app Java como medida de defense-in-depth (aunque el validador SQL ya bloquea writes).

## Uso

### Resetear la BD (borra los datos y recrea desde cero)

```bash
# Detener el contenedor y borrar el volumen
docker compose down -v

# Levantar de nuevo (ejecuta init.sql automaticamente)
docker compose up -d

# Cargar schema + datos
python -m scripts.seed_db
```

### Verificar el usuario readonly

```bash
# Esto debe funcionar (SELECT):
docker compose exec postgres psql -U sqlgen_readonly -d sqlgen -c "SELECT * FROM users LIMIT 1"

# Esto debe FALLAR con "permission denied" (DROP):
docker compose exec postgres psql -U sqlgen_readonly -d sqlgen -c "DROP TABLE users"
```
