"""Submódulo de base de datos."""

from .connection import DatabaseError, execute_query, get_connection

__all__ = ["DatabaseError", "execute_query", "get_connection"]
