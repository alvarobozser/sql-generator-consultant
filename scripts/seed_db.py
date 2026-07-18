"""Script de seed: crea el schema e inserta datos de ejemplo en PostgreSQL.

Uso:
    python -m scripts.seed_db

Lee DATABASE_URL del entorno o del .env. El script es idempotente:
puede ejecutarse varias veces seguidas sin error (DROP IF EXISTS al inicio).
"""

from __future__ import annotations

import sys
from pathlib import Path

import psycopg

# Permite ejecutar el script tanto como módulo (python -m scripts.seed_db)
# como script directo (python scripts/seed_db.py).
_PROJECT_ROOT = Path(__file__).resolve().parent.parent
sys.path.insert(0, str(_PROJECT_ROOT))

from src.config import load_config  # noqa: E402

SCHEMA_PATH = _PROJECT_ROOT / "schema.sql"


def _read_schema() -> str:
    """Lee el archivo schema.sql desde la raíz del proyecto."""
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
