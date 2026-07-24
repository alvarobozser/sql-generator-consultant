"""Submódulo de validadores."""

from .sql_safety import UnsafeSQLError, validate_sql

__all__ = ["UnsafeSQLError", "validate_sql"]
