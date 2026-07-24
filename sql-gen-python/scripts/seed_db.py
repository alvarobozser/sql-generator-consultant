"""Script de seed: crea el schema e inserta datos de ejemplo en PostgreSQL.

Uso (desde la raiz del repo):
    python -m sql-gen-python.scripts.seed_db
    # o bien:
    cd sql-gen-python && python -m scripts.seed_db

Lee DATABASE_URL del entorno o del .env. El script es idempotente:
puede ejecutarse varias veces seguidas sin error (DROP IF EXISTS al inicio).
"""

from __future__ import annotations

import sys
from pathlib import Path

import psycopg

# Estructura del repo (tras la reorganizacion):
#   nproject/
#   ├── db/schema.sql                    <- schema compartido
#   ├── sql-gen-python/                   <- este script
#   │   ├── src/config.py                 <- importado aqui
#   │   └── scripts/seed_db.py            <- este archivo
#   └── sql-gen-spring-angular/
#
# _REPO_ROOT apunta a la raiz del repo (necesario para db/ y src/).
_REPO_ROOT = Path(__file__).resolve().parents[2]
_PYTHON_PROJECT = _REPO_ROOT / "sql-gen-python"

# Anade sql-gen-python/ al path para que `from src.config import ...` funcione.
sys.path.insert(0, str(_PYTHON_PROJECT))

from src.config import load_config  # noqa: E402

SCHEMA_PATH = _REPO_ROOT / "db" / "schema.sql"


def _read_schema() -> str:
    """Lee el archivo schema.sql desde db/ en la raíz del proyecto."""
    if not SCHEMA_PATH.exists():
        raise FileNotFoundError(f"No se encuentra schema.sql en {SCHEMA_PATH}")
    return SCHEMA_PATH.read_text(encoding="utf-8")


def seed_database() -> None:
    """Ejecuta schema.sql contra la BD indicada por DATABASE_URL."""
    config = load_config()
    schema_sql = _read_schema()

    print("Conectando a PostgreSQL...")
    with psycopg.connect(config.database_url) as conn, conn.cursor() as cur:
        print("Ejecutando schema (DROP + CREATE + INSERT)...")
        cur.execute(schema_sql)
        conn.commit()

    print("[OK] Schema aplicado. Verificando conteo de filas...")

    # Verificación rápida.
    with psycopg.connect(config.database_url) as conn, conn.cursor() as cur:
        cur.execute("SELECT count(*) FROM users")
        users = cur.fetchone()[0]
        cur.execute("SELECT count(*) FROM products")
        products = cur.fetchone()[0]
        cur.execute("SELECT count(*) FROM orders")
        orders = cur.fetchone()[0]
        cur.execute("SELECT count(*) FROM order_items")
        items = cur.fetchone()[0]

    print(f"  users       : {users}")
    print(f"  products    : {products}")
    print(f"  orders      : {orders}")
    print(f"  order_items : {items}")
    print("[OK] Seed completado.")


if __name__ == "__main__":
    seed_database()
