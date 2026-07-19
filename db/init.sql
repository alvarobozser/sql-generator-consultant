-- =================================================================
-- AI SQL Query Generator — init script de Postgres
-- =================================================================
-- Se ejecuta automaticamente al crear el contenedor (solo la primera
-- vez, si el volumen esta vacio). Crea un usuario de BD de solo
-- lectura que sera usado por la app Java para ejecutar queries.
-- =================================================================

-- Crear usuario readonly (si no existe).
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_user WHERE usename = 'sqlgen_readonly') THEN
        CREATE USER sqlgen_readonly WITH PASSWORD 'readonly_password';
    END IF;
END
$$;

-- Conceder permiso de conexion a la base de datos.
GRANT CONNECT ON DATABASE sqlgen TO sqlgen_readonly;

-- Conceder USAGE en el schema public (necesario para ver tablas).
GRANT USAGE ON SCHEMA public TO sqlgen_readonly;

-- Conceder SELECT en todas las tablas existentes Y futuras del schema public.
GRANT SELECT ON ALL TABLES IN SCHEMA public TO sqlgen_readonly;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT ON TABLES TO sqlgen_readonly;
